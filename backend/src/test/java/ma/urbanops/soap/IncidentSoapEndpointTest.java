package ma.urbanops.soap;

import ma.urbanops.dto.response.StatsResponse;
import ma.urbanops.entity.Category;
import ma.urbanops.entity.Incident;
import ma.urbanops.entity.Sector;
import ma.urbanops.enums.IncidentStatus;
import ma.urbanops.enums.Severity;
import ma.urbanops.repository.IncidentRepository;
import ma.urbanops.service.StatsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentSoapEndpointTest {

    @Mock private IncidentRepository incidentRepository;
    @Mock private StatsService statsService;

    @InjectMocks private IncidentSoapEndpoint endpoint;

    @Test
    void getIncidentStats_shouldMapDashboardStats() {
        when(statsService.getDashboardStats()).thenReturn(StatsResponse.builder()
                .totalIncidents(20)
                .openIncidents(5)
                .resolvedIncidents(10)
                .highSeverityCount(3)
                .resolutionRate(50.0)
                .build());
        IncidentSoapEndpoint.GetIncidentStatsRequest request = new IncidentSoapEndpoint.GetIncidentStatsRequest();
        request.setSector("Gueliz");
        request.setCategory("Voirie");

        IncidentSoapEndpoint.GetIncidentStatsResponse response = endpoint.getIncidentStats(request);

        assertEquals("Gueliz", request.getSector());
        assertEquals("Voirie", request.getCategory());
        assertEquals(20, response.getTotalIncidents());
        assertEquals(5, response.getOpenIncidents());
        assertEquals(10, response.getResolvedIncidents());
        assertEquals(3, response.getHighSeverityCount());
        assertEquals(50.0, response.getResolutionRate());
        assertNotNull(response.getGeneratedAt());
    }

    @Test
    void getIncidentByReference_whenFound_shouldMapIncident() {
        Incident incident = Incident.builder()
                .id(7L)
                .referenceCode("INC-0007")
                .title("Cable expose")
                .severity(Severity.HIGH)
                .status(IncidentStatus.OPEN)
                .category(Category.builder().name("Electricite").build())
                .sector(Sector.builder().name("Gueliz").build())
                .authorityNotified("ONEE")
                .createdAt(LocalDateTime.of(2026, 5, 20, 8, 0))
                .build();
        when(incidentRepository.findByReferenceCode("INC-0007")).thenReturn(Optional.of(incident));
        IncidentSoapEndpoint.GetIncidentByReferenceRequest request = new IncidentSoapEndpoint.GetIncidentByReferenceRequest();
        request.setReferenceCode("INC-0007");

        IncidentSoapEndpoint.GetIncidentByReferenceResponse response = endpoint.getIncidentByReference(request);

        assertTrue(response.isFound());
        assertEquals(7L, response.getId());
        assertEquals("INC-0007", response.getReferenceCode());
        assertEquals("Cable expose", response.getTitle());
        assertEquals("HIGH", response.getSeverity());
        assertEquals("OPEN", response.getStatus());
        assertEquals("Electricite", response.getCategory());
        assertEquals("Gueliz", response.getSector());
        assertEquals("ONEE", response.getAuthorityNotified());
        assertEquals("2026-05-20T08:00", response.getCreatedAt());
    }

    @Test
    void getIncidentByReference_whenMissing_shouldReturnNotFoundResponse() {
        when(incidentRepository.findByReferenceCode("INC-9999")).thenReturn(Optional.empty());
        IncidentSoapEndpoint.GetIncidentByReferenceRequest request = new IncidentSoapEndpoint.GetIncidentByReferenceRequest();
        request.setReferenceCode("INC-9999");

        IncidentSoapEndpoint.GetIncidentByReferenceResponse response = endpoint.getIncidentByReference(request);

        assertFalse(response.isFound());
        assertEquals("INC-9999", response.getReferenceCode());
        assertEquals("Incident not found", response.getTitle());
    }

    @Test
    void getIncidentByReference_whenOptionalFieldsAreNull_shouldUseEmptyStrings() {
        Incident incident = new Incident();
        incident.setId(8L);
        incident.setReferenceCode("INC-0008");
        incident.setTitle("Partial");
        incident.setSeverity(null);
        incident.setStatus(null);
        when(incidentRepository.findByReferenceCode("INC-0008")).thenReturn(Optional.of(incident));
        IncidentSoapEndpoint.GetIncidentByReferenceRequest request = new IncidentSoapEndpoint.GetIncidentByReferenceRequest();
        request.setReferenceCode("INC-0008");

        IncidentSoapEndpoint.GetIncidentByReferenceResponse response = endpoint.getIncidentByReference(request);

        assertTrue(response.isFound());
        assertEquals("", response.getSeverity());
        assertEquals("", response.getStatus());
        assertEquals("", response.getCategory());
        assertEquals("", response.getSector());
        assertEquals("", response.getAuthorityNotified());
        assertEquals("", response.getCreatedAt());
    }
}
