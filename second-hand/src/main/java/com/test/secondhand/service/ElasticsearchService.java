package com.test.secondhand.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.test.secondhand.entity.Goods;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ElasticsearchService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ElasticsearchService.class);

    @Autowired(required = false)
    private ElasticsearchClient esClient;

    @Autowired
    private EmbeddingService embeddingService;

    private static final String INDEX_NAME = "goods";

    @PostConstruct
    public void initIndex() {
        if (esClient == null) {
            log.warn("[ES] ElasticsearchClient 自动注入失败，请检查相关依赖及配置。");
            return;
        }

        try {
            boolean exists = esClient.indices().exists(e -> e.index(INDEX_NAME)).value();
            if (!exists) {
                log.info("[ES] 索引 'goods' 不存在，开始自动创建索引及字段映射...");
                try {
                    createIndexWithAnalyzer("ik_max_word");
                    log.info("[ES] 使用 IK 分词器成功创建商品索引！");
                } catch (Exception e) {
                    log.warn("[ES] 使用 IK 分词器创建商品索引失败，可能未安装 IK 插件。尝试使用 standard 分词器创建。错误: {}", e.getMessage());
                    createIndexWithAnalyzer("standard");
                    log.info("[ES] 使用 standard 分词器成功创建商品索引！");
                }
            } else {
                log.info("[ES] 商品索引 'goods' 已存在，跳过初始化。");
            }
        } catch (Exception e) {
            log.warn("[ES] 索引初始化异常，Elasticsearch 可能处于离线状态，后续将自动启用数据库兜底。异常: {}", e.getMessage());
        }
    }

    private void createIndexWithAnalyzer(String analyzer) throws Exception {
        esClient.indices().create(c -> c
                .index(INDEX_NAME)
                .mappings(m -> m
                        .properties("id", p -> p.long_(l -> l))
                        .properties("name", p -> p.text(t -> {
                            if ("ik_max_word".equals(analyzer)) {
                                t.analyzer("ik_max_word").searchAnalyzer("ik_smart");
                            }
                            return t;
                        }))
                        .properties("description", p -> p.text(t -> {
                            if ("ik_max_word".equals(analyzer)) {
                                t.analyzer("ik_max_word").searchAnalyzer("ik_smart");
                            }
                            return t;
                        }))
                        .properties("price", p -> p.double_(d -> d))
                        .properties("imageUrl", p -> p.keyword(k -> k))
                        .properties("category", p -> p.keyword(k -> k))
                        .properties("condition", p -> p.keyword(k -> k))
                        .properties("viewCount", p -> p.integer(i -> i))
                        .properties("status", p -> p.integer(i -> i))
                        .properties("sellerId", p -> p.long_(l -> l))
                        .properties("createTime", p -> p.date(d -> d))
                        .properties("goodsVector", p -> p.denseVector(dv -> dv
                                .dims(1536)
                                .index(true)
                                .similarity(co.elastic.clients.elasticsearch._types.mapping.DenseVectorSimilarity.Cosine)
                        ))
                )
        );
    }

    /**
     * 同步商品数据到 Elasticsearch 索引并生成向量
     */
    public void syncGoods(Goods goods) {
        if (esClient == null) {
            return;
        }

        try {
            // 拼接文本用于生成语义 Embedding 向量
            String textToEmbed = goods.getName() + " " + (goods.getDescription() != null ? goods.getDescription() : "");
            float[] vector = embeddingService.getEmbedding(textToEmbed);

            List<Float> vectorList = new ArrayList<>();
            for (float v : vector) {
                vectorList.add(v);
            }

            Map<String, Object> doc = new HashMap<>();
            doc.put("id", goods.getId());
            doc.put("name", goods.getName());
            doc.put("description", goods.getDescription());
            doc.put("price", goods.getPrice() != null ? goods.getPrice().doubleValue() : 0.0);
            doc.put("imageUrl", goods.getImageUrl());
            doc.put("category", goods.getCategory());
            doc.put("condition", goods.getCondition());
            doc.put("viewCount", goods.getViewCount() != null ? goods.getViewCount() : 0);
            doc.put("status", goods.getStatus() != null ? goods.getStatus() : 0);
            doc.put("sellerId", goods.getSellerId());
            doc.put("createTime", goods.getCreateTime() != null ? goods.getCreateTime().toString() : LocalDateTime.now().toString());
            doc.put("goodsVector", vectorList);

            esClient.index(i -> i
                    .index(INDEX_NAME)
                    .id(String.valueOf(goods.getId()))
                    .document(doc)
            );
            log.info("[ES] 同步商品文档成功，ID: {}", goods.getId());
        } catch (Exception e) {
            log.error("[ES] 同步商品文档失败，ID: {}", goods.getId(), e);
            throw new RuntimeException("同步 Elasticsearch 失败", e);
        }
    }

    /**
     * 从 Elasticsearch 删除商品
     */
    public void deleteGoods(Long goodsId) {
        if (esClient == null) {
            return;
        }
        try {
            esClient.delete(d -> d.index(INDEX_NAME).id(String.valueOf(goodsId)));
            log.info("[ES] 删除商品文档成功，ID: {}", goodsId);
        } catch (Exception e) {
            log.error("[ES] 删除商品文档失败，ID: {}", goodsId, e);
        }
    }

    /**
     * 混合搜索：结合 BM25 关键字匹配与 kNN 向量语义相似度搜索
     */
    public List<Long> searchGoodsIds(String keyword, String category, String sortBy, int page, int size) {
        if (esClient == null) {
            throw new IllegalStateException("Elasticsearch 客户端未就绪");
        }

        try {
            SearchRequest.Builder searchBuilder = new SearchRequest.Builder().index(INDEX_NAME);

            // 1. 计算搜索词的语义 Embedding 向量
            float[] queryVector = embeddingService.getEmbedding(keyword != null ? keyword : "");
            List<Float> queryVectorList = new ArrayList<>();
            for (float v : queryVector) {
                queryVectorList.add(v);
            }

            // 2. 构造向量 kNN 查询 (传递 lambda 构造 KnnSearch)
            searchBuilder.knn(k -> k
                    .field("goodsVector")
                    .queryVector(queryVectorList)
                    .k(size)
                    .numCandidates(100)
            );

            // 3. 构造过滤器（只查在售商品，以及按分类筛选）和传统的关键字匹配查询
            Query query = Query.of(q -> q
                    .bool(b -> {
                        // 只搜索在售状态(0)的商品
                        b.must(m -> m.term(t -> t.field("status").value(0)));

                        if (category != null && !category.trim().isEmpty()) {
                            b.must(m -> m.term(t -> t.field("category").value(category)));
                        }

                        // 关键字全文检索
                        if (keyword != null && !keyword.trim().isEmpty()) {
                            b.should(s -> s.match(mt -> mt.field("name").query(keyword).boost(2.0f)));
                            b.should(s -> s.match(mt -> mt.field("description").query(keyword).boost(1.0f)));
                        }
                        return b;
                    })
            );
            searchBuilder.query(query);

            // 4. 分页控制
            searchBuilder.from((page - 1) * size);
            searchBuilder.size(size);

            // 5. 排序支持
            if ("price_asc".equals(sortBy)) {
                searchBuilder.sort(s -> s.field(f -> f.field("price").order(SortOrder.Asc)));
            } else if ("price_desc".equals(sortBy)) {
                searchBuilder.sort(s -> s.field(f -> f.field("price").order(SortOrder.Desc)));
            } else if ("views".equals(sortBy)) {
                searchBuilder.sort(s -> s.field(f -> f.field("viewCount").order(SortOrder.Desc)));
            } else if ("newest".equals(sortBy)) {
                searchBuilder.sort(s -> s.field(f -> f.field("createTime").order(SortOrder.Desc)));
            }

            // 执行搜索
            SearchResponse<Map> response = esClient.search(searchBuilder.build(), Map.class);
            List<Long> ids = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                if (hit.source() != null && hit.source().get("id") != null) {
                    ids.add(Long.valueOf(hit.source().get("id").toString()));
                }
            }
            return ids;

        } catch (Exception e) {
            log.error("[ES] 混合检索异常，关键字: {}, 分类: {}", keyword, category, e);
            throw new RuntimeException("Elasticsearch 检索失败", e);
        }
    }
}
