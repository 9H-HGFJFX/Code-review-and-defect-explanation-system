package com.codereview;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 代码审查系统主应用类
 * 
 * @author code-review-team
 */
@SpringBootApplication
@EnableScheduling
@MapperScan(value = {"com.codereview.mapper", "com.codereview.repository"}, nameGenerator = CodeReviewApplication.MapperBeanNameGenerator.class)
public class CodeReviewApplication {

    /**
     * 自定义Mapper Bean名称生成器，避免不同包下同名Mapper冲突
     */
    public static class MapperBeanNameGenerator implements BeanNameGenerator {
        @Override
        public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
            String beanClassName = definition.getBeanClassName();
            if (beanClassName != null) {
                return beanClassName.replace(".", "_");
            }
            return null;
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(CodeReviewApplication.class, args);
    }
}
