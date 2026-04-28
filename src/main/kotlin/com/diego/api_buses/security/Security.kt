package com.diego.api_buses.security

import com.diego.api_buses.domain.UserEntity
import com.diego.api_buses.domain.OperationalStatus
import com.diego.api_buses.domain.UserRole
import com.diego.api_buses.dto.AuthUserResponse
import com.diego.api_buses.dto.LoginRequest
import com.diego.api_buses.dto.LoginResponse
import com.diego.api_buses.dto.RegisterRequest
import com.diego.api_buses.error.BusinessException
import com.diego.api_buses.repository.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Service
import org.springframework.web.filter.OncePerRequestFilter
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class UserPrincipal(val user: UserEntity) : UserDetails {
    override fun getAuthorities() = listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
    override fun getPassword() = user.passwordHash
    override fun getUsername() = user.email
}

@Service
class AppUserDetailsService(private val users: UserRepository) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails =
        users.findByEmail(username)?.let(::UserPrincipal) ?: throw UsernameNotFoundException("Usuario no encontrado.")
}

@Service
class JwtService(
    @Value("\${app.security.jwt-secret}") private val secret: String,
    @Value("\${app.security.jwt-expiration-minutes}") private val expirationMinutes: Long,
) {
    fun generate(user: UserEntity): String {
        val now = Instant.now()
        val exp = now.plusSeconds(expirationMinutes * 60)
        val header = """{"alg":"HS256","typ":"JWT"}""".base64Url()
        val payload = """{"sub":"${user.email}","role":"${user.role}","iat":${now.epochSecond},"exp":${exp.epochSecond}}""".base64Url()
        val signature = sign("$header.$payload")
        return "$header.$payload.$signature"
    }

    fun subject(token: String): String? {
        if (!isValid(token)) return null
        val payload = String(Base64.getUrlDecoder().decode(token.split(".")[1]), StandardCharsets.UTF_8)
        return Regex(""""sub"\s*:\s*"([^"]+)"""").find(payload)?.groupValues?.get(1)
    }

    private fun isValid(token: String): Boolean {
        val parts = token.split(".")
        if (parts.size != 3) return false
        if (sign("${parts[0]}.${parts[1]}") != parts[2]) return false
        val payload = String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
        val exp = Regex(""""exp"\s*:\s*(\d+)""").find(payload)?.groupValues?.get(1)?.toLongOrNull() ?: return false
        return Instant.now().epochSecond < exp
    }

    private fun sign(data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data.toByteArray(StandardCharsets.UTF_8)))
    }

    private fun String.base64Url() = Base64.getUrlEncoder().withoutPadding().encodeToString(toByteArray(StandardCharsets.UTF_8))
}

@Service
class AuthService(private val users: UserRepository, private val passwordEncoder: PasswordEncoder, private val jwtService: JwtService) {
    fun login(request: LoginRequest): LoginResponse {
        val user = users.findByEmail(request.email) ?: throw BusinessException("Credenciales invalidas.")
        val hash = user.passwordHash ?: throw BusinessException("Credenciales invalidas.")
        if (!passwordEncoder.matches(request.password, hash)) throw BusinessException("Credenciales invalidas.")
        return LoginResponse(jwtService.generate(user), AuthUserResponse(user.id, user.name, user.email, user.role, user.status))
    }

    fun register(request: RegisterRequest): LoginResponse {
        if (users.existsByEmail(request.email)) throw BusinessException("Ya existe un usuario registrado con ese email.")
        val user = users.save(
            UserEntity(
                name = request.name,
                email = request.email,
                passwordHash = passwordEncoder.encode(request.password),
                role = UserRole.PASSENGER,
                status = OperationalStatus.ACTIVE,
            ),
        )
        return LoginResponse(jwtService.generate(user), AuthUserResponse(user.id, user.name, user.email, user.role, user.status))
    }
}

class JwtAuthenticationFilter(private val jwtService: JwtService, private val userDetailsService: AppUserDetailsService) : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val token = request.getHeader("Authorization")?.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")
        val subject = token?.let(jwtService::subject)
        if (subject != null && SecurityContextHolder.getContext().authentication == null) {
            val details = userDetailsService.loadUserByUsername(subject)
            val auth = UsernamePasswordAuthenticationToken(details, null, details.authorities)
            auth.details = WebAuthenticationDetailsSource().buildDetails(request)
            SecurityContextHolder.getContext().authentication = auth
        }
        filterChain.doFilter(request, response)
    }
}

fun UserRole.springRole() = "ROLE_$name"
