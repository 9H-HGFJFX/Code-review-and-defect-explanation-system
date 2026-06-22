package com.codereview.controller;

import com.codereview.common.PageRequest;
import com.codereview.common.PageResponse;
import com.codereview.common.Result;
import com.codereview.config.JwtUtil;
import com.codereview.dto.ReviewResultDTO;
import com.codereview.dto.ReviewSubmitRequest;
import com.codereview.entity.User;
import com.codereview.service.AuthService;
import com.codereview.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 代码审查控制器
 */
@Tag(name = "代码审查", description = "代码提交、审查结果查询")
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {
    
    private final ReviewService reviewService;
    private final AuthService authService;
    private final JwtUtil jwtUtil;
    
    @Operation(summary = "提交代码审查")
    @PostMapping("/submit")
    public Result<ReviewResultDTO> submitReview(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ReviewSubmitRequest request) {
        
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(token);
        String username = jwtUtil.getUsername(token);
        
        ReviewResultDTO result = reviewService.submitReview(userId, username, request);
        return Result.success(result);
    }
    
    @Operation(summary = "获取审查结果详情")
    @GetMapping("/{id}")
    public Result<ReviewResultDTO> getReviewResult(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(token);
        
        ReviewResultDTO result = reviewService.getReviewResult(id, userId);
        return Result.success(result);
    }
    
    @Operation(summary = "分页查询审查记录")
    @GetMapping("/list")
    public Result<PageResponse<ReviewResultDTO>> getReviewList(
            @RequestHeader("Authorization") String authHeader,
            PageRequest pageRequest) {
        
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(token);
        String role = jwtUtil.getRole(token);
        
        PageResponse<ReviewResultDTO> result = reviewService.getReviewList(userId, role, pageRequest);
        return Result.success(result);
    }
    
    @Operation(summary = "轮询异步审查任务结果")
    @GetMapping("/async/result/{taskId}")
    public Result<ReviewResultDTO> pollAsyncResult(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String taskId) {
        
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(token);
        
        ReviewResultDTO result = reviewService.pollAsyncResult(taskId, userId);
        return Result.success(result);
    }
}
