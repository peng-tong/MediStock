package com.medistock.system.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 修改密码请求 */
@Data
public class ChangePasswordReq {
    @NotBlank
    private String oldPassword;
    // 第五轮模块F：新密码长度 6~64（前端提示至少 6 位，后端同样强制，防止直调接口设置 1 位弱密码）
    @NotBlank
    @Size(min = 6, max = 64, message = "新密码长度必须为 6~64 位")
    private String newPassword;
}
