package ma.urbanops.controller;

import ma.urbanops.dto.response.AlertResponse;
import ma.urbanops.entity.Alert;
import ma.urbanops.entity.Incident;
import ma.urbanops.enums.Severity;
import ma.urbanops.service.AlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertControllerTest {

    @Mock private AlertService alertService;

    @InjectMocks private AlertController alertController;

    private Alert alert;

    @BeforeEach
    void setUp() {
        Incident incident = Incident.builder()
                .id(42L)
                .referenceCode("INC-0042")
                .build();
        alert = Alert.builder()
                .id(7L)
                .incident(incident)
                .severity(Severity.HIGH)
                .title("Incident critique")
                .message("A traiter rapidement")
                .sentTo("autorite@marrakech.ma")
                .emailSent(true)
                .acknowledged(false)
                .sentAt(LocalDateTime.of(2026, 5, 20, 10, 0))
                .build();
    }

    @Test
    void getAllAlerts_shouldReturnMappedAlertPage() {
        Pageable pageable = Pageable.ofSize(5);
        when(alertService.getAllAlerts(pageable)).thenReturn(new PageImpl<>(List.of(alert)));

        ResponseEntity<Page<AlertResponse>> response = alertController.getAllAlerts(null, null, pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        AlertResponse body = response.getBody().getContent().get(0);
        assertEquals(7L, body.getId());
        assertEquals(42L, body.getIncidentId());
        assertEquals("INC-0042", body.getIncidentReference());
        assertEquals(Severity.HIGH, body.getSeverity());
        assertEquals("autorite@marrakech.ma", body.getSentTo());
    }

    @Test
    void getAllAlerts_withFiltersPresent_shouldStillReturnMappedAlertPage() {
        Pageable pageable = Pageable.ofSize(5);
        when(alertService.getAllAlerts(pageable)).thenReturn(new PageImpl<>(List.of(alert)));

        ResponseEntity<Page<AlertResponse>> response = alertController.getAllAlerts(Severity.HIGH, false, pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getNumberOfElements());
        verify(alertService).getAllAlerts(pageable);
    }

    @Test
    void getAlertById_shouldReturnMappedAlert() {
        when(alertService.findById(7L)).thenReturn(alert);

        ResponseEntity<AlertResponse> response = alertController.getAlertById(7L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Incident critique", response.getBody().getTitle());
        assertFalse(response.getBody().getAcknowledged());
    }

    @Test
    void getAlertsByIncident_shouldReturnMappedList() {
        when(alertService.findByIncident(42L)).thenReturn(List.of(alert));

        ResponseEntity<List<AlertResponse>> response = alertController.getAlertsByIncident(42L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("INC-0042", response.getBody().get(0).getIncidentReference());
    }

    @Test
    void resendAlert_shouldReturnAccepted() {
        ResponseEntity<Void> response = alertController.resendAlert(7L);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        verify(alertService).resendAlert(7L);
    }

    @Test
    void acknowledgeAlert_shouldReturnOk() {
        ResponseEntity<Void> response = alertController.acknowledgeAlert(7L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(alertService).acknowledgeAlert(7L);
    }

    @Test
    void getRecentAlerts_shouldRequestTenAlerts() {
        when(alertService.getRecentAlerts(10)).thenReturn(List.of(alert));

        ResponseEntity<List<AlertResponse>> response = alertController.getRecentAlerts();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(alertService).getRecentAlerts(10);
    }

    @Test
    void getCriticalAlerts_shouldReturnMappedCriticalAlerts() {
        when(alertService.getCriticalUnacknowledgedAlerts()).thenReturn(List.of(alert));

        ResponseEntity<List<AlertResponse>> response = alertController.getCriticalAlerts();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Severity.HIGH, response.getBody().get(0).getSeverity());
    }
}
