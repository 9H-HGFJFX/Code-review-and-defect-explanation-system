package com.codeaudit.service;

import com.codeaudit.dto.ReviewSubmitReq;
import com.codeaudit.entity.Review;
import com.codeaudit.vo.ReviewResultVO;

import java.util.List;

public interface ReviewService {
    /** 同步提交审查 */
    ReviewResultVO submit(Long userId, ReviewSubmitReq req);

    /** 获取审查详情 */
    ReviewResultVO detail(Long reviewId, Long currentUserId, String currentRole);

    /** 分页查询当前用户历史 */
    List<Review> history(Long userId, int current, int size);

    /** 全平台分页（教师/管理员） */
    List<Review> allHistory(int current, int size);

    long countHistory(Long userId);
    long countAllHistory();
}
