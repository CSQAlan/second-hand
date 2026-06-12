package com.test.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.test.secondhand.entity.UploadFile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UploadFileMapper extends BaseMapper<UploadFile> {
}
