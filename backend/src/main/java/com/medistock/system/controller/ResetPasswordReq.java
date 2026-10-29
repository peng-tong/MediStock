package com.medistock.system.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 重置密码请求：明文密码走 body 传输，避免进入访问日志 */
@Data
public class ResetPasswordReq {
    @NotBlank(message = "新密码不能为空")
    private String password;
}
