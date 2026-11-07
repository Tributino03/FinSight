package dev.Tributino.FinSight.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import dev.Tributino.FinSight.domain.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final TokenBlacklistService blacklistService;

    public AuthService(
            AuthenticationManager authenticationManager,
            TokenService tokenService,
            TokenBlacklistService blacklistService
    ) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.blacklistService = blacklistService;
    }

    public String login(String email, String password) {
        var credentials = new UsernamePasswordAuthenticationToken(email, password);

        Authentication authentication = authenticationManager.authenticate(credentials);

        User user = (User) authentication.getPrincipal();

        return tokenService.generateToken(user);
    }

    public void logout(String token) {
        DecodedJWT jwt = tokenService.decodeToken(token);

        if (jwt == null || jwt.getId() == null) {
            return;
        }

        blacklistService.revoke(jwt.getId(), jwt.getExpiresAtAsInstant());
    }
}