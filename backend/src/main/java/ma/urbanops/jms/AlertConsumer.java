package ma.urbanops.jms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.urbanops.dto.jms.AlertMessage;
import ma.urbanops.service.EmailService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AlertConsumer {

    private final EmailService emailService;

    /**
     * Flow: IncidentService → AlertService → AlertProducer → [JMS Queue] → AlertConsumer → EmailService
     */
    @JmsListener(destination = "${jms.queue.alert}",
            containerFactory = "jmsListenerContainerFactory")
    public void receiveAlert(AlertMessage message) {
        log.info("[JMS CONSUMER] Received alert from queue for incident {}",
                message.getReferenceCode());

        try {
            emailService.sendAlertEmailFromJms(
                    message.getAuthorityName(),
                    message.getAuthorityEmail(),
                    message.getReferenceCode(),
                    message.getSeverity(),
                    message.getSector(),
                    message.getCategory(),
                    message.getCreatedAt(),
                    message.getDescription(),
                    message.getLatitude(),
                    message.getLongitude()
            );

            log.info("[JMS CONSUMER] Email sent successfully to {} for incident {}",
                    message.getAuthorityEmail(), message.getReferenceCode());

        } catch (Exception e) {
            log.error("[JMS CONSUMER] Failed to send email for {}: {}",
                    message.getReferenceCode(), e.getMessage());
        }
    }
}
