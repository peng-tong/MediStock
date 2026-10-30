package com.medistock.common.service;

import com.medistock.base.entity.Customer;
import com.medistock.base.entity.Drug;
import com.medistock.base.entity.Supplier;
import com.medistock.base.entity.Warehouse;
import com.medistock.base.mapper.CustomerMapper;
import com.medistock.base.mapper.DrugMapper;
import com.medistock.base.mapper.SupplierMapper;
import com.medistock.base.mapper.WarehouseMapper;
import com.medistock.system.entity.User;
import com.medistock.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 列表展示字段解析：把 ID 批量转成名称（药品/仓库/供应商/客户/用户）。
 * 统一一次 in 查询，避免列表行内循环查库造成 N+1。
 */
@Service
@RequiredArgsConstructor
public class NameResolver {

    private final DrugMapper drugMapper;
    private final WarehouseMapper warehouseMapper;
    private final SupplierMapper supplierMapper;
    private final CustomerMapper customerMapper;
    private final UserMapper userMapper;

    /** 批量查药品（id → 实体），供需要规格等额外字段的场景复用 */
    public Map<Long, Drug> drugs(Collection<Long> ids) {
        List<Long> clean = cleanIds(ids);
        if (clean.isEmpty()) return Map.of();
        return drugMapper.selectBatchIds(clean).stream()
                .collect(Collectors.toMap(Drug::getId, d -> d, (a, b) -> a));
    }

    /** 药品展示名：通用名 + 规格（如 "阿莫西林胶囊 0.25g×24粒"） */
    public Map<Long, String> drugNames(Collection<Long> ids) {
        Map<Long, String> result = new HashMap<>();
        drugs(ids).forEach((id, d) -> result.put(id,
                d.getSpec() == null || d.getSpec().isBlank()
                        ? d.getGenericName()
                        : d.getGenericName() + " " + d.getSpec()));
        return result;
    }

    public Map<Long, String> warehouseNames(Collection<Long> ids) {
        return nameMap(warehouseMapper.selectBatchIds(cleanIds(ids)), Warehouse::getId, Warehouse::getName);
    }

    public Map<Long, String> supplierNames(Collection<Long> ids) {
        return nameMap(supplierMapper.selectBatchIds(cleanIds(ids)), Supplier::getId, Supplier::getName);
    }

    public Map<Long, String> customerNames(Collection<Long> ids) {
        return nameMap(customerMapper.selectBatchIds(cleanIds(ids)), Customer::getId, Customer::getName);
    }

    /** 操作人展示名：优先真实姓名，缺失时回退登录名 */
    public Map<Long, String> userNames(Collection<Long> ids) {
        List<Long> clean = cleanIds(ids);
        if (clean.isEmpty()) return Map.of();
        return userMapper.selectBatchIds(clean).stream().collect(Collectors.toMap(
                User::getId,
                u -> u.getRealName() == null || u.getRealName().isBlank() ? u.getUsername() : u.getRealName(),
                (a, b) -> a));
    }

    private <T> Map<Long, String> nameMap(List<T> list, java.util.function.Function<T, Long> keyFn,
                                          java.util.function.Function<T, String> valueFn) {
        if (list == null || list.isEmpty()) return Map.of();
        return list.stream().collect(Collectors.toMap(keyFn, valueFn, (a, b) -> a));
    }

    /** 过滤 null 并去重，避免 in () 空集合触发 SQL 异常 */
    private List<Long> cleanIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }
}
