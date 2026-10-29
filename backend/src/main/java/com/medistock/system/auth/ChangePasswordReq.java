package com.medistock.system.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 修改密码请求 */
@Data
public class ChangePasswordReq {
    @NotBlank
    private String oldPassword;
    @NotBlank
    private String newPassword;
}
