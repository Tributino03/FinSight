package dev.Tributino.FinSight.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.enums.Role;
import dev.Tributino.FinSight.repository.UserRepository;
import dev.Tributino.FinSight.service.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.context.annotation.Import;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional
@Import(SecurityIntegrationTest.SecurityProbeController.class)
class SecurityIntegrationTest {

    private static final String PROTECTED_URL = "/api/security-probe";
    private static final String ADMIN_URL = "/api/admin/security-probe";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    @Value("${api.security.token.secret}")
    private String secret;

    /**
     * Endpoints existem apenas nos testes. Assim validamos a camada de segurança
     * sem depender de nenhum controller real da aplicação.
     */
    @RestController
    static class SecurityProbeController {

        @GetMapping(PROTECTED_URL)
        String protectedEndpoint() {
            return "ok";
        }

        @GetMapping(ADMIN_URL)
        String adminEndpoint() {
            return "ok";
        }
    }

    // ---------------------------------------------------------------
    // Cenário 1: usuário ativo + JWT válido
    // ---------------------------------------------------------------
    @Test
    @DisplayName("Usuário ativo com JWT válido acessa endpoint protegido")
    void activeUserWithValidTokenCanAccessProtectedEndpoint() throws Exception {
        User user = saveUser(Role.USER, true);

        mockMvc.perform(get(PROTECTED_URL).header("Authorization", bearer(tokenService.generateToken(user))))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------
    // Cenário 2: usuário desativado + JWT ainda válido
    // ---------------------------------------------------------------
    @Test
    @DisplayName("Usuário desativado com JWT ainda válido recebe 401")
    void disabledUserWithValidTokenReceives401() throws Exception {
        User user = saveUser(Role.USER, false);

        mockMvc.perform(get(PROTECTED_URL).header("Authorization", bearer(tokenService.generateToken(user))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    // ---------------------------------------------------------------
    // Cenário 3: token expirado
    // ---------------------------------------------------------------
    @Test
    @DisplayName("Token expirado não autentica o usuário")
    void expiredTokenDoesNotAuthenticate() throws Exception {
        User user = saveUser(Role.USER, true);
        String expired = buildToken(secret, user.getEmail(), Instant.now().minusSeconds(60));

        mockMvc.perform(get(PROTECTED_URL).header("Authorization", bearer(expired)))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------
    // Cenário 4: token inválido (lixo, assinatura errada, sem token)
    // ---------------------------------------------------------------
    @Test
    @DisplayName("Token malformado não autentica o usuário")
    void malformedTokenDoesNotAuthenticate() throws Exception {
        mockMvc.perform(get(PROTECTED_URL).header("Authorization", bearer("isto.nao.e.um.jwt")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token assinado com outra chave não autentica o usuário")
    void tokenSignedWithAnotherSecretDoesNotAuthenticate() throws Exception {
        User user = saveUser(Role.USER, true);
        String forged = buildToken("outra-chave-qualquer", user.getEmail(), Instant.now().plusSeconds(3600));

        mockMvc.perform(get(PROTECTED_URL).header("Authorization", bearer(forged)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Requisição sem token recebe 401")
    void requestWithoutTokenReceives401() throws Exception {
        mockMvc.perform(get(PROTECTED_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    // ---------------------------------------------------------------
    // Cenário 5: USER em rota administrativa
    // ---------------------------------------------------------------
    @Test
    @DisplayName("Usuário ativo com papel USER recebe 403 em /api/admin/**")
    void userRoleReceives403OnAdminEndpoint() throws Exception {
        User user = saveUser(Role.USER, true);

        mockMvc.perform(get(ADMIN_URL).header("Authorization", bearer(tokenService.generateToken(user))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    // ---------------------------------------------------------------
    // Cenário 6: ADMIN em rota administrativa
    // ---------------------------------------------------------------
    @Test
    @DisplayName("Usuário ativo com papel ADMIN acessa /api/admin/**")
    void adminRoleCanAccessAdminEndpoint() throws Exception {
        User admin = saveUser(Role.ADMIN, true);

        mockMvc.perform(get(ADMIN_URL).header("Authorization", bearer(tokenService.generateToken(admin))))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------
    private User saveUser(Role role, boolean enabled) {
        String email = "user-" + UUID.randomUUID() + "@finsight.test";

        User user = new User("Test User", email, passwordEncoder.encode("123456"));
        user.changeRole(role);

        if (!enabled) {
            user.disable();
        }

        return userRepository.save(user);
    }

    private String buildToken(String signingSecret, String subject, Instant expiresAt) {
        return JWT.create()
                .withIssuer("FinSight API")
                .withSubject(subject)
                .withExpiresAt(expiresAt)
                .sign(Algorithm.HMAC256(signingSecret));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}