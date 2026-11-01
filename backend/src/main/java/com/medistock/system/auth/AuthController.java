package com.medistock.system.auth;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medistock.common.config.TenantContext;
import com.medistock.common.config.UserContext;
import com.medistock.common.result.Result;
import com.medistock.common.util.JwtUtil;
import com.medistock.common.util.TokenBlacklist;
import com.medistock.system.entity.Role;
import com.medistock.system.entity.UserRole;
import com.medistock.system.mapper.RoleMapper;
import com.medistock.system.mapper.UserMapper;
import com.medistock.system.mapper.UserRoleMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 认证接口：验证码 / 登录 / 登出 / 当前用户 / 改密 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CaptchaStore captchaStore;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;

    /** 验证码开关：随接口下发，前端据此隐藏验证码区域（测试问题 82） */
    @Value("${captcha.enabled:true}")
    private boolean captchaEnabled;

    /**
     * 图形验证码：返回 key + base64 图片 + enabled 开关。
     * 关闭验证码时前端不再展示输入框，避免用户填写后被后端忽略造成困惑。
     */
    @GetMapping("/captcha")
    public Result<Map<String, Object>> captcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40);
        String key = UUID.randomUUID().toString().replace("-", "");
        captchaStore.put(key, captcha.getCode());
        return Result.ok(Map.of(
                "key", key,
                "image", "data:image/png;base64," + captcha.getImageBase64(),
                "enabled", captchaEnabled));
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginReq req) {
        return Result.ok(authService.login(req));
    }

    /**
     * 登出：把当前 token 加入黑名单，使其立即失效（而非等自然过期）。
     * 前端仍需清除本地 token；服务端黑名单兜底，防止登出后旧 token 继续访问接口。
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            Claims claims = jwtUtil.parseToken(token);
            if (claims != null && claims.getExpiration() != null) {
                // 记录到 token 自身过期时间，到期后由黑名单自动清理
                tokenBlacklist.revoke(token, claims.getExpiration().getTime());
            }
        }
        return Result.ok();
    }

    /**
     * 当前登录用户信息（前端动态渲染菜单与顶栏角色用）
     * roles：该用户关联的角色名列表（可能为空，前端不得用「超级管理员」兜底掩盖）
     */
    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        List<String> roles = new ArrayList<>();
        List<UserRole> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, UserContext.getUserId()));
        if (!userRoles.isEmpty()) {
            List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).toList();
            roles = roleMapper.selectBatchIds(roleIds).stream()
                    .map(Role::getName).toList();
        }
        // realName 与用户列表字段保持一致，前端顶栏优先展示姓名（此前缺失导致只显示用户名）
        com.medistock.system.entity.User account = userMapper.selectById(UserContext.getUserId());
        return Result.ok(Map.of(
                "userId", UserContext.getUserId(),
                "username", UserContext.getUsername(),
                "realName", account == null || account.getRealName() == null
                        ? UserContext.getUsername() : account.getRealName(),
                "tenantId", TenantContext.getTenantId(),
                "roles", roles));
    }

    /**
     * 修改密码：成功后把当前 token 加入黑名单，使旧 token 立即失效（测试「改密后旧 token 复用」）。
     * 前端收到成功提示后会清理本地登录态并要求重新登录。
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordReq req, HttpServletRequest request) {
        authService.changePassword(req);
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            Claims claims = jwtUtil.parseToken(token);
            if (claims != null && claims.getExpiration() != null) {
                tokenBlacklist.revoke(token, claims.getExpiration().getTime());
            }
        }
        return Result.ok();
    }
}
