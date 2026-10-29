package com.medistock.system.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 登录请求：账号密码 + 图形验证码（验证码可空，P1 接入滑块后弱化） */
@Data
public class LoginReq {
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
    private String captchaKey;
    private String captchaCode;
}
