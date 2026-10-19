
package org.entrypulse.entrypulse.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Diagnostic log: confirm whether the request carries a Bearer token.
        log.info(
                "Incoming request: method={}, path={}, bearerPresent={}",
                request.getMethod(),
                request.getRequestURI(),
                header != null && header.startsWith("Bearer ")
        );

        if (header != null && header.startsWith("Bearer ")) {
            try {
                String token = header.substring(7);
                Claims claims = jwtService.parse(token);

                String userId = claims.getSubject();
                String role = claims.get("role", String.class);

                // Diagnostic log: confirm that the token was accepted.
                // Never log the actual token or secret.
                log.info(
                        "JWT accepted for path={}, subject={}, role={}",
                        request.getRequestURI(),
                        userId,
                        role
                );

                var authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                List.of(
                                        new SimpleGrantedAuthority(
                                                "ROLE_" + role
                                        )
                                )
                        );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);

            } catch (JwtException | IllegalArgumentException e) {
                log.warn(
                        "JWT rejected for path={}: {}",
                        request.getRequestURI(),
                        e.getMessage()
                );

                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }
}
