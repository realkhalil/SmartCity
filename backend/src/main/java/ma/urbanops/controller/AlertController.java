package ma.urbanops.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.urbanops.dto.response.AlertResponse;
import ma.urbanops.entity.Alert;
import ma.urbanops.enums.Severity;
import ma.urbanops.service.AlertService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Alert management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all alerts")
    public ResponseEntity<Page<AlertResponse>> getAllAlerts(
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) Boolean acknowledged,
            Pageable pageable) {
        Page<Alert> alerts = alertService.getAllAlerts(pageable);
        return ResponseEntity.ok(alerts.map(this::toResponse));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get alert by ID")
    public ResponseEntity<AlertResponse> getAlertById(@PathVariable Long id) {
        Alert alert = alertService.findById(id);
        return ResponseEntity.ok(toResponse(alert));
    }

    @GetMapping("/incident/{incidentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get alerts for a specific incident")
    public ResponseEntity<List<AlertResponse>> getAlertsByIncident(@PathVariable Long incidentId) {
        List<Alert> alerts = alertService.findByIncident(incidentId);
        return ResponseEntity.ok(alerts.stream().map(this::toResponse).toList());
    }

    @PostMapping("/{incidentId}/resend")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resend alert email for an incident")
    public ResponseEntity<Void> resendAlert(@PathVariable Long incidentId) {
        alertService.resendAlert(incidentId);
        return ResponseEntity.accepted().build();  // HTTP 202 Accepted (async processing)
    }

    @PatchMapping("/{id}/acknowledge")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mark alert as acknowledged")
    public ResponseEntity<Void> acknowledgeAlert(@PathVariable Long id) {
        alertService.acknowledgeAlert(id);
        return ResponseEntity.ok().build();  // HTTP 200 OK for PATCH
    }

    @GetMapping("/recent")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get 10 most recent alerts")
    public ResponseEntity<List<AlertResponse>> getRecentAlerts() {
        List<Alert> alerts = alertService.getRecentAlerts(10);
        return ResponseEntity.ok(alerts.stream().map(this::toResponse).toList());
    }

    @GetMapping("/critical")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get critical unacknowledged alerts")
    public ResponseEntity<List<AlertResponse>> getCriticalAlerts() {
        List<Alert> alerts = alertService.getCriticalUnacknowledgedAlerts();
        return ResponseEntity.ok(alerts.stream().map(this::toResponse).toList());
    }

    private AlertResponse toResponse(Alert a) {
        return AlertResponse.builder()
                .id(a.getId())
                .incidentId(a.getIncident().getId())
                .incidentReference(a.getIncident().getReferenceCode())
                .severity(a.getSeverity())
                .title(a.getTitle())
                .message(a.getMessage())
                .sentTo(a.getSentTo())
                .emailSent(a.getEmailSent())
                .sentAt(a.getSentAt())
                .acknowledged(a.getAcknowledged())
                .acknowledgedAt(a.getAcknowledgedAt())
                .build();
    }
}
