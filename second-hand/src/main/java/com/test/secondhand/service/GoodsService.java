package com.test.secondhand.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.test.secondhand.entity.Goods;
import com.test.secondhand.entity.UploadFile;
import com.test.secondhand.mapper.GoodsMapper;
import com.test.secondhand.util.RedisCacheHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoodsService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GoodsService.class);

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private RedisCacheHelper redisCacheHelper;

    @Autowired
    private com.test.secondhand.mapper.UploadFileMapper uploadFileMapper;

    @Autowired
    private ElasticsearchService elasticsearchService;

    /**
     * 获取全部商品列表（在售状态 0）
     */
    public List<Goods> getActiveGoodsList() {
        return goodsMapper.selectList(new LambdaQueryWrapper<Goods>()
                .eq(Goods::getStatus, 0)
                .orderByDesc(Goods::getCreateTime));
    }

    /**
     * 根据ID获取商品详情（引入 Redis 缓存防击穿、防穿透、防雪崩设计）
     */
    public Goods getGoodsById(Long id) {
        String cacheKey = "goods:detail:" + id;
        String lockKey = "lock:goods:" + id;
        
        // 使用 RedisCacheHelper 缓存加载组件，有效应对高并发下缓存防击穿、防穿透、防雪崩
        return redisCacheHelper.getOrLoad(
                cacheKey, 
                lockKey, 
                Goods.class, 
                () -> goodsMapper.selectById(id), 
                3600 // 缓存过期时间设置为 3600秒（1小时）
        );
    }

    /**
     * 发布二手商品
     */
    public void publishGoods(Goods goods) {
        goods.setStatus(0); // 默认在售
        goodsMapper.insert(goods);
        markImageAsUsed(goods.getImageUrl());

        // 同步至 ES
        try {
            elasticsearchService.syncGoods(goods);
        } catch (Exception e) {
            log.error("[ES] 自动同步新商品文档失败，不阻碍主业务。ID: {}", goods.getId(), e);
        }
    }

    /**
     * 更新商品信息（引入延迟双删确保缓存一致性）
     */
    public void updateGoods(Goods goods) {
        String cacheKey = "goods:detail:" + goods.getId();
        
        // 使用缓存一致性双闪更新，保证 DB 与 Redis 缓存数据一致
        redisCacheHelper.updateDbAndEvictCache(cacheKey, () -> {
            goodsMapper.updateById(goods);
            markImageAsUsed(goods.getImageUrl());
        });

        // 同步至 ES
        try {
            Goods updated = goodsMapper.selectById(goods.getId());
            if (updated != null) {
                elasticsearchService.syncGoods(updated);
            }
        } catch (Exception e) {
            log.error("[ES] 自动同步更新商品文档失败。ID: {}", goods.getId(), e);
        }
    }

    /**
     * 将用户上传的图片文件在表里标记为"已使用"，避免定时垃圾任务将其删除
     */
    private void markImageAsUsed(String imageUrl) {
        if (imageUrl != null && imageUrl.contains("/uploads/")) {
            String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            uploadFileMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<UploadFile>()
                        .like("file_path", filename)
                        .set("is_used", 1)
            );
        }
    }

    /**
     * 商品搜索
     */
    public List<Goods> searchGoods(String keyword, String category, String sortBy, int page, int size) {
        // 1. 尝试使用 Elasticsearch 混合搜索
        try {
            List<Long> ids = elasticsearchService.searchGoodsIds(keyword, category, sortBy, page, size);
            if (ids == null || ids.isEmpty()) {
                return new java.util.ArrayList<>();
            }
            List<Goods> dbGoodsList = goodsMapper.selectBatchIds(ids);
            // 保持 ES 返回的评分/排序顺序
            dbGoodsList.sort(java.util.Comparator.comparingInt(item -> ids.indexOf(item.getId())));
            return dbGoodsList;
        } catch (Exception e) {
            log.warn("[ES] Elasticsearch 检索服务异常，自动降级至数据库 SQL 模糊匹配查询。原因: {}", e.getMessage());
        }

        // 2. 数据库检索兜底逻辑
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<Goods>()
                .eq(Goods::getStatus, 0); // 只查在售商品

        // 关键字搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w
                    .like(Goods::getName, keyword)
                    .or()
                    .like(Goods::getDescription, keyword));
        }

        // 分类筛选
        if (category != null && !category.trim().isEmpty()) {
            wrapper.eq(Goods::getCategory, category);
        }

        // 排序
        if ("price_asc".equals(sortBy)) {
            wrapper.orderByAsc(Goods::getPrice);
        } else if ("price_desc".equals(sortBy)) {
            wrapper.orderByDesc(Goods::getPrice);
        } else if ("views".equals(sortBy)) {
            wrapper.orderByDesc(Goods::getViewCount);
        } else {
            // 默认按创建时间降序
            wrapper.orderByDesc(Goods::getCreateTime);
        }

        // 分页
        wrapper.last("LIMIT " + size + " OFFSET " + (page - 1) * size);

        return goodsMapper.selectList(wrapper);
    }

    /**
     * 增加浏览量
     */
    public void incrementViewCount(Long goodsId) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods != null) {
            goods.setViewCount(goods.getViewCount() != null ? goods.getViewCount() + 1 : 1);
            goodsMapper.updateById(goods);
        }
    }

    /**
     * 获取相关推荐商品（同分类，排除当前商品）
     */
    public List<Goods> getRelatedGoods(Long goodsId, String category, int limit) {
        if (category == null || category.isEmpty()) {
            return goodsMapper.selectList(
                    new LambdaQueryWrapper<Goods>()
                            .eq(Goods::getStatus, 0)
                            .ne(Goods::getId, goodsId)
                            .orderByDesc(Goods::getViewCount)
                            .last("LIMIT " + limit));
        }

        return goodsMapper.selectList(
                new LambdaQueryWrapper<Goods>()
                        .eq(Goods::getStatus, 0)
                        .eq(Goods::getCategory, category)
                        .ne(Goods::getId, goodsId)
                        .orderByDesc(Goods::getViewCount)
                        .last("LIMIT " + limit));
    }
}
