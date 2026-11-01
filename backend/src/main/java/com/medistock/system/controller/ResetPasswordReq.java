package com.medistock.system.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 重置密码请求：明文密码走 body 传输，避免进入访问日志 */
@Data
public class ResetPasswordReq {
    @NotBlank(message = "新密码不能为空")
    // 第五轮模块F：重置密码同样要求 6~64 位，避免把他人密码重置成极弱口令
    @Size(min = 6, max = 64, message = "密码长度必须为 6~64 位")
    private String password;
}
