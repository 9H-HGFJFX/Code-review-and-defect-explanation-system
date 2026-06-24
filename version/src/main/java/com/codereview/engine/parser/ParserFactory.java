package com.codereview.engine.parser;

import com.codereview.common.enums.ProgrammingLanguage;
import com.codereview.entity.ReviewRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 解析器工厂类
 * 负责管理不同语言的解析器
 * 
 * @author code-review-team
 */
@Component
@Slf4j
public class ParserFactory {

    /**
     * 解析器缓存
     */
    private final Map<String, SourceParser> parserCache = new ConcurrentHashMap<>();

    /**
     * Java解析器
     */
    private final JavaParser javaParser;

    /**
     * Python解析器
     */
    private final PythonParser pythonParser;

    /**
     * 构造函数 - 注入解析器
     */
    public ParserFactory(JavaParser javaParser, PythonParser pythonParser) {
        this.javaParser = javaParser;
        this.pythonParser = pythonParser;
        
        // 注册默认解析器
        registerParser(javaParser);
        registerParser(pythonParser);
        
        log.info("解析器工厂初始化完成，已注册解析器数量: {}", parserCache.size());
    }

    /**
     * 注册解析器
     * 
     * @param parser 解析器实例
     */
    public void registerParser(SourceParser parser) {
        for (String language : parser.getSupportedLanguages()) {
            parserCache.put(language.toLowerCase(), parser);
            log.debug("注册解析器: {} -> {}", language, parser.getClass().getSimpleName());
        }
    }

    /**
     * 获取指定语言的解析器
     * 
     * @param language 编程语言
     * @return 对应的解析器，如果不存在则返回null
     */
    public SourceParser getParser(ProgrammingLanguage language) {
        if (language == null) {
            return null;
        }
        return getParser(language.getCode());
    }

    /**
     * 获取指定语言的解析器
     * 
     * @param language 语言代码（如：java, python）
     * @return 对应的解析器
     */
    public SourceParser getParser(String language) {
        if (language == null) {
            return null;
        }
        return parserCache.get(language.toLowerCase());
    }

    /**
     * 检查是否支持指定语言
     * 
     * @param language 编程语言
     * @return 是否支持
     */
    public boolean isSupported(ProgrammingLanguage language) {
        return language != null && parserCache.containsKey(language.getCode().toLowerCase());
    }

    /**
     * 检查是否支持指定语言
     * 
     * @param language 语言代码
     * @return 是否支持
     */
    public boolean isSupported(String language) {
        return language != null && parserCache.containsKey(language.toLowerCase());
    }

    /**
     * 获取所有支持的解析器
     * 
     * @return 解析器列表
     */
    public Map<String, SourceParser> getAllParsers() {
        return Map.copyOf(parserCache);
    }

    /**
     * 获取已注册的解析器数量
     * 
     * @return 解析器数量
     */
    public int getParserCount() {
        return parserCache.size();
    }
}
