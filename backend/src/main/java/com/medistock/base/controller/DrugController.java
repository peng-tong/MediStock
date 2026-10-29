package com.medistock.base.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medistock.base.entity.Drug;
import com.medistock.base.service.DrugService;
import com.medistock.common.result.PageResult;
import com.medistock.common.result.Result;
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
                                         @RequestParam(required = false) Long categoryId) {
        Page<Drug> p = service.pageQuery(page, pageSize, keyword, categoryId);
        return Result.ok(new PageResult<>(p.getTotal(), p.getRecords()));
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
