package com.codeaudit.engine.parser;

import com.codeaudit.engine.ReviewContext;
import com.codeaudit.common.exception.CodeParseException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ParseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JavaParser 包装 - 解析 + 预处理（BOM 去除、换行统一）
 */
@Slf4j
@Component
public class JavaCodeParser {

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
            StaticJavaParser.Config config = new StaticJavaParser.Config()
                    .setLanguageLevel(com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_17);
            StaticJavaParser.setConfiguration(config);
            ParseResult<CompilationUnit> result = new com.github.javaparser.JavaParser(config).parse(code);
            if (!result.isSuccessful() || result.getResult().isEmpty()) {
                String msg = result.getProblems().stream()
                        .findFirst()
                        .map(p -> p.getMessage())
                        .orElse("语法错误");
                log.warn("[PARSE_FAIL] file={} msg={}", fileName, msg);
                b.parseError(msg);
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
