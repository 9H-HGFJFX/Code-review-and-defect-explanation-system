package com.codeaudit.gateway.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 网关统一错误响应
 */
@Slf4j
@Component
public class JsonErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper om = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (ex instanceof org.springframework.web.server.ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", status.value());
        body.put("message", ex.getMessage() == null ? status.getReasonPhrase() : ex.getMessage());
        body.put("data", null);

        log.warn("[GW_ERR] {} {} -> {} {}",
                exchange.getRequest().getMethodValue(),
                exchange.getRequest().getURI().getPath(),
                status.value(), ex.getMessage());

        response.setStatusCode(status);
        try {
            byte[] bytes = om.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
