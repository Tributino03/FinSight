package dev.Tributino.FinSight.config;

import com.auth0.jwt.interfaces.DecodedJWT;
import dev.Tributino.FinSight.repository.UserRepository;
import dev.Tributino.FinSight.service.TokenBlacklistService;
import dev.Tributino.FinSight.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final TokenBlacklistService blacklistService;
    private final UserRepository userRepository;

    public SecurityFilter(
            TokenService tokenService,
            TokenBlacklistService blacklistService,
            UserRepository userRepository
    ) {
        this.tokenService = tokenService;
        this.blacklistService = blacklistService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = recoverToken(request);

        if (token != null) {

            DecodedJWT jwt = tokenService.decodeToken(token);

            if (jwt != null && !blacklistService.isRevoked(jwt.getId())) {

                String email = jwt.getSubject();

                if (email != null && !email.isBlank()) {
                    userRepository.findByEmail(email)
                            .filter(user -> user.isEnabled())
                            .ifPresent(user -> {
                                var authentication = new UsernamePasswordAuthenticationToken(
                                        user, null, user.getAuthorities()
                                );
                                authentication.setDetails(
                                        new WebAuthenticationDetailsSource().buildDetails(request)
                                );
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                            });
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        return authHeader.substring(7);
    }
}