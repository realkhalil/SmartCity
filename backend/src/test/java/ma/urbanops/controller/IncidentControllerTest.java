package ma.urbanops.controller;

import ma.urbanops.dto.request.IncidentRequest;
import ma.urbanops.dto.request.UpdateStatusRequest;
import ma.urbanops.dto.response.IncidentResponse;
import ma.urbanops.entity.Category;
import ma.urbanops.entity.Incident;
import ma.urbanops.entity.Sector;
import ma.urbanops.entity.User;
import ma.urbanops.enums.IncidentStatus;
import ma.urbanops.enums.Role;
import ma.urbanops.enums.Severity;
import ma.urbanops.security.UserDetailsImpl;
import ma.urbanops.service.IncidentService;
import ma.urbanops.service.UserService;
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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentControllerTest {

    @Mock private IncidentService incidentService;
    @Mock private UserService userService;

    @InjectMocks private IncidentController incidentController;

    private Incident incident;
    private User reporter;

    @BeforeEach
    void setUp() {
        Category category = Category.builder()
                .id(1L)
                .name("Voirie")
                .icon("road")
                .defaultAuthority("Travaux Publics")
                .authorityEmail("voirie@marrakech.ma")
                .build();
        Sector sector = Sector.builder()
                .id(2L)
                .name("Gueliz")
                .city("Marrakech")
                .centerLat(31.63)
                .centerLng(-8.0)
                .build();
        reporter = User.builder()
                .id(3L)
                .firstName("Yassine")
                .lastName("Benali")
                .email("yassine@test.ma")
                .phone("0600000000")
                .role(Role.CITIZEN)
                .sector("Gueliz")
                .receiveAlerts(true)
                .isActive(true)
                .password("secret")
                .build();
        incident = Incident.builder()
                .id(4L)
                .referenceCode("INC-0004")
                .title("Trou dangereux")
                .description("Trou profond sur la route")
                .category(category)
                .sector(sector)
                .severity(Severity.HIGH)
                .status(IncidentStatus.OPEN)
                .reportedBy(reporter)
                .photoUrl("/uploads/photo.jpg")
                .latitude(31.63)
                .longitude(-8.0)
                .authorityNotified("Travaux Publics")
                .aiAnalysisResult("Danger pour les voitures")
                .alertSent(true)
                .build();
    }

    @Test
    void getAllIncidents_shouldReturnMappedPage() {
        Pageable pageable = Pageable.ofSize(10);
        when(incidentService.getAllIncidents(1L, 2L, Severity.HIGH, IncidentStatus.OPEN, "trou", pageable))
                .thenReturn(new PageImpl<>(List.of(incident)));

        ResponseEntity<Page<IncidentResponse>> response = incidentController.getAllIncidents(
                1L, 2L, Severity.HIGH, IncidentStatus.OPEN, "trou", pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        IncidentResponse body = response.getBody().getContent().get(0);
        assertEquals("INC-0004", body.getReferenceCode());
        assertEquals("Voirie", body.getCategory().getName());
        assertEquals("Gueliz", body.getSector().getName());
        assertEquals("yassine@test.ma", body.getReportedBy().getEmail());
    }

    @Test
    void getIncidentById_shouldReturnMappedIncident() {
        when(incidentService.getById(4L)).thenReturn(incident);

        ResponseEntity<IncidentResponse> response = incidentController.getIncidentById(4L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("HIGH", response.getBody().getSeverity());
    }

    @Test
    void getIncidentByReference_shouldReturnMappedIncident() {
        when(incidentService.getByReference("INC-4")).thenReturn(incident);

        ResponseEntity<IncidentResponse> response = incidentController.getIncidentByReference("INC-4");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("INC-0004", response.getBody().getReferenceCode());
    }

    @Test
    void createIncident_withAuthenticatedUser_shouldCreateWithLocationHeader() {
        IncidentRequest request = IncidentRequest.builder()
                .title("Trou dangereux")
                .description("Trou profond sur la route")
                .categoryId(1L)
                .sectorId(2L)
                .latitude(31.63)
                .longitude(-8.0)
                .build();
        UserDetailsImpl userDetails = UserDetailsImpl.build(reporter);
        when(userService.findByEmail("yassine@test.ma")).thenReturn(reporter);
        when(incidentService.createIncident(request, null, reporter)).thenReturn(incident);

        ResponseEntity<IncidentResponse> response = incidentController.createIncident(request, null, userDetails);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("/api/v1/incidents/4", response.getHeaders().getLocation().toString());
        assertEquals("INC-0004", response.getBody().getReferenceCode());
    }

    @Test
    void createIncident_withoutAuthenticatedUser_shouldCreateAnonymousIncident() {
        IncidentRequest request = IncidentRequest.builder()
                .title("Trou dangereux")
                .description("Trou profond sur la route")
                .categoryId(1L)
                .sectorId(2L)
                .latitude(31.63)
                .longitude(-8.0)
                .build();
        when(incidentService.createIncident(request, null, null)).thenReturn(incident);

        ResponseEntity<IncidentResponse> response = incidentController.createIncident(request, null, null);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(userService, never()).findByEmail(anyString());
    }

    @Test
    void updateStatus_shouldReturnUpdatedIncident() {
        UpdateStatusRequest request = UpdateStatusRequest.builder().status(IncidentStatus.RESOLVED).build();
        incident.setStatus(IncidentStatus.RESOLVED);
        when(incidentService.updateStatus(4L, request)).thenReturn(incident);

        ResponseEntity<IncidentResponse> response = incidentController.updateStatus(4L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("RESOLVED", response.getBody().getStatus());
    }

    @Test
    void deleteIncident_shouldReturnNoContent() {
        ResponseEntity<Void> response = incidentController.deleteIncident(4L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(incidentService).deleteIncident(4L);
    }

    @Test
    void getMyIncidents_shouldResolveCurrentUserAndMapResults() {
        UserDetailsImpl userDetails = UserDetailsImpl.build(reporter);
        when(userService.findByEmail("yassine@test.ma")).thenReturn(reporter);
        when(incidentService.getMyIncidents(reporter)).thenReturn(List.of(incident));

        ResponseEntity<List<IncidentResponse>> response = incidentController.getMyIncidents(userDetails);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getForMap_shouldReturnMapRowsWithDefaultsForMissingValues() {
        Incident partial = Incident.builder()
                .id(5L)
                .title("Signalement partiel")
                .latitude(31.7)
                .longitude(-8.1)
                .build();
        when(incidentService.getAllForMap()).thenReturn(List.of(incident, partial));

        ResponseEntity<List<Map<String, Object>>> response = incidentController.getForMap();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("HIGH", response.getBody().get(0).get("severity"));
        assertEquals("MEDIUM", response.getBody().get(1).get("severity"));
        assertEquals("OPEN", response.getBody().get(1).get("status"));
        assertEquals("Autre", ((Map<?, ?>) response.getBody().get(1).get("category")).get("name"));
    }

    @Test
    void listEndpoints_shouldMapServiceResults() {
        when(incidentService.getRecentIncidents(10)).thenReturn(List.of(incident));
        when(incidentService.getBySector(2L)).thenReturn(List.of(incident));
        when(incidentService.getByCategory(1L)).thenReturn(List.of(incident));

        assertEquals(1, incidentController.getRecentIncidents().getBody().size());
        assertEquals(1, incidentController.getBySector(2L).getBody().size());
        assertEquals(1, incidentController.getByCategory(1L).getBody().size());
    }
}
