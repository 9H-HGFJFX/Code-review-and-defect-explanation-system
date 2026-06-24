package com.codereview.config;

import com.codereview.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security配置
 * 配置JWT认证、RBAC权限控制、CORS等
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 禁用CSRF（使用JWT Header认证，天然防御CSRF）
        http.csrf(AbstractHttpConfigurer::disable);

        // 配置CORS
        http.cors(cors -> {});

        // 配置会话管理为无状态（使用JWT）
        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        // 临时禁用安全验证（dev模式调试）：直接全部 permitAll，不写其他规则
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        // 添加JWT认证过滤器
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 占位：原来的细粒度规则已禁用，方便调试
     */
    @Deprecated
    private void deprecatedRules(HttpSecurity http) throws Exception {
        // 配置请求授权
        http.authorizeHttpRequests(auth -> auth
                // 公开路径
                .requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/error").permitAll()

                // 管理员专用路径
                .requestMatchers(HttpMethod.DELETE, "/api/tasks/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/api/rules/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/api/classes/**").hasAnyRole("SUPER_ADMIN", "TEACHER")

                // 教师专用路径（SUPER_ADMIN不能创建任务）
                .requestMatchers(HttpMethod.POST, "/api/tasks").hasRole("TEACHER")
                .requestMatchers(HttpMethod.PATCH, "/api/tasks/*/status").hasRole("TEACHER")
                .requestMatchers("/api/issues/*/assign").hasRole("TEACHER")

                // 其他路径需要认证
                .anyRequest().authenticated()
        );

        // 添加JWT认证过滤器
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }

    /**
     * 密码编码器
     * 使用BCrypt，强哈希算法，天然防时序攻击
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // cost factor = 12，满足安全要求
        return new BCryptPasswordEncoder(12);
    }
}
