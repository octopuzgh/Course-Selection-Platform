package com.simon.basicservice.controller;

import com.simon.basicservice.common.ErrorCode;
import com.simon.basicservice.common.Result;
import com.simon.basicservice.dto.LoginRequest;
import com.simon.basicservice.dto.LoginResponse;
import com.simon.basicservice.entity.User;
import com.simon.basicservice.service.interf.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理", description = "用户登录和信息查询接口")
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "用户登录", description = "使用用户名和密码进行登录验证，返回用户角色信息")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.getByUsername(request.getUsername());
        if (user == null) {
            return Result.error(ErrorCode.USER_NOT_FOUND);
        }

        if (request.getPassword().equals(user.getPassword())) {
            LoginResponse response = new LoginResponse();
            response.setUsername(user.getUsername());
            response.setRole(user.getRole());
            response.setToken("mock-token-" + user.getUsername());
            return Result.success(response);
        } else {
            return Result.error(ErrorCode.USER_PASSWORD_ERROR);
        }
    }

    @Operation(summary = "查询用户信息", description = "根据用户名查询用户信息（不返回密码字段）")
    @GetMapping("/{username}")
    public Result<User> getUser(@PathVariable String username) {
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error(ErrorCode.USER_NOT_FOUND);
        }
        user.setPassword(null);
        return Result.success(user);
    }
}

