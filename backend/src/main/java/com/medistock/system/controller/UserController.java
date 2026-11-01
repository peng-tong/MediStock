package com.medistock.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medistock.common.result.PageResult;
import com.medistock.common.result.Result;
import com.medistock.common.util.PageUtil;
import com.medistock.system.entity.User;
import com.medistock.system.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 用户管理接口：分页 / 增删改 / 禁用 / 重置密码 / 分配角色 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public Result<PageResult<User>> page(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int pageSize,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Integer status) {
        Page<User> p = userService.pageQuery(PageUtil.page(page), PageUtil.pageSize(pageSize), keyword, status);
        return Result.ok(new PageResult<>(p.getTotal(), p.getRecords()));
    }

    /** 详情：存在返回 200，不存在/跨租户返回 404（补齐此前缺失导致 500 的接口） */
    @GetMapping("/{id}")
    public Result<User> detail(@PathVariable Long id) {
        return Result.ok(userService.detail(id));
    }

    /*
     * 已知限制（测试问题 73，暂不处理）：RBAC 未完善，任意登录用户可管理同租户用户，
     * 属当前正常行为，不做接口级权限校验，详见 UserService#createUser 上方注释。
     */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody UserCreateReq req) {
        userService.createUser(req.getUser(), req.getPassword());
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        userService.updateUser(user);   // 禁止请求体覆写 tenant_id/password
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);     // 被角色引用时拒绝删除
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.changeStatus(id, status); return Result.ok();
    }

    @PutMapping("/{id}/password/reset")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordReq req) {
        // 明文密码改走 body，避免进入访问日志
        userService.resetPassword(id, req.getPassword());
        return Result.ok();
    }

    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        userService.assignRoles(id, roleIds); return Result.ok();
    }
}
