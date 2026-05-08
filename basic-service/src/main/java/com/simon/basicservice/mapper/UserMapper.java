package com.simon.basicservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simon.basicservice.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
