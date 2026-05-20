package ma.urbanops.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("GET", "/api/test");
    }

    @Test
    void handleResourceNotFoundException_shouldReturnNotFound() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleResourceNotFoundException(new ResourceNotFoundException("Missing"), request);

        assertError(response, HttpStatus.NOT_FOUND, "Missing");
    }

    @Test
    void handleUnauthorizedException_shouldReturnUnauthorized() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleUnauthorizedException(new UnauthorizedException("No token"), request);

        assertError(response, HttpStatus.UNAUTHORIZED, "No token");
    }

    @Test
    void handleAccessDeniedException_shouldReturnForbidden() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleAccessDeniedException(new AccessDeniedException("denied"), request);

        assertError(response, HttpStatus.FORBIDDEN, "insufficient permissions");
    }

    @Test
    void handleAIAnalysisException_shouldReturnBadGateway() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleAIAnalysisException(new AIAnalysisException("timeout"), request);

        assertError(response, HttpStatus.BAD_GATEWAY, "AI analysis service error: timeout");
    }

    @Test
    void handleValidationException_shouldReturnFieldErrors() throws Exception {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new FieldError("request", "title", "title required"));
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyValidationTarget", String.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(new MethodParameter(method, 0), binding);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleValidationException(ex, request);

        assertError(response, HttpStatus.BAD_REQUEST, "title required");
        assertTrue(response.getBody().getMessage().contains("title"));
    }

    @Test
    void handleMaxUploadSizeExceededException_shouldReturnPayloadTooLarge() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleMaxUploadSizeExceededException(new MaxUploadSizeExceededException(10), request);

        assertError(response, HttpStatus.PAYLOAD_TOO_LARGE, "File size exceeds maximum allowed");
    }

    @Test
    void handleIllegalArgumentException_shouldReturnBadRequest() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleIllegalArgumentException(new IllegalArgumentException("bad input"), request);

        assertError(response, HttpStatus.BAD_REQUEST, "bad input");
    }

    @Test
    void handleAuthenticationFailures_shouldReturnExpectedStatuses() {
        assertError(handler.handleBadCredentials(new BadCredentialsException("bad"), request),
                HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect.");
        assertError(handler.handleDisabled(new DisabledException("disabled"), request),
                HttpStatus.FORBIDDEN, "Ce compte est");
        assertError(handler.handleAuthentication(new AuthenticationException("auth failed") {}, request),
                HttpStatus.UNAUTHORIZED, "Authentification impossible");
    }

    @Test
    void handleGlobalException_shouldReturnMessageOrDefault() {
        assertError(handler.handleGlobalException(new RuntimeException("boom"), request),
                HttpStatus.INTERNAL_SERVER_ERROR, "boom");
        assertError(handler.handleGlobalException(new RuntimeException(), request),
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    @Test
    void errorResponse_shouldExposePojoAccessors() {
        GlobalExceptionHandler.ErrorResponse error = new GlobalExceptionHandler.ErrorResponse();
        error.setStatus(418);
        error.setError("Teapot");
        error.setMessage("short and stout");
        error.setPath("/tea");

        assertEquals(418, error.getStatus());
        assertEquals("Teapot", error.getError());
        assertEquals("short and stout", error.getMessage());
        assertEquals("/tea", error.getPath());
    }

    @SuppressWarnings("unused")
    private void dummyValidationTarget(String value) {
    }

    private void assertError(ResponseEntity<GlobalExceptionHandler.ErrorResponse> response,
                             HttpStatus status,
                             String messageFragment) {
        assertEquals(status, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(status.value(), response.getBody().getStatus());
        assertEquals("/api/test", response.getBody().getPath());
        assertTrue(response.getBody().getMessage().contains(messageFragment));
        assertNotNull(response.getBody().getTimestamp());
    }
}
