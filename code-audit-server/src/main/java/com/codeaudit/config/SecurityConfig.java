package com.codeaudit.config;

import com.codeaudit.security.JwtAuthenticationFilter;
import com.codeaudit.security.RestAuthEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CorsFilter;

/**
 * Spring Security 配置 - JWT 无状态认证
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthEntryPoint restAuthEntryPoint;
    private final CorsFilter corsFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 关闭 CSRF
            .csrf(csrf -> csrf.disable())
            // 启用 CORS
            .cors(cors -> {})
            // 关闭表单登录
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            // 无状态 Session
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 异常处理
            .exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthEntryPoint))
            // 授权规则
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/auth/refresh",
                    "/doc.html",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**",
                    "/favicon.ico",
                    "/error"
                ).permitAll()
                // 教师/管理员专属
                .requestMatchers("/api/rule/add", "/api/rule/update/**", "/api/rule/delete/**")
                    .hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers("/api/class/**", "/api/statistic/**")
                    .hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // 其他都需要登录
                .anyRequest().authenticated()
            )
            // JWT 过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(corsFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}
