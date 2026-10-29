package com.medistock.system.controller;

import com.medistock.system.entity.User;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 新增用户请求：user + 明文密码 */
@Data
public class UserCreateReq {
    @NotBlank
    private String password;
    private User user;
}
