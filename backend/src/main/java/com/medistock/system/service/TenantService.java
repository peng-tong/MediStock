package com.medistock.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.system.entity.Tenant;
import com.medistock.system.entity.User;
import com.medistock.system.mapper.TenantMapper;
import com.medistock.system.mapper.UserMapper;
import org.springframework.stereotype.Service;

/** 租户管理：创建/启停/最大用户数限制 */
@Service
public class TenantService extends ServiceImpl<TenantMapper, Tenant> {

    private final UserMapper userMapper;

    public TenantService(TenantMapper tenantMapper, UserMapper userMapper) {
        // ServiceImpl 在 MP 3.5.7 无带参构造器，直接为 protected baseMapper 赋值
        this.baseMapper = tenantMapper;
        this.userMapper = userMapper;
    }

    public Page<Tenant> pageQuery(int page, int pageSize, String keyword) {
        LambdaQueryWrapper<Tenant> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(Tenant::getName, keyword).or().like(Tenant::getCode, keyword));
        }
        qw.orderByDesc(Tenant::getId);
        return this.page(new Page<>(page, pageSize), qw);
    }

    /** 停用租户 */
    public void changeStatus(Long id, Integer status) {
        Tenant tenant = getRequired(id);
        tenant.setStatus(status);
        updateById(tenant);
    }

    /** 删除：仅允许无用户关联的租户 */
    public void deleteTenant(Long id) {
        getRequired(id);
        // 租户下仍存在用户时禁止删除，防止孤儿用户数据
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getTenantId, id)) > 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "租户下存在用户，禁止删除");
        }
        removeById(id);
    }

    private Tenant getRequired(Long id) {
        Tenant tenant = getById(id);
        if (tenant == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return tenant;
    }
}
