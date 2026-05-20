package ma.urbanops.jms;

import ma.urbanops.dto.jms.AlertMessage;
import ma.urbanops.entity.Category;
import ma.urbanops.entity.Incident;
import ma.urbanops.entity.Sector;
import ma.urbanops.entity.User;
import ma.urbanops.enums.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertProducerTest {

    @Mock private JmsTemplate jmsTemplate;

    private AlertProducer alertProducer;

    @BeforeEach
    void setUp() {
        alertProducer = new AlertProducer(jmsTemplate);
        ReflectionTestUtils.setField(alertProducer, "alertQueue", "alerts.queue");
    }

    @Test
    void sendAlertToQueue_shouldBuildMessageFromIncident() {
        Category category = Category.builder()
                .name("Voirie")
                .authorityEmail("voirie@marrakech.ma")
                .build();
        Sector sector = Sector.builder().name("Gueliz").build();
        User reporter = User.builder().email("citizen@test.ma").build();
        Incident incident = Incident.builder()
                .id(11L)
                .referenceCode("INC-0011")
                .title("Route abimee")
                .description("Trou profond")
                .severity(Severity.HIGH)
                .category(category)
                .sector(sector)
                .authorityNotified("Service voirie")
                .latitude(31.63)
                .longitude(-8.0)
                .reportedBy(reporter)
                .createdAt(LocalDateTime.of(2026, 5, 20, 12, 30))
                .build();

        alertProducer.sendAlertToQueue(incident);

        ArgumentCaptor<AlertMessage> messageCaptor = ArgumentCaptor.forClass(AlertMessage.class);
        verify(jmsTemplate).convertAndSend(eq("alerts.queue"), messageCaptor.capture());
        AlertMessage message = messageCaptor.getValue();
        assertEquals(11L, message.getIncidentId());
        assertEquals("INC-0011", message.getReferenceCode());
        assertEquals("Route abimee", message.getTitle());
        assertEquals("Trou profond", message.getDescription());
        assertEquals("HIGH", message.getSeverity());
        assertEquals("Voirie", message.getCategory());
        assertEquals("Gueliz", message.getSector());
        assertEquals("Service voirie", message.getAuthorityName());
        assertEquals("voirie@marrakech.ma", message.getAuthorityEmail());
        assertEquals("citizen@test.ma", message.getReporterEmail());
        assertEquals("2026-05-20T12:30", message.getCreatedAt());
    }

    @Test
    void sendAlertToQueue_whenOptionalFieldsAreMissing_shouldUseDefaults() {
        Incident incident = Incident.builder()
                .id(12L)
                .referenceCode("INC-0012")
                .title("Signalement incomplet")
                .description("Details a confirmer")
                .latitude(31.63)
                .longitude(-8.0)
                .build();

        alertProducer.sendAlertToQueue(incident);

        ArgumentCaptor<AlertMessage> messageCaptor = ArgumentCaptor.forClass(AlertMessage.class);
        verify(jmsTemplate).convertAndSend(eq("alerts.queue"), messageCaptor.capture());
        AlertMessage message = messageCaptor.getValue();
        assertEquals("MEDIUM", message.getSeverity());
        assertEquals("", message.getCategory());
        assertEquals("", message.getSector());
        assertEquals("", message.getAuthorityEmail());
        assertEquals("anonyme", message.getReporterEmail());
        assertEquals("", message.getCreatedAt());
    }

    @Test
    void sendAlertToQueue_whenJmsFails_shouldSwallowException() {
        Incident incident = Incident.builder()
                .id(13L)
                .referenceCode("INC-0013")
                .title("Panne")
                .description("Alerte")
                .severity(Severity.MEDIUM)
                .category(Category.builder().name("Eclairage").authorityEmail("eclairage@test.ma").build())
                .sector(Sector.builder().name("Medina").build())
                .build();
        doThrow(new RuntimeException("broker down"))
                .when(jmsTemplate).convertAndSend(eq("alerts.queue"), any(AlertMessage.class));

        assertDoesNotThrow(() -> alertProducer.sendAlertToQueue(incident));
    }
}
