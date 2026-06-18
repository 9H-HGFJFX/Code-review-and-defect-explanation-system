package com.codeaudit.controller;

import com.codeaudit.common.Result;
import com.codeaudit.security.SecurityUtil;
import com.codeaudit.service.ReportService;
import com.codeaudit.service.ReviewService;
import com.codeaudit.vo.ReviewResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Tag(name = "报告导出")
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReviewService reviewService;
    private final ReportService reportService;

    @GetMapping("/export/{reviewId}")
    @Operation(summary = "导出报告，type=pdf|word")
    public ResponseEntity<byte[]> export(@PathVariable Long reviewId,
                                          @RequestParam(defaultValue = "pdf") String type) {
        Long uid = SecurityUtil.currentUserId();
        String role = SecurityUtil.currentRole();
        ReviewResultVO result = reviewService.detail(reviewId, uid, role);

        byte[] bytes;
        MediaType mt;
        String fileName;
        if ("word".equalsIgnoreCase(type) || "docx".equalsIgnoreCase(type)) {
            bytes = reportService.exportWord(result);
            mt = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            fileName = "review-" + reviewId + ".docx";
        } else {
            bytes = reportService.exportPdf(result);
            mt = MediaType.APPLICATION_PDF;
            fileName = "review-" + reviewId + ".pdf";
        }
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(mt)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encoded + "\"")
                .body(bytes);
    }
}
