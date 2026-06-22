package com.codeaudit.engine.parser;

import com.codeaudit.engine.ReviewContext;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * JavaParser 包装 - 解析 + 预处理（BOM 去除、换行统一）。
 * 每次 parse 使用独立 Parser 实例 + 线程局部配置，避免全局状态污染（线程安全）。
 */
@Slf4j
@Component
public class JavaCodeParser {

    /** 线程局部 Parser，复用以获得更佳性能（JavaParser 本身线程不安全） */
    private final ThreadLocal<JavaParser> parserHolder = ThreadLocal.withInitial(() -> {
        ParserConfiguration cfg = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
                .setAttributeComments(false);
        return new JavaParser(cfg);
    });

    public ReviewContext parse(String rawCode, String fileName) {
        if (rawCode == null) rawCode = "";

        // 去除 UTF-8 BOM
        String code = rawCode;
        if (!code.isEmpty() && code.charAt(0) == '\uFEFF') {
            code = code.substring(1);
        }
        // 统一换行符
        code = code.replace("\r\n", "\n").replace('\r', '\n');

        ReviewContext.ReviewContextBuilder b = ReviewContext.builder()
                .sourceCode(rawCode)
                .preprocessedCode(code)
                .fileName(fileName);

        try {
            ParseResult<CompilationUnit> result = parserHolder.get().parse(code);
            if (!result.isSuccessful() || result.getResult().isEmpty()) {
                AtomicReference<String> msg = new AtomicReference<>("语法错误");
                result.getProblems().stream().findFirst().ifPresent(p -> msg.set(p.getMessage()));
                log.warn("[PARSE_FAIL] file={} msg={}", fileName, msg.get());
                b.parseError(msg.get());
            } else {
                b.compilationUnit(result.getResult().get());
            }
        } catch (Exception e) {
            log.warn("[PARSE_EXCEPTION] file={}", fileName, e);
            b.parseError(e.getMessage());
        }
        return b.build();
    }
}
