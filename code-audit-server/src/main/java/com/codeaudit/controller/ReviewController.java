package com.codeaudit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codeaudit.common.PageResult;
import com.codeaudit.common.Result;
import com.codeaudit.dto.ReviewSubmitReq;
import com.codeaudit.entity.Review;
import com.codeaudit.security.SecurityUtil;
import com.codeaudit.service.ReviewService;
import com.codeaudit.vo.ReviewResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "代码审查")
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/submit")
    @Operation(summary = "提交代码审查（≤500 行同步）")
    public Result<ReviewResultVO> submit(@RequestBody @Valid ReviewSubmitReq req) {
        Long userId = SecurityUtil.currentUserId();
        return Result.success(reviewService.submit(userId, req));
    }

    @GetMapping("/{id}")
    @Operation(summary = "审查详情")
    public Result<ReviewResultVO> detail(@PathVariable Long id) {
        Long uid = SecurityUtil.currentUserId();
        String role = SecurityUtil.currentRole();
        return Result.success(reviewService.detail(id, uid, role));
    }

    @GetMapping("/list")
    @Operation(summary = "分页查询当前用户历史")
    public Result<PageResult<Review>> list(@RequestParam(defaultValue = "1") int current,
                                            @RequestParam(defaultValue = "10") int size) {
        Long uid = SecurityUtil.currentUserId();
        return Result.success(PageResult.of(
                reviewService.history(uid, current, size),
                reviewService.countHistory(uid),
                current, size));
    }

    @GetMapping("/list-all")
    @Operation(summary = "分页查询全平台审查记录（教师/管理员）")
    public Result<PageResult<Review>> listAll(@RequestParam(defaultValue = "1") int current,
                                               @RequestParam(defaultValue = "10") int size) {
        SecurityUtil.requireRole("TEACHER", "ADMIN");
        return Result.success(PageResult.of(
                reviewService.allHistory(current, size),
                reviewService.countAllHistory(),
                current, size));
    }
}
