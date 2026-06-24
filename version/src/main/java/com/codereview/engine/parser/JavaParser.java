package com.codereview.engine.parser;

import com.codereview.common.enums.ProgrammingLanguage;
import com.codereview.entity.ReviewRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Java代码解析器
 * 使用Eclipse JDT进行AST解析
 * 
 * 支持功能：
 * - 语法树生成
 * - 方法提取
 * - 变量追踪
 * - 类型检查
 * 
 * @author code-review-team
 */
@Component
@Slf4j
public class JavaParser implements SourceParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解析Java源代码文件
     * 
     * @param file 源代码文件
     * @param rules 适用规则列表
     * @return 解析结果
     * @throws ParseException 解析异常
     */
    @Override
    public ParseResult parse(SourceFile file, List<ReviewRule> rules) throws ParseException {
        // 参数校验
        if (file == null || file.getContent() == null) {
            throw ParseException.syntaxError("文件内容为空", file.getPath(), null);
        }

        try {
            // 创建AST解析器
            ASTParser parser = ASTParser.newParser(AST.JLS17);
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            
            // 设置源码内容
            String sourceCode = file.getContent();
            parser.setSource(sourceCode.toCharArray());
            
            // 设置编译器选项
            Map<String, String> options = JavaCore.getOptions();
            options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_17);
            options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_17);
            options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_17);
            parser.setCompilerOptions(options);
            
            // 绑定环境（需要至少一个绑定才能解析）
            String[] emptyClasspath = {""};
            parser.setEnvironment(emptyClasspath, new String[]{""}, new String[]{""}, true);
            parser.setUnitName(file.getPath());

            // 执行解析
            CompilationUnit cu = (CompilationUnit) parser.createAST(null);

            // 构建解析结果
            ParseResult result = ParseResult.success();
            result.setLineCount(cu.getLineNumber(sourceCode.length() - 1));
            
            // 提取方法信息
            List<ParseResult.MethodInfo> methods = extractMethods(cu);
            result.setMethods(methods);
            
            // 提取类信息
            List<ParseResult.ClassInfo> classes = extractClasses(cu);
            result.setClasses(classes);
            
            // 提取导入语句
            List<String> imports = extractImports(cu);
            result.setImports(imports);

            // 将AST转换为JSON（用于缓存和调试）
            result.setAstJson(convertASTToJson(cu));

            log.debug("Java文件解析成功: {}, 行数: {}, 方法数: {}, 类数: {}", 
                file.getPath(), result.getLineCount(), methods.size(), classes.size());

            return result;

        } catch (Exception e) {
            log.error("Java文件解析失败: {}, 错误: {}", file.getPath(), e.getMessage(), e);
            throw ParseException.syntaxError(
                "解析失败: " + e.getMessage(), 
                file.getPath(), 
                findErrorLine(e, file.getContent())
            );
        }
    }

    /**
     * 获取解析器支持的编程语言
     */
    @Override
    public List<String> getSupportedLanguages() {
        return List.of(ProgrammingLanguage.JAVA.getCode());
    }

    /**
     * 检查是否支持指定语言
     */
    @Override
    public boolean supports(String language) {
        return ProgrammingLanguage.JAVA.getCode().equalsIgnoreCase(language);
    }

    /**
     * 提取方法信息
     */
    private List<ParseResult.MethodInfo> extractMethods(CompilationUnit cu) {
        List<ParseResult.MethodInfo> methods = new ArrayList<>();
        
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                ParseResult.MethodInfo methodInfo = new ParseResult.MethodInfo();
                methodInfo.setName(node.getName().getIdentifier());
                methodInfo.setStartLine(cu.getLineNumber(node.getStartPosition()));
                methodInfo.setEndLine(cu.getLineNumber(node.getStartPosition() + node.getLength()));
                methodInfo.setParameterCount(node.parameters().size());
                
                if (node.getReturnType2() != null) {
                    methodInfo.setReturnType(node.getReturnType2().toString());
                }
                
                methodInfo.setLineCount(
                    methodInfo.getEndLine() - methodInfo.getStartLine() + 1
                );
                
                methods.add(methodInfo);
                return super.visit(node);
            }
        });
        
        return methods;
    }

    /**
     * 提取类信息
     */
    private List<ParseResult.ClassInfo> extractClasses(CompilationUnit cu) {
        List<ParseResult.ClassInfo> classes = new ArrayList<>();
        
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                if (node.isPackageMemberTypeDeclaration()) {
                    ParseResult.ClassInfo classInfo = new ParseResult.ClassInfo();
                    classInfo.setName(node.getName().getIdentifier());
                    classInfo.setStartLine(cu.getLineNumber(node.getStartPosition()));
                    classInfo.setEndLine(cu.getLineNumber(node.getStartPosition() + node.getLength()));
                    classInfo.setPackageName(cu.getPackage() != null ? 
                        cu.getPackage().getName().getFullyQualifiedName() : "");
                    
                    // 统计方法数量
                    int methodCount = 0;
                    for (Object body : node.bodyDeclarations()) {
                        if (body instanceof MethodDeclaration) {
                            methodCount++;
                        }
                    }
                    classInfo.setMethodCount(methodCount);
                    
                    classes.add(classInfo);
                }
                return super.visit(node);
            }
        });
        
        return classes;
    }

    /**
     * 提取导入语句
     */
    private List<String> extractImports(CompilationUnit cu) {
        List<String> imports = new ArrayList<>();
        
        for (Object importStatement : cu.imports()) {
            if (importStatement instanceof ImportDeclaration) {
                imports.add(importStatement.toString().trim());
            }
        }
        
        return imports;
    }

    /**
     * 将AST转换为JSON字符串（简化版本，仅用于缓存）
     */
    private String convertASTToJson(CompilationUnit cu) {
        try {
            // 简化AST表示，实际项目中可以保存更详细的结构
            java.util.Map<String, Object> astMap = new java.util.HashMap<>();
            astMap.put("classCount", cu.types().size());
            astMap.put("importCount", cu.imports().size());
            return objectMapper.writeValueAsString(astMap);
        } catch (Exception e) {
            log.warn("AST转JSON失败: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * 从异常中查找错误行号
     */
    private Integer findErrorLine(Exception e, String content) {
        if (content == null) return null;
        String[] lines = content.split("\n");
        for (int i = 0; i < Math.min(lines.length, 10); i++) {
            if (e.getMessage() != null && e.getMessage().contains(String.valueOf(i + 1))) {
                return i + 1;
            }
        }
        return null;
    }

    /**
     * 检测文件编码
     */
    public static Charset detectCharset(byte[] bytes) {
        // 简单实现：优先尝试UTF-8
        try {
            new String(bytes, StandardCharsets.UTF_8);
            return StandardCharsets.UTF_8;
        } catch (Exception e) {
            return Charset.forName("GBK");
        }
    }
}
