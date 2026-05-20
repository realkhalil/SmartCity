package ma.urbanops.jms;

import ma.urbanops.dto.jms.AlertMessage;
import ma.urbanops.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertConsumerTest {

    @Mock private EmailService emailService;

    private AlertConsumer alertConsumer;
    private AlertMessage message;

    @BeforeEach
    void setUp() {
        alertConsumer = new AlertConsumer(emailService);
        message = AlertMessage.builder()
                .referenceCode("INC-0020")
                .title("Fuite d'eau")
                .description("Rue principale")
                .severity("HIGH")
                .category("Eau")
                .sector("Medina")
                .authorityName("Service eau")
                .authorityEmail("eau@marrakech.ma")
                .latitude(31.63)
                .longitude(-8.0)
                .createdAt("2026-05-20T12:30")
                .build();
    }

    @Test
    void receiveAlert_shouldSendEmailFromJmsMessage() {
        alertConsumer.receiveAlert(message);

        verify(emailService).sendAlertEmailFromJms(
                "Service eau",
                "eau@marrakech.ma",
                "INC-0020",
                "HIGH",
                "Medina",
                "Eau",
                "2026-05-20T12:30",
                "Rue principale",
                31.63,
                -8.0
        );
    }

    @Test
    void receiveAlert_whenEmailServiceFails_shouldSwallowException() {
        doThrow(new RuntimeException("smtp unavailable")).when(emailService).sendAlertEmailFromJms(
                anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyDouble(), anyDouble());

        assertDoesNotThrow(() -> alertConsumer.receiveAlert(message));
    }
}
