package ma.urbanops.service;

import ma.urbanops.entity.Incident;
import ma.urbanops.enums.IncidentStatus;
import ma.urbanops.enums.Severity;
import ma.urbanops.repository.AlertRepository;
import ma.urbanops.repository.IncidentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledTaskServiceTest {

    @Mock private IncidentRepository incidentRepository;
    @Mock private AlertRepository alertRepository;

    @InjectMocks private ScheduledTaskService scheduledTaskService;

    @Test
    void dailySummaryLog_shouldReadCounts() {
        when(incidentRepository.countByStatus(IncidentStatus.OPEN)).thenReturn(4L);
        when(incidentRepository.countByStatus(IncidentStatus.IN_PROGRESS)).thenReturn(2L);
        when(alertRepository.findCriticalUnacknowledged()).thenReturn(List.of());

        scheduledTaskService.dailySummaryLog();

        verify(incidentRepository).countByStatus(IncidentStatus.OPEN);
        verify(incidentRepository).countByStatus(IncidentStatus.IN_PROGRESS);
        verify(alertRepository).findCriticalUnacknowledged();
    }

    @Test
    void checkOverdueHighSeverityIncidents_shouldHandleOverdueAndEmptyResults() {
        when(incidentRepository.findBySeverityAndStatusAndCreatedAtBefore(
                eq(Severity.HIGH), eq(IncidentStatus.OPEN), any(LocalDateTime.class)))
                .thenReturn(List.of(Incident.builder()
                        .referenceCode("INC-0001")
                        .title("Cable expose")
                        .createdAt(LocalDateTime.now().minusHours(3))
                        .build()))
                .thenReturn(List.of());

        scheduledTaskService.checkOverdueHighSeverityIncidents();
        scheduledTaskService.checkOverdueHighSeverityIncidents();

        verify(incidentRepository, times(2)).findBySeverityAndStatusAndCreatedAtBefore(
                eq(Severity.HIGH), eq(IncidentStatus.OPEN), any(LocalDateTime.class));
    }

    @Test
    void hourlyStatsAggregation_shouldCountRecentIncidents() {
        when(incidentRepository.countCreatedAfter(any(LocalDateTime.class))).thenReturn(3L).thenReturn(0L);

        scheduledTaskService.hourlyStatsAggregation();
        scheduledTaskService.hourlyStatsAggregation();

        verify(incidentRepository, times(2)).countCreatedAfter(any(LocalDateTime.class));
    }
}
