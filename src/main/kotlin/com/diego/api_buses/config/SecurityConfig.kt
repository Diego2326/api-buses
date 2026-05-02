package com.diego.api_buses.config

import com.diego.api_buses.security.AppUserDetailsService
import com.diego.api_buses.security.JwtAuthenticationFilter
import com.diego.api_buses.security.JwtService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableMethodSecurity
class SecurityConfig {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity, jwtService: JwtService, userDetailsService: AppUserDetailsService): SecurityFilterChain =
        http.csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout", "/auth/logout").authenticated()
                    .requestMatchers("/api/v1/auth/me", "/auth/me").authenticated()
                    .requestMatchers(
                        "/api/v1/auth/login",
                        "/api/v1/auth/register",
                        "/api/v1/auth/login/",
                        "/api/v1/auth/register/",
                        "/auth/login",
                        "/auth/register",
                        "/auth/login/",
                        "/auth/register/",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                    ).permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/payments", "/api/v1/wallet/top-ups").hasAnyRole("ADMIN", "OPERATOR", "PASSENGER")
                    .requestMatchers(HttpMethod.GET, "/api/v1/**").hasAnyRole("ADMIN", "OPERATOR", "INSPECTOR", "PASSENGER")
                    .requestMatchers("/api/v1/**").hasAnyRole("ADMIN", "OPERATOR")
                    .anyRequest().authenticated()
            }
            .addFilterBefore(JwtAuthenticationFilter(jwtService, userDetailsService), UsernamePasswordAuthenticationFilter::class.java)
            .build()
}
