package ma.urbanops.service;

import ma.urbanops.entity.Alert;
import ma.urbanops.entity.Category;
import ma.urbanops.entity.Incident;
import ma.urbanops.entity.Sector;
import ma.urbanops.enums.Severity;
import ma.urbanops.exception.ResourceNotFoundException;
import ma.urbanops.jms.AlertProducer;
import ma.urbanops.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock private AlertRepository alertRepository;
    @Mock private AlertProducer alertProducer;

    @InjectMocks private AlertService alertService;

    private Incident incident;

    @BeforeEach
    void setUp() {
        Category category = Category.builder()
                .id(1L)
                .name("Voirie")
                .defaultAuthority("Service voirie")
                .authorityEmail("voirie@marrakech.ma")
                .build();
        Sector sector = Sector.builder()
                .id(2L)
                .name("Gueliz")
                .build();
        incident = Incident.builder()
                .id(10L)
                .referenceCode("INC-0010")
                .title("Lampadaire dangereux")
                .description("Cable expose pres de la route")
                .category(category)
                .sector(sector)
                .severity(Severity.HIGH)
                .latitude(31.63)
                .longitude(-8.0)
                .build();
    }

    @Test
    void createAndSendAlert_whenAuthorityEmailExists_shouldPersistAndQueueAlert() {
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Alert result = alertService.createAndSendAlert(incident);

        assertEquals(incident, result.getIncident());
        assertEquals(Severity.HIGH, result.getSeverity());
        assertEquals("Lampadaire dangereux", result.getTitle());
        assertEquals("[HIGH] Lampadaire dangereux in Gueliz - Cable expose pres de la route", result.getMessage());
        assertEquals("voirie@marrakech.ma", result.getSentTo());
        assertTrue(result.getEmailSent());
        assertFalse(result.getAcknowledged());

        verify(alertProducer).sendAlertToQueue(incident);
        verify(alertRepository, times(2)).save(any(Alert.class));
    }

    @Test
    void createAndSendAlert_whenAuthorityEmailIsBlank_shouldOnlyPersistAlert() {
        incident.getCategory().setAuthorityEmail("");
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Alert result = alertService.createAndSendAlert(incident);

        assertFalse(result.getEmailSent());
        assertEquals("", result.getSentTo());
        verify(alertProducer, never()).sendAlertToQueue(any());
        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void resendAlert_whenAlertExistsWithEmail_shouldQueueAndMarkEmailSent() {
        Alert alert = Alert.builder()
                .id(5L)
                .incident(incident)
                .emailSent(false)
                .build();
        when(alertRepository.findById(5L)).thenReturn(Optional.of(alert));

        alertService.resendAlert(5L);

        assertTrue(alert.getEmailSent());
        verify(alertProducer).sendAlertToQueue(incident);
        verify(alertRepository).save(alert);
    }

    @Test
    void resendAlert_whenAlertHasNoAuthorityEmail_shouldNotQueue() {
        incident.getCategory().setAuthorityEmail(null);
        Alert alert = Alert.builder()
                .id(5L)
                .incident(incident)
                .emailSent(false)
                .build();
        when(alertRepository.findById(5L)).thenReturn(Optional.of(alert));

        alertService.resendAlert(5L);

        assertFalse(alert.getEmailSent());
        verify(alertProducer, never()).sendAlertToQueue(any());
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void resendAlert_whenAlertMissing_shouldThrowResourceNotFoundException() {
        when(alertRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> alertService.resendAlert(404L));
    }

    @Test
    void acknowledgeAlert_shouldAcknowledgeAndSaveAlert() {
        Alert alert = Alert.builder()
                .id(8L)
                .incident(incident)
                .acknowledged(false)
                .build();
        when(alertRepository.findById(8L)).thenReturn(Optional.of(alert));

        alertService.acknowledgeAlert(8L);

        assertTrue(alert.getAcknowledged());
        assertNotNull(alert.getAcknowledgedAt());
        verify(alertRepository).save(alert);
    }

    @Test
    void acknowledgeAlert_whenAlertMissing_shouldThrowResourceNotFoundException() {
        when(alertRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> alertService.acknowledgeAlert(404L));
    }

    @Test
    void getCriticalUnacknowledgedAlerts_shouldAskRepositoryForHighSeverity() {
        Alert alert = Alert.builder().id(1L).incident(incident).severity(Severity.HIGH).build();
        when(alertRepository.findCriticalUnacknowledged(Severity.HIGH)).thenReturn(List.of(alert));

        List<Alert> result = alertService.getCriticalUnacknowledgedAlerts();

        assertEquals(List.of(alert), result);
        verify(alertRepository).findCriticalUnacknowledged(Severity.HIGH);
    }

    @Test
    void getAllUnacknowledgedAlerts_shouldDelegateToRepository() {
        Alert alert = Alert.builder().id(1L).incident(incident).build();
        when(alertRepository.findAllUnacknowledgedOrderBySeverity()).thenReturn(List.of(alert));

        List<Alert> result = alertService.getAllUnacknowledgedAlerts();

        assertEquals(List.of(alert), result);
    }

    @Test
    void getRecentAlerts_shouldUseRequestedLimitAsPageSize() {
        Alert alert = Alert.builder().id(1L).incident(incident).build();
        when(alertRepository.findRecentAlerts(any(Pageable.class))).thenReturn(List.of(alert));

        List<Alert> result = alertService.getRecentAlerts(3);

        assertEquals(List.of(alert), result);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(alertRepository).findRecentAlerts(pageableCaptor.capture());
        assertEquals(3, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void getAllAlerts_shouldReturnRepositoryPage() {
        Pageable pageable = Pageable.ofSize(2);
        Page<Alert> page = new PageImpl<>(List.of(Alert.builder().id(1L).incident(incident).build()));
        when(alertRepository.findAll(pageable)).thenReturn(page);

        assertSame(page, alertService.getAllAlerts(pageable));
    }

    @Test
    void findById_whenAlertExists_shouldReturnAlert() {
        Alert alert = Alert.builder().id(7L).incident(incident).build();
        when(alertRepository.findById(7L)).thenReturn(Optional.of(alert));

        assertSame(alert, alertService.findById(7L));
    }

    @Test
    void findById_whenAlertMissing_shouldThrowResourceNotFoundException() {
        when(alertRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> alertService.findById(404L));
    }

    @Test
    void findByIncident_shouldQueryWithIncidentReferenceOnly() {
        Alert alert = Alert.builder().id(1L).incident(incident).build();
        when(alertRepository.findByIncident(any(Incident.class))).thenReturn(List.of(alert));

        List<Alert> result = alertService.findByIncident(10L);

        assertEquals(List.of(alert), result);
        ArgumentCaptor<Incident> incidentCaptor = ArgumentCaptor.forClass(Incident.class);
        verify(alertRepository).findByIncident(incidentCaptor.capture());
        assertEquals(10L, incidentCaptor.getValue().getId());
    }
}
