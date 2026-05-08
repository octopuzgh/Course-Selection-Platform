package com.simon.basicservice.controller;

import com.simon.basicservice.common.Result;
import com.simon.basicservice.entity.User;
import com.simon.basicservice.service.interf.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "用户管理", description = "用户登录和信息查询接口")
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "用户登录", description = "使用用户名和密码进行登录验证")
    @PostMapping("/login")
    public Result<String> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null) {
            return Result.error("用户名或密码不能为空");
        }

        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        // 明文密码校验（仅测试环境）
        if (password.equals(user.getPassword())) {
            return Result.success("登录成功");
        } else {
            return Result.error(401, "密码错误");
        }
    }

     //根据用户名查询用户信息（不返回密码）
     @Operation(summary = "查询用户信息", description = "根据用户名查询用户信息（不返回密码字段）")
     @GetMapping("/{username}")
    public Result<User> getUser(@PathVariable String username) {
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        // 出于安全，返回前可清空密码字段
        user.setPassword(null);
        return Result.success(user);
    }
}
