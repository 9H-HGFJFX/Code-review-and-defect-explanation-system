package com.codeaudit.service;

import com.codeaudit.vo.ReviewResultVO;

public interface ReportService {
    byte[] exportPdf(ReviewResultVO result);
    byte[] exportWord(ReviewResultVO result);
}
