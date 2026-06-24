package com.codereview.engine.parser;

import com.codereview.common.enums.ProgrammingLanguage;
import lombok.Data;

/**
 * 源代码文件实体类
 * 封装待解析的源代码文件信息
 * 
 * @author code-review-team
 */
@Data
public class SourceFile {
    
    /**
     * 文件路径
     */
    private String path;
    
    /**
     * 编程语言
     * @see com.codereview.common.enums.ProgrammingLanguage
     */
    private ProgrammingLanguage language;
    
    /**
     * 文件内容（字符串形式）
     */
    private String content;
    
    /**
     * 原始字节内容
     */
    private byte[] rawContent;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 文件行数
     */
    private Integer lineCount;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件扩展名
     */
    private String extension;

    /**
     * 创建SourceFile的Builder
     * 
     * @return Builder实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * SourceFile构建器
     */
    public static class Builder {
        private final SourceFile sourceFile = new SourceFile();

        public Builder path(String path) {
            sourceFile.setPath(path);
            // 从路径中提取文件名
            if (path != null && path.contains("/")) {
                sourceFile.setFileName(path.substring(path.lastIndexOf("/") + 1));
            } else if (path != null && path.contains("\\")) {
                sourceFile.setFileName(path.substring(path.lastIndexOf("\\") + 1));
            } else {
                sourceFile.setFileName(path);
            }
            // 从文件名提取扩展名
            if (sourceFile.getFileName() != null && sourceFile.getFileName().contains(".")) {
                sourceFile.setExtension(
                    sourceFile.getFileName().substring(sourceFile.getFileName().lastIndexOf(".") + 1).toLowerCase()
                );
                // 自动识别语言
                sourceFile.setLanguage(ProgrammingLanguage.fromExtension(sourceFile.getExtension()));
            }
            return this;
        }

        public Builder content(String content) {
            sourceFile.setContent(content);
            if (content != null) {
                sourceFile.setLineCount((int) content.lines().count());
            }
            return this;
        }

        public Builder rawContent(byte[] rawContent) {
            sourceFile.setRawContent(rawContent);
            if (rawContent != null) {
                sourceFile.setFileSize((long) rawContent.length);
            }
            return this;
        }

        public Builder language(ProgrammingLanguage language) {
            sourceFile.setLanguage(language);
            return this;
        }

        public Builder fileSize(Long fileSize) {
            sourceFile.setFileSize(fileSize);
            return this;
        }

        public Builder lineCount(Integer lineCount) {
            sourceFile.setLineCount(lineCount);
            return this;
        }

        public Builder fileName(String fileName) {
            sourceFile.setFileName(fileName);
            return this;
        }

        public Builder extension(String extension) {
            sourceFile.setExtension(extension);
            return this;
        }

        public SourceFile build() {
            return sourceFile;
        }
    }
}
