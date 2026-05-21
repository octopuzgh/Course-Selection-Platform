package com.simon.basicservice.service.interf;

import com.baomidou.mybatisplus.extension.service.IService;
import com.simon.basicservice.entity.User;

public interface UserService extends IService<User> {

    //根据用户名查询用户
    User getByUserid(String userid);

}
