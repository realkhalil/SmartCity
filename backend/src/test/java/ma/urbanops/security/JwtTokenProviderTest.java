package ma.urbanops.security;

import ma.urbanops.entity.User;
import ma.urbanops.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        String secret = Base64.getEncoder().encodeToString(
                "urbanops-test-secret-with-enough-bytes-for-hs512-signing-and-then-some".getBytes(StandardCharsets.UTF_8));
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", secret);
        ReflectionTestUtils.setField(tokenProvider, "jwtExpirationInMs", 60_000L);
        ReflectionTestUtils.setField(tokenProvider, "refreshExpirationInMs", 120_000L);
    }

    @Test
    void generateToken_shouldIncludeUsernameAndValidate() {
        UserDetailsImpl userDetails = UserDetailsImpl.build(User.builder()
                .id(1L)
                .firstName("Yassine")
                .email("yassine@test.ma")
                .password("secret")
                .role(Role.ADMIN)
                .build());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, "secret", userDetails.getAuthorities());

        String token = tokenProvider.generateToken(authentication);

        assertTrue(tokenProvider.validateToken(token));
        assertEquals("yassine@test.ma", tokenProvider.getUsernameFromJWT(token));
        assertEquals(60_000L, tokenProvider.getExpirationTime());
    }

    @Test
    void generateTokenFromUsernameAndRefreshToken_shouldValidate() {
        String access = tokenProvider.generateTokenFromUsername("citizen@test.ma");
        String refresh = tokenProvider.generateRefreshToken("citizen@test.ma");

        assertTrue(tokenProvider.validateToken(access));
        assertTrue(tokenProvider.validateToken(refresh));
        assertEquals("citizen@test.ma", tokenProvider.getUsernameFromJWT(refresh));
    }

    @Test
    void validateToken_shouldReturnFalseForInvalidTokens() {
        assertFalse(tokenProvider.validateToken("not-a-jwt"));
        assertFalse(tokenProvider.validateToken(""));
    }

    @Test
    void validateToken_shouldReturnFalseForExpiredToken() {
        ReflectionTestUtils.setField(tokenProvider, "jwtExpirationInMs", -1L);

        String expired = tokenProvider.generateTokenFromUsername("old@test.ma");

        assertFalse(tokenProvider.validateToken(expired));
    }
}
