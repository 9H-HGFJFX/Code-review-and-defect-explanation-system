package com.codereview.engine.parser;

import com.codereview.common.enums.ProgrammingLanguage;
import com.codereview.entity.ReviewRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Python代码解析器
 * 使用Python内置ast模块进行解析
 * 
 * 支持功能：
 * - 语法树生成
 * - 函数提取
 * - 类提取
 * - 导入语句分析
 * 
 * @author code-review-team
 */
@Component
@Slf4j
public class PythonParser implements SourceParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Python AST解析脚本
     * 使用ProcessBuilder调用Python解释器执行
     */
    private static final String PYTHON_AST_SCRIPT = 
        "import ast\n" +
        "import json\n" +
        "import sys\n" +
        "\n" +
        "try:\n" +
        "    source = sys.stdin.read()\n" +
        "    tree = ast.parse(source)\n" +
        "\n" +
        "    result = {\n" +
        "        'success': True,\n" +
        "        'classes': [],\n" +
        "        'functions': [],\n" +
        "        'imports': [],\n" +
        "        'lineCount': len(source.split('\\n'))\n" +
        "    }\n" +
        "\n" +
        "    for node in ast.walk(tree):\n" +
        "        if isinstance(node, ast.ClassDef):\n" +
        "            class_info = {\n" +
        "                'name': node.name,\n" +
        "                'line': node.lineno,\n" +
        "                'methodCount': len([n for n in node.body if isinstance(n, ast.FunctionDef)])\n" +
        "            }\n" +
        "            result['classes'].append(class_info)\n" +
        "        elif isinstance(node, ast.FunctionDef) and node.col_offset == 0:\n" +
        "            func_info = {\n" +
        "                'name': node.name,\n" +
        "                'line': node.lineno,\n" +
        "                'args': len(node.args.args)\n" +
        "            }\n" +
        "            result['functions'].append(func_info)\n" +
        "        elif isinstance(node, ast.Import):\n" +
        "            for alias in node.names:\n" +
        "                result['imports'].append(alias.name)\n" +
        "        elif isinstance(node, ast.ImportFrom):\n" +
        "            if node.module:\n" +
        "                result['imports'].append(node.module)\n" +
        "\n" +
        "    print(json.dumps(result))\n" +
        "except Exception as e:\n" +
        "    print(json.dumps({'success': False, 'error': str(e)}))\n";

    /**
     * 解析Python源代码文件
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
            // 调用Python解释器进行解析
            String jsonResult = executePythonAst(file.getContent());
            
            // 解析JSON结果
            Map<String, Object> resultMap = objectMapper.readValue(jsonResult, Map.class);
            
            ParseResult result = new ParseResult();
            result.setSuccess((Boolean) resultMap.getOrDefault("success", false));
            
            if (!result.isSuccess()) {
                String errorMsg = (String) resultMap.getOrDefault("error", "未知错误");
                throw ParseException.syntaxError("Python解析失败: " + errorMsg, file.getPath(), null);
            }
            
            // 填充结果
            result.setLineCount((Integer) resultMap.getOrDefault("lineCount", 0));
            result.setAstJson(jsonResult);
            
            // 提取类信息
            List<Map<String, Object>> classList = (List<Map<String, Object>>) resultMap.getOrDefault("classes", new ArrayList<>());
            List<ParseResult.ClassInfo> classes = new ArrayList<>();
            for (Map<String, Object> classMap : classList) {
                ParseResult.ClassInfo classInfo = new ParseResult.ClassInfo();
                classInfo.setName((String) classMap.get("name"));
                classInfo.setStartLine((Integer) classMap.get("line"));
                classInfo.setMethodCount((Integer) classMap.getOrDefault("methodCount", 0));
                classes.add(classInfo);
            }
            result.setClasses(classes);
            
            // 提取函数信息
            List<Map<String, Object>> funcList = (List<Map<String, Object>>) resultMap.getOrDefault("functions", new ArrayList<>());
            List<ParseResult.MethodInfo> methods = new ArrayList<>();
            for (Map<String, Object> funcMap : funcList) {
                ParseResult.MethodInfo methodInfo = new ParseResult.MethodInfo();
                methodInfo.setName((String) funcMap.get("name"));
                methodInfo.setStartLine((Integer) funcMap.get("line"));
                methodInfo.setParameterCount((Integer) funcMap.getOrDefault("args", 0));
                methods.add(methodInfo);
            }
            result.setMethods(methods);
            
            // 提取导入语句
            List<String> imports = (List<String>) resultMap.getOrDefault("imports", new ArrayList<>());
            result.setImports(imports);
            
            log.debug("Python文件解析成功: {}, 行数: {}, 类数: {}, 函数数: {}", 
                file.getPath(), result.getLineCount(), classes.size(), methods.size());
            
            return result;
            
        } catch (ParseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Python文件解析失败: {}, 错误: {}", file.getPath(), e.getMessage(), e);
            throw ParseException.syntaxError(
                "Python解析失败: " + e.getMessage(), 
                file.getPath(), 
                null
            );
        }
    }

    /**
     * 执行Python AST解析脚本
     * 
     * @param sourceCode Python源代码
     * @return JSON格式的解析结果
     */
    private String executePythonAst(String sourceCode) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("python", "-c", PYTHON_AST_SCRIPT);
        pb.redirectErrorStream(true);
        
        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            // 尝试python3
            pb.command("python3", "-c", PYTHON_AST_SCRIPT);
            process = pb.start();
        }
        
        // 写入源代码到stdin
        try (OutputStream os = process.getOutputStream()) {
            os.write(sourceCode.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
        
        // 读取结果
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }
        
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.warn("Python进程退出码: {}", exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Python解析被中断", e);
        }
        
        return output.toString();
    }

    /**
     * 获取解析器支持的编程语言
     */
    @Override
    public List<String> getSupportedLanguages() {
        return List.of(ProgrammingLanguage.PYTHON.getCode());
    }

    /**
     * 检查是否支持指定语言
     */
    @Override
    public boolean supports(String language) {
        return ProgrammingLanguage.PYTHON.getCode().equalsIgnoreCase(language);
    }
}
