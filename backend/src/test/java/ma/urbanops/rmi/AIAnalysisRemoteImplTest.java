package ma.urbanops.rmi;

import ma.urbanops.dto.response.AIAnalysisResult;
import ma.urbanops.service.AIAnalysisService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.rmi.server.UnicastRemoteObject;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AIAnalysisRemoteImplTest {

    private AIAnalysisRemoteImpl remote;

    @AfterEach
    void tearDown() throws Exception {
        if (remote != null) {
            UnicastRemoteObject.unexportObject(remote, true);
        }
    }

    @Test
    void classifyIncident_shouldReturnAnalysisJson() throws Exception {
        AIAnalysisService service = mock(AIAnalysisService.class);
        when(service.analyze("desc", "Voirie")).thenReturn(AIAnalysisResult.builder()
                .category("Voirie")
                .severity("HIGH")
                .authorityName("Commune")
                .confidence(0.8)
                .fallbackUsed(false)
                .build());
        remote = new AIAnalysisRemoteImpl(service);

        String json = remote.classifyIncident("desc", "Voirie");

        assertTrue(json.contains("\"category\":\"Voirie\""));
        assertTrue(json.contains("\"severity\":\"HIGH\""));
        assertTrue(json.contains("\"fallback\":false"));
    }

    @Test
    void classifyIncident_whenAnalyzeFails_shouldUseFallback() throws Exception {
        AIAnalysisService service = mock(AIAnalysisService.class);
        when(service.analyze("desc", "Voirie")).thenThrow(new RuntimeException("down"));
        when(service.fallback("desc", "Voirie")).thenReturn(AIAnalysisResult.builder()
                .category("Voirie")
                .severity("MEDIUM")
                .authorityName("Commune")
                .confidence(null)
                .build());
        remote = new AIAnalysisRemoteImpl(service);

        String json = remote.classifyIncident("desc", "Voirie");

        assertTrue(json.contains("\"severity\":\"MEDIUM\""));
        assertTrue(json.contains("\"confidence\":0.0"));
        assertTrue(json.contains("\"fallback\":true"));
    }

    @Test
    void getSeverityAndPing_shouldReturnSimpleValues() throws Exception {
        AIAnalysisService service = mock(AIAnalysisService.class);
        when(service.analyze("danger", null)).thenReturn(AIAnalysisResult.builder().severity("HIGH").build());
        remote = new AIAnalysisRemoteImpl(service);

        assertEquals("HIGH", remote.getSeverity("danger"));
        assertTrue(remote.ping().startsWith("OK"));
    }

    @Test
    void getSeverity_whenAnalyzeFails_shouldReturnMedium() throws Exception {
        AIAnalysisService service = mock(AIAnalysisService.class);
        when(service.analyze("danger", null)).thenThrow(new RuntimeException("down"));
        remote = new AIAnalysisRemoteImpl(service);

        assertEquals("MEDIUM", remote.getSeverity("danger"));
    }
}
