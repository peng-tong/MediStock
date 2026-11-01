package com.medistock.base.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medistock.base.entity.Drug;
import com.medistock.base.service.DrugService;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.common.result.PageResult;
import com.medistock.common.result.Result;
import com.medistock.common.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 药品档案接口：分页 / 增删改 */
@RestController
@RequestMapping("/api/drugs")
@RequiredArgsConstructor
public class DrugController {

    private final DrugService service;

    @GetMapping
    public Result<PageResult<Drug>> page(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int pageSize,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Long categoryId,
                                         // 状态过滤：声明为 Integer，非法值（如 abc）由全局类型转换返回 400
                                         @RequestParam(required = false) Integer status) {
        Page<Drug> p = service.pageQuery(PageUtil.page(page), PageUtil.pageSize(pageSize), keyword, categoryId, status);
        return Result.ok(new PageResult<>(p.getTotal(), p.getRecords()));
    }

    /** 详情：存在返回 200，不存在/跨租户返回 404（补齐此前缺失导致 500 的接口） */
    @GetMapping("/{id}")
    public Result<Drug> detail(@PathVariable Long id) {
        Drug d = service.getById(id);
        if (d == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "药品不存在");
        }
        return Result.ok(d);
    }

    @PostMapping
    public Result<Void> create(@RequestBody Drug d) { service.createDrug(d); return Result.ok(); }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Drug d) {
        d.setId(id); service.updateDrug(d); return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { service.deleteDrug(id); return Result.ok(); }
}
