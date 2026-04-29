package com.simon.basicservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simon.basicservice.entity.Student;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentMapper extends BaseMapper<Student> {
}
