package com.codeaudit.controller;

import com.codeaudit.common.Result;
import com.codeaudit.security.SecurityUtil;
import com.codeaudit.service.StatisticService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "数据统计")
@RestController
@RequestMapping("/api/statistic")
@RequiredArgsConstructor
public class StatsController {

    private final StatisticService statisticService;

    @GetMapping("/overview")
    @Operation(summary = "数据看板概览（教师/管理员）")
    public Result<Map<String, Object>> overview() {
        SecurityUtil.requireRole("TEACHER", "ADMIN");
        return Result.success(statisticService.overview());
    }
}
