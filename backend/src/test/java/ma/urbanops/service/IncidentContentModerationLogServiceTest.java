package ma.urbanops.service;

import ma.urbanops.dto.response.ContentModerationResult;
import ma.urbanops.entity.Category;
import ma.urbanops.entity.IncidentContentModerationLog;
import ma.urbanops.entity.Sector;
import ma.urbanops.entity.User;
import ma.urbanops.repository.IncidentContentModerationLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IncidentContentModerationLogServiceTest {

    @Mock private IncidentContentModerationLogRepository repository;

    @InjectMocks private IncidentContentModerationLogService service;

    @Test
    void log_shouldPersistModerationDetails() {
        Category category = Category.builder().name("Voirie").build();
        Sector sector = Sector.builder().name("Gueliz").build();
        User reporter = User.builder().email("citizen@test.ma").build();

        service.log(5L, "Title", "Description", category, sector, reporter,
                ContentModerationResult.builder()
                        .accepted(true)
                        .reason("OK")
                        .confidence(0.8)
                        .fallbackUsed(false)
                        .build());

        ArgumentCaptor<IncidentContentModerationLog> captor = ArgumentCaptor.forClass(IncidentContentModerationLog.class);
        verify(repository).save(captor.capture());
        IncidentContentModerationLog log = captor.getValue();
        assertEquals(5L, log.getIncidentId());
        assertEquals("citizen@test.ma", log.getReporterEmail());
        assertEquals("Voirie", log.getCategoryName());
        assertEquals("Gueliz", log.getSectorName());
        assertTrue(log.getAccepted());
        assertEquals(0.8, log.getConfidence());
    }

    @Test
    void log_whenOptionalValuesMissing_shouldUseNullsAndDefaultReason() {
        service.log(null, "Title", "Description", null, null, null, null);

        ArgumentCaptor<IncidentContentModerationLog> captor = ArgumentCaptor.forClass(IncidentContentModerationLog.class);
        verify(repository).save(captor.capture());
        IncidentContentModerationLog log = captor.getValue();
        assertFalse(log.getAccepted());
        assertNull(log.getReporterEmail());
        assertNull(log.getCategoryName());
        assertEquals("Moderation result missing", log.getReason());
    }
}
