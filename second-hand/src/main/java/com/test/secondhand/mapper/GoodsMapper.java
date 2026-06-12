package com.test.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.test.secondhand.entity.Goods;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {
}
