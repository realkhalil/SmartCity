package ma.urbanops.service;

import ma.urbanops.dto.request.IncidentRequest;
import ma.urbanops.dto.response.AIAnalysisResult;
import ma.urbanops.dto.response.ContentModerationResult;
import ma.urbanops.entity.Category;
import ma.urbanops.entity.Incident;
import ma.urbanops.entity.Sector;
import ma.urbanops.entity.User;
import ma.urbanops.enums.IncidentStatus;
import ma.urbanops.enums.Severity;
import ma.urbanops.exception.ResourceNotFoundException;
import ma.urbanops.repository.CategoryRepository;
import ma.urbanops.repository.IncidentRepository;
import ma.urbanops.repository.SectorRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock private IncidentRepository incidentRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private SectorRepository sectorRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private AIAnalysisService aiAnalysisService;
    @Mock private AlertService alertService;
    @Mock private IncidentContentModerationLogService moderationLogService;

    @InjectMocks private IncidentService incidentService;

    private Incident testIncident;
    private User testUser;

    @BeforeAll
    static void initAll() {
        System.out.println("=== Starting IncidentService Tests ===");
    }

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@marrakech.ma");
        testUser.setFirstName("Yassine");

        testIncident = new Incident();
        testIncident.setId(1L);
        testIncident.setReferenceCode("INC-1001");
        testIncident.setTitle("Fuite d'eau Bab Doukkala");
        testIncident.setSeverity(Severity.HIGH);
        testIncident.setStatus(IncidentStatus.OPEN);
        testIncident.setLatitude(31.6330);
        testIncident.setLongitude(-7.9990);
    }

    @AfterEach
    void tearDown() {
        // cleanup if needed
    }

    @AfterAll
    static void cleanAll() {
        System.out.println("=== IncidentService Tests Complete ===");
    }

    @Test
    void getById_whenIncidentExists_shouldReturnIncident() {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(testIncident));
        Incident result = incidentService.getById(1L);
        assertNotNull(result);
        assertEquals("INC-1001", result.getReferenceCode());
        assertEquals(Severity.HIGH, result.getSeverity());
    }

    @Test
    void getById_whenIncidentNotFound_shouldThrowResourceNotFoundException() {
        when(incidentRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> {
            incidentService.getById(999L);
        });
    }

    @Test
    void getById_whenIdIsNull_shouldThrowException() {
        assertThrows(Exception.class, () -> {
            incidentService.getById(null);
        });
    }

    @Test
    void updateStatus_whenValidTransition_shouldUpdateAndSave() {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(testIncident));
        when(incidentRepository.save(any(Incident.class))).thenReturn(testIncident);

        Incident result = incidentService.updateStatus(1L, ma.urbanops.dto.request.UpdateStatusRequest.builder()
                .status(IncidentStatus.IN_PROGRESS).build());

        assertNotNull(result);
        assertEquals(IncidentStatus.IN_PROGRESS, result.getStatus());
        verify(incidentRepository, times(1)).save(any(Incident.class));
    }

    @Test
    void updateStatus_whenResolvedStatus_shouldSetResolvedAt() {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(testIncident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));

        Incident result = incidentService.updateStatus(1L, ma.urbanops.dto.request.UpdateStatusRequest.builder()
                .status(IncidentStatus.RESOLVED).build());

        assertNotNull(result.getResolvedAt());
        assertEquals(IncidentStatus.RESOLVED, result.getStatus());
    }

    @Test
    void getRecentIncidents_shouldReturnLimitedList() {
        java.util.List<Incident> mockList = java.util.List.of(testIncident, testIncident, testIncident);
        when(incidentRepository.findTopNByOrderByCreatedAtDesc(any())).thenReturn(mockList);

        java.util.List<Incident> result = incidentService.getRecentIncidents(10);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.size() <= 10);
    }

    @Test
    void generateReferenceCode_shouldMatchExpectedFormat() {
        String code = incidentService.generateReferenceCode(1001L);
        assertNotNull(code);
        assertTrue(code.startsWith("INC-"));
        assertEquals("INC-1001", code);
    }

    @Test
    void getByReference_whenExists_shouldReturnIncident() {
        when(incidentRepository.findByReferenceCode("INC-1001")).thenReturn(Optional.of(testIncident));
        Incident result = incidentService.getByReference("INC-1001");
        assertNotNull(result);
        assertEquals("INC-1001", result.getReferenceCode());
    }

    @Test
    void deleteIncident_whenCalled_shouldDeleteFromRepository() {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(testIncident));
        doNothing().when(incidentRepository).delete(any(Incident.class));
        
        incidentService.deleteIncident(1L);
        
        verify(incidentRepository, times(1)).delete(testIncident);
    }

    @Test
    void createIncident_whenModerationRejects_shouldNotPersistIncidentOrPhoto() {
        Category category = Category.builder().id(1L).name("Voirie").build();
        Sector sector = Sector.builder().id(1L).name("Gueliz").build();
        IncidentRequest request = IncidentRequest.builder()
                .title("azerty qwerty")
                .description("asdf asdf asdf random test")
                .categoryId(category.getId())
                .sectorId(sector.getId())
                .latitude(31.63)
                .longitude(-8.0)
                .build();

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(sectorRepository.findById(sector.getId())).thenReturn(Optional.of(sector));
        when(aiAnalysisService.moderateIncidentContent(
                request.getTitle(), request.getDescription(), category.getName(), sector.getName()))
                .thenReturn(ContentModerationResult.builder()
                        .accepted(false)
                        .reason("Contenu aleatoire")
                        .confidence(0.9)
                        .fallbackUsed(false)
                        .build());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> incidentService.createIncident(request, null, testUser));

        assertTrue(ex.getMessage().contains("Signalement refuse"));
        verify(moderationLogService).log(null, request.getTitle(), request.getDescription(),
                category, sector, testUser, ContentModerationResult.builder()
                        .accepted(false)
                        .reason("Contenu aleatoire")
                        .confidence(0.9)
                        .fallbackUsed(false)
                        .build());
        verify(incidentRepository, never()).save(any(Incident.class));
        verify(fileStorageService, never()).storeFile(any());
    }

    @Test
    void createIncident_whenAcceptedHighSeverity_shouldStorePhotoAnalyzeAlertAndLog() {
        Category category = Category.builder().id(1L).name("Electricite").defaultAuthority("ONEE").authorityEmail("onee@test.ma").build();
        Sector sector = Sector.builder().id(2L).name("Gueliz").build();
        IncidentRequest request = IncidentRequest.builder()
                .title("Cable expose")
                .description("Cable electrique expose sur la route")
                .categoryId(1L)
                .sectorId(2L)
                .latitude(31.63)
                .longitude(-8.0)
                .build();
        org.springframework.mock.web.MockMultipartFile photo =
                new org.springframework.mock.web.MockMultipartFile("photo", "cable.jpg", "image/jpeg", "x".getBytes());
        ContentModerationResult moderation = ContentModerationResult.builder()
                .accepted(true)
                .reason("OK")
                .confidence(0.8)
                .fallbackUsed(false)
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(sectorRepository.findById(2L)).thenReturn(Optional.of(sector));
        when(aiAnalysisService.moderateIncidentContent(request.getTitle(), request.getDescription(), "Electricite", "Gueliz"))
                .thenReturn(moderation);
        when(fileStorageService.storeFile(photo)).thenReturn("stored.jpg");
        when(aiAnalysisService.analyze(request.getDescription(), "Electricite")).thenReturn(AIAnalysisResult.builder()
                .severity("HIGH")
                .category("Electricite")
                .authorityName("ONEE")
                .reason("Danger immediat")
                .confidence(0.9)
                .fallbackUsed(false)
                .build());
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> {
            Incident saved = inv.getArgument(0);
            if (saved.getId() == null) saved.setId(77L);
            return saved;
        });

        Incident result = incidentService.createIncident(request, photo, testUser);

        assertEquals(Severity.HIGH, result.getSeverity());
        assertEquals("ONEE", result.getAuthorityNotified());
        assertEquals("INC-0077", result.getReferenceCode());
        assertEquals("stored.jpg", result.getPhotoUrl());
        assertTrue(result.getAlertSent());
        verify(alertService).createAndSendAlert(result);
        verify(moderationLogService).log(eq(77L), eq(request.getTitle()), eq(request.getDescription()),
                eq(category), eq(sector), eq(testUser), eq(moderation));
    }

    @Test
    void createIncident_whenAiFails_shouldUseDefaultsAndStillAlert() {
        Category category = Category.builder().id(1L).name("Voirie").defaultAuthority("Commune").build();
        Sector sector = Sector.builder().id(2L).name("Medina").build();
        IncidentRequest request = IncidentRequest.builder()
                .title("Route abimee")
                .description("Route abimee avec trou profond")
                .categoryId(1L)
                .sectorId(2L)
                .latitude(31.63)
                .longitude(-8.0)
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(sectorRepository.findById(2L)).thenReturn(Optional.of(sector));
        when(aiAnalysisService.moderateIncidentContent(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(ContentModerationResult.builder().accepted(true).build());
        when(aiAnalysisService.analyze(anyString(), anyString())).thenThrow(new RuntimeException("ai down"));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> {
            Incident saved = inv.getArgument(0);
            if (saved.getId() == null) saved.setId(88L);
            return saved;
        });

        Incident result = incidentService.createIncident(request, null, null);

        assertEquals(Severity.MEDIUM, result.getSeverity());
        assertEquals("Commune", result.getAuthorityNotified());
        assertEquals("Analyse indisponible", result.getAiAnalysisResult());
        assertTrue(result.getAlertSent());
    }

    @Test
    void createIncident_whenSeverityIsLow_shouldNotCreateAlert() {
        Category category = Category.builder().id(1L).name("Dechets").build();
        Sector sector = Sector.builder().id(2L).name("Palmeraie").build();
        IncidentRequest request = IncidentRequest.builder()
                .title("Poubelle pleine")
                .description("Poubelle pleine depuis hier")
                .categoryId(1L)
                .sectorId(2L)
                .latitude(31.63)
                .longitude(-8.0)
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(sectorRepository.findById(2L)).thenReturn(Optional.of(sector));
        when(aiAnalysisService.moderateIncidentContent(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(ContentModerationResult.builder().accepted(true).build());
        when(aiAnalysisService.analyze(anyString(), anyString())).thenReturn(AIAnalysisResult.builder()
                .severity("LOW")
                .category("Dechets")
                .authorityName("Commune")
                .reason("Gene mineure")
                .build());
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> {
            Incident saved = inv.getArgument(0);
            if (saved.getId() == null) saved.setId(99L);
            return saved;
        });

        Incident result = incidentService.createIncident(request, null, null);

        assertEquals(Severity.LOW, result.getSeverity());
        assertFalse(result.getAlertSent());
        verify(alertService, never()).createAndSendAlert(any());
    }

    @Test
    void createIncident_whenAlertFails_shouldStillReturnSavedIncident() {
        Category category = Category.builder().id(1L).name("Voirie").build();
        Sector sector = Sector.builder().id(2L).name("Gueliz").build();
        IncidentRequest request = IncidentRequest.builder()
                .title("Route bloquee")
                .description("Route bloquee par un obstacle")
                .categoryId(1L)
                .sectorId(2L)
                .latitude(31.63)
                .longitude(-8.0)
                .build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(sectorRepository.findById(2L)).thenReturn(Optional.of(sector));
        when(aiAnalysisService.moderateIncidentContent(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(ContentModerationResult.builder().accepted(true).build());
        when(aiAnalysisService.analyze(anyString(), anyString())).thenReturn(AIAnalysisResult.builder()
                .severity("MEDIUM")
                .authorityName("Commune")
                .reason("Obstacle")
                .build());
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> {
            Incident saved = inv.getArgument(0);
            if (saved.getId() == null) saved.setId(100L);
            return saved;
        });
        doThrow(new RuntimeException("queue down")).when(alertService).createAndSendAlert(any());

        Incident result = incidentService.createIncident(request, null, null);

        assertEquals("INC-0100", result.getReferenceCode());
        assertFalse(result.getAlertSent());
    }

    @Test
    @Disabled("Désactivé: dépend de l'API AI externe — à tester en intégration")
    void createIncident_withAIAnalysis_shouldSetSeverityFromAI() {
        fail("Test désactivé intentionnellement - requires real AI API");
    }
}
