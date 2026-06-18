package com.codeaudit.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * MiniMax M3 API 客户端（缺陷解释）
 * 用途：调用大模型对审查出的问题进行更详细、友好的解释。
 * 设计为可选：MiniMax.enabled=false 时直接返回 null，审查主流程不受影响。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MiniMaxClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final MiniMaxProperties props;
    private final ObjectMapper om = new ObjectMapper();

    private OkHttpClient buildClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(props.getTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(props.getTimeoutSeconds(), TimeUnit.SECONDS)
                .writeTimeout(props.getTimeoutSeconds(), TimeUnit.SECONDS)
                .build();
    }

    /**
     * 对单个问题生成 AI 解释
     * @return AI 解释文本，未启用或失败时返回 null
     */
    public String explainIssue(String ruleName, String description, String codeSnippet) {
        if (!props.isEnabled() || props.getApiKey() == null || props.getApiKey().startsWith("your-")) {
            return null;
        }
        try {
            String prompt = buildPrompt(ruleName, description, codeSnippet);
            Map<String, Object> body = new HashMap<>();
            body.put("model", props.getModel());
            body.put("max_tokens", 400);
            body.put("temperature", 0.3);
            body.put("messages", List.of(
                    Map.of("role", "system", "content", "你是一名资深的 Java 代码审查专家，请用中文简洁解释代码缺陷，给出风险等级、根本原因和修复建议。"),
                    Map.of("role", "user", "content", prompt)
            ));

            Request request = new Request.Builder()
                    .url(props.getBaseUrl() + "/chat/completions")
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .post(RequestBody.create(om.writeValueAsString(body), JSON))
                    .build();

            try (Response resp = buildClient().newCall(request).execute()) {
                if (!resp.isSuccessful()) {
                    log.warn("[M3] 调用失败 status={}", resp.code());
                    return null;
                }
                String respBody = resp.body() == null ? "" : resp.body().string();
                @SuppressWarnings("unchecked")
                Map<String, Object> json = om.readValue(respBody, Map.class);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) json.get("choices");
                if (choices == null || choices.isEmpty()) return null;
                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return message == null ? null : String.valueOf(message.get("content"));
            }
        } catch (Exception e) {
            log.error("[M3] 解释异常", e);
            return null;
        }
    }

    private String buildPrompt(String ruleName, String description, String code) {
        StringBuilder sb = new StringBuilder();
        sb.append("【规则】").append(ruleName).append("\n");
        sb.append("【问题】").append(description).append("\n");
        if (code != null && !code.isBlank()) {
            sb.append("【代码】\n```java\n").append(code.length() > 600 ? code.substring(0, 600) + "..." : code).append("\n```\n");
        }
        sb.append("请用中文回答，控制在 200 字以内，结构：1) 根本原因 2) 风险 3) 推荐修复方案。");
        return sb.toString();
    }
}
