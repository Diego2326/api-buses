package com.diego.api_buses.config

import com.diego.api_buses.security.AppUserDetailsService
import com.diego.api_buses.security.JwtAuthenticationFilter
import com.diego.api_buses.security.JwtService
import org.springframework.beans.factory.annotation.Value
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
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableMethodSecurity
class SecurityConfig(
    @Value("\${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:8081,http://127.0.0.1:8081}")
    private val allowedOrigins: String,
    @Value("\${app.cors.allowed-origin-patterns:}")
    private val allowedOriginPatterns: String,
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity, jwtService: JwtService, userDetailsService: AppUserDetailsService): SecurityFilterChain =
        http.csrf { it.disable() }
            .cors { }
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
                    .requestMatchers(HttpMethod.POST, "/api/v1/payments", "/api/v1/wallet/top-ups", "/payments", "/wallet/top-ups").hasAnyRole("ADMIN", "OPERATOR", "PASSENGER")
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/**",
                        "/buses/**",
                        "/stops/**",
                        "/routes/**",
                        "/fares/**",
                        "/payments/**",
                        "/users/**",
                        "/wallet/**",
                        "/dashboard",
                        "/operations-map",
                        "/reports/**",
                    ).hasAnyRole("ADMIN", "OPERATOR", "INSPECTOR", "PASSENGER")
                    .requestMatchers(
                        "/api/v1/**",
                        "/buses/**",
                        "/stops/**",
                        "/routes/**",
                        "/fares/**",
                        "/payments/**",
                        "/users/**",
                        "/wallet/**",
                        "/dashboard",
                        "/operations-map",
                        "/reports/**",
                    ).hasAnyRole("ADMIN", "OPERATOR")
                    .anyRequest().authenticated()
            }
            .addFilterBefore(JwtAuthenticationFilter(jwtService, userDetailsService), UsernamePasswordAuthenticationFilter::class.java)
            .build()

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = allowedOrigins.split(",").map(String::trim).filter(String::isNotEmpty)
        configuration.allowedOriginPatterns = allowedOriginPatterns.split(",").map(String::trim).filter(String::isNotEmpty)
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.maxAge = 3600
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
