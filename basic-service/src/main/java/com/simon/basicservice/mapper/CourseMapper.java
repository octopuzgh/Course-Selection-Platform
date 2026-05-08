package com.simon.basicservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simon.basicservice.entity.Course;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {
}
