package com.codereview.engine.parser;

import com.codereview.entity.ReviewRule;

import java.util.List;

/**
 * 源代码解析器接口
 * 定义解析器必须实现的方法
 * 
 * @author code-review-team
 */
public interface SourceParser {

    /**
     * 解析源代码文件，返回缺陷列表
     * 
     * @param file 源代码文件
     * @param rules 适用规则列表
     * @return 解析后的AST节点或问题列表
     * @throws ParseException 解析异常
     */
    ParseResult parse(SourceFile file, List<ReviewRule> rules) throws ParseException;

    /**
     * 获取解析器支持的编程语言
     * 
     * @return 支持的语言列表
     */
    List<String> getSupportedLanguages();

    /**
     * 检查解析器是否支持指定语言
     * 
     * @param language 语言标识
     * @return 是否支持
     */
    boolean supports(String language);
}
