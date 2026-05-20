package ma.urbanops.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.urbanops.dto.response.AIAnalysisResult;
import ma.urbanops.dto.response.ContentModerationResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIAnalysisServiceGeminiTest {

    @Mock private RestTemplate restTemplate;

    private AIAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new AIAnalysisService(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "endpoint", "https://gemini.test/generate");
    }

    @AfterEach
    void tearDown() {
        Thread.interrupted();
    }

    @Test
    void analyze_whenGeminiReturnsJson_shouldParseResult() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(geminiBody("""
                        {"category":"Electricite","severity":"HIGH","authorityName":"ONEE","authorityEmail":"onee@test.ma","reason":"Cable expose","confidence":0.93}
                        """)));

        AIAnalysisResult result = service.analyze("Cable electrique expose", "Electricite");

        assertEquals("Electricite", result.getCategory());
        assertEquals("HIGH", result.getSeverity());
        assertEquals("ONEE", result.getAuthorityName());
        assertEquals(0.93, result.getConfidence());
        assertFalse(result.getFallbackUsed());
    }

    @Test
    void analyze_whenGeminiReturnsMalformedJson_shouldUseFallback() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(geminiBody("not-json")));

        AIAnalysisResult result = service.analyze("poubelle pleine avec dechets", "Dechets");

        assertEquals("MEDIUM", result.getSeverity());
        assertEquals("Dechets", result.getCategory());
        assertTrue(result.getFallbackUsed());
    }

    @Test
    void analyze_whenGeminiResponseBodyIsNull_shouldUseFallback() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(null));

        AIAnalysisResult result = service.analyze("accident sur la route", "Transport");

        assertEquals("HIGH", result.getSeverity());
        assertTrue(result.getFallbackUsed());
    }

    @Test
    void analyze_whenRateLimitedAndSleepInterrupted_shouldReinterruptAndFallback() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "rate limit", null, null, null));
        Thread.currentThread().interrupt();

        AIAnalysisResult result = service.analyze("route bloquee", "Transport");

        assertTrue(Thread.currentThread().isInterrupted());
        assertTrue(result.getFallbackUsed());
        assertEquals("MEDIUM", result.getSeverity());
    }

    @Test
    void moderateIncidentContent_whenGeminiAcceptsAndLocalAccepts_shouldReturnAiResult() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(geminiBody("""
                        {"accepted":true,"reason":"Incident urbain clair","confidence":0.88}
                        """)));

        ContentModerationResult result = service.moderateIncidentContent(
                "Fuite d'eau rue principale",
                "Il y a une fuite d'eau importante dans la rue principale",
                "Eau",
                "Medina");

        assertTrue(result.getAccepted());
        assertEquals("Incident urbain clair", result.getReason());
        assertFalse(result.getFallbackUsed());
    }

    @Test
    void moderateIncidentContent_whenGeminiAcceptsButLocalRejects_shouldReturnStrictRejection() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(geminiBody("""
                        {"accepted":true,"reason":"OK","confidence":0.99}
                        """)));

        ContentModerationResult result = service.moderateIncidentContent(
                "azerty qwerty",
                "asdf asdf random text",
                "Voirie",
                "Gueliz");

        assertFalse(result.getAccepted());
        assertTrue(result.getReason().contains("controle strict"));
    }

    @Test
    void moderateIncidentContent_whenGeminiReturnsMalformedJson_shouldRejectWithFallback() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(geminiBody("not-json")));

        ContentModerationResult result = service.moderateIncidentContent(
                "Fuite d'eau rue principale",
                "Il y a une fuite d'eau importante dans la rue principale",
                "Eau",
                "Medina");

        assertFalse(result.getAccepted());
        assertTrue(result.getFallbackUsed());
        assertEquals("Reponse IA de moderation illisible", result.getReason());
    }

    @Test
    void fallback_shouldClassifyKeywordBasedSeverity() {
        assertEquals("HIGH", service.fallback("court-circuit et cable electrique", "Electricite").getSeverity());
        assertEquals("HIGH", service.fallback("feu et accident avec blesse", "Securite").getSeverity());
        assertEquals("HIGH", service.fallback("fuite de gaz et inondation", "Eau").getSeverity());
        assertEquals("MEDIUM", service.fallback("route bloquee avec trafic", "Transport").getSeverity());
        assertEquals("MEDIUM", service.fallback("lampadaire sans lumiere", "Eclairage").getSeverity());
        assertEquals("LOW", service.fallback("poubelle pleine de dechets", "Dechets").getSeverity());
        assertEquals("Voirie", service.fallback(null, null).getCategory());
    }

    @Test
    void fallbackModeration_shouldRejectShortRepetitiveAndSymbolHeavyContent() {
        assertFalse(service.fallbackModeration("court", "texte").getAccepted());
        assertFalse(service.fallbackModeration("aaaaaaaaaaaaaaaaaaaaaaaa", "route route").getAccepted());
        assertFalse(service.fallbackModeration("!!!! #### $$$$", "1234567890 !!!!").getAccepted());
    }

    private Map<String, Object> geminiBody(String text) {
        return Map.of(
                "candidates", List.of(Map.of(
                        "content", Map.of(
                                "parts", List.of(Map.of("text", text))
                        )
                ))
        );
    }
}
