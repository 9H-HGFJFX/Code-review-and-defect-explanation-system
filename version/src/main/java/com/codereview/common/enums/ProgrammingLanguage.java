package com.codereview.common.enums;

/**
 * 编程语言枚举
 * 定义系统支持的编程语言
 * 
 * @author code-review-team
 */
public enum ProgrammingLanguage {
    
    /**
     * Java语言
     */
    JAVA("java", "Java"),
    
    /**
     * Python语言
     */
    PYTHON("python", "Python"),
    
    /**
     * JavaScript语言
     */
    JAVASCRIPT("javascript", "JavaScript"),
    
    /**
     * TypeScript语言
     */
    TYPESCRIPT("typescript", "TypeScript"),
    
    /**
     * Go语言
     */
    GO("go", "Go"),
    
    /**
     * C/C++语言
     */
    CPP("cpp", "C/C++"),
    
    /**
     * Rust语言
     */
    RUST("rust", "Rust"),
    
    /**
     * 未知语言
     */
    UNKNOWN("unknown", "未知");

    private final String code;
    private final String displayName;

    ProgrammingLanguage(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 根据文件扩展名获取编程语言
     * 
     * @param extension 文件扩展名（不包含点号）
     * @return 对应的编程语言，如果无法识别则返回UNKNOWN
     */
    public static ProgrammingLanguage fromExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return UNKNOWN;
        }
        
        String ext = extension.toLowerCase();
        switch (ext) {
            case "java":
                return JAVA;
            case "py":
            case "pyw":
                return PYTHON;
            case "js":
            case "jsx":
            case "mjs":
                return JAVASCRIPT;
            case "ts":
            case "tsx":
                return TYPESCRIPT;
            case "go":
                return GO;
            case "c":
            case "cpp":
            case "cc":
            case "cxx":
            case "h":
            case "hpp":
                return CPP;
            case "rs":
                return RUST;
            default:
                return UNKNOWN;
        }
    }

    /**
     * 根据文件名获取编程语言
     * 
     * @param fileName 文件名
     * @return 对应的编程语言
     */
    public static ProgrammingLanguage fromFileName(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return UNKNOWN;
        }
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
        return fromExtension(extension);
    }

    public static ProgrammingLanguage fromName(String name) {
        if (name == null || name.isEmpty()) return UNKNOWN;
        try {
            return ProgrammingLanguage.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fromExtension(name);
        }
    }
}
