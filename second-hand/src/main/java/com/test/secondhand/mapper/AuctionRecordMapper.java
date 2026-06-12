package com.test.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.test.secondhand.entity.AuctionRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuctionRecordMapper extends BaseMapper<AuctionRecord> {
}
