package com.medistock.system.auth;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.medistock.common.config.UserContext;
import com.medistock.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/** 认证接口：验证码 / 登录 / 登出 / 当前用户 / 改密 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CaptchaStore captchaStore;

    /** 图形验证码：返回 key + base64 图片 */
    @GetMapping("/captcha")
    public Result<Map<String, String>> captcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40);
        String key = UUID.randomUUID().toString().replace("-", "");
        captchaStore.put(key, captcha.getCode());
        return Result.ok(Map.of("key", key, "image", "data:image/png;base64," + captcha.getImageBase64()));
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginReq req) {
        return Result.ok(authService.login(req));
    }

    /** 登出：JWT 无状态，前端清除 token 即可（token 黑名单为 P1 预留） */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok();
    }

    /** 当前登录用户信息（前端动态渲染菜单用） */
    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        return Result.ok(Map.of(
                "userId", UserContext.getUserId(),
                "username", UserContext.getUsername()));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordReq req) {
        authService.changePassword(req);
        return Result.ok();
    }
}
