package com.codeaudit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codeaudit.common.PageResult;
import com.codeaudit.common.Result;
import com.codeaudit.dto.RuleSaveReq;
import com.codeaudit.entity.Rule;
import com.codeaudit.security.SecurityUtil;
import com.codeaudit.service.RuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "规则管理")
@RestController
@RequestMapping("/api/rule")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;

    @GetMapping("/list")
    @Operation(summary = "分页查询规则")
    public Result<PageResult<Rule>> list(@RequestParam(defaultValue = "1") int current,
                                          @RequestParam(defaultValue = "20") int size,
                                          @RequestParam(required = false) String category) {
        return Result.success(PageResult.of(
                ruleService.listAll(current, size, category),
                ruleService.countAll(category),
                current, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "规则详情")
    public Result<Rule> get(@PathVariable Long id) {
        return Result.success(ruleService.getById(id));
    }

    @PostMapping("/add")
    @Operation(summary = "新增规则（教师/管理员）")
    public Result<Long> add(@RequestBody @Valid RuleSaveReq req) {
        String role = SecurityUtil.currentRole();
        return Result.success(ruleService.add(req, role));
    }

    @PutMapping("/update/{id}")
    @Operation(summary = "编辑规则（自动触发热更新）")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid RuleSaveReq req) {
        String role = SecurityUtil.currentRole();
        ruleService.update(id, req, role);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除规则（非内置）")
    public Result<Void> delete(@PathVariable Long id) {
        String role = SecurityUtil.currentRole();
        ruleService.delete(id, role);
        return Result.success();
    }

    @PutMapping("/toggle/{id}")
    @Operation(summary = "启用/禁用规则")
    public Result<Void> toggle(@PathVariable Long id, @RequestParam Integer enabled) {
        String role = SecurityUtil.currentRole();
        ruleService.toggleEnabled(id, enabled, role);
        return Result.success();
    }

    @PostMapping("/refresh")
    @Operation(summary = "手动刷新规则缓存（热更新）")
    public Result<Void> refresh() {
        SecurityUtil.requireRole("TEACHER", "ADMIN");
        ruleService.refreshCache();
        return Result.success();
    }
}
