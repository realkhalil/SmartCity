package ma.urbanops.service;

import ma.urbanops.dto.response.SectorResponse;
import ma.urbanops.entity.Incident;
import ma.urbanops.entity.Sector;
import ma.urbanops.exception.ResourceNotFoundException;
import ma.urbanops.repository.IncidentRepository;
import ma.urbanops.repository.SectorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SectorServiceTest {

    @Mock private SectorRepository sectorRepository;
    @Mock private IncidentRepository incidentRepository;

    @InjectMocks private SectorService sectorService;

    private final Sector sector = Sector.builder()
            .id(1L)
            .name("Gueliz")
            .city("Marrakech")
            .centerLat(31.63)
            .centerLng(-8.0)
            .build();

    @Test
    void findAndCreateMethods_shouldDelegateToRepositories() {
        when(sectorRepository.findAll()).thenReturn(List.of(sector));
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector));
        when(sectorRepository.findByCity("Marrakech")).thenReturn(List.of(sector));
        when(sectorRepository.save(sector)).thenReturn(sector);

        assertEquals(List.of(sector), sectorService.findAll());
        assertEquals(List.of(sector), sectorService.findAllActive());
        assertSame(sector, sectorService.findById(1L));
        assertEquals(List.of(sector), sectorService.findByCity("Marrakech"));
        assertSame(sector, sectorService.create(sector));
    }

    @Test
    void findById_whenMissing_shouldThrow() {
        when(sectorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> sectorService.findById(99L));
    }

    @Test
    void getIncidentsBySector_shouldBuildSpecificationForSector() {
        Incident incident = Incident.builder().id(9L).sector(sector).build();
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector));
        when(incidentRepository.findAll(any(Specification.class))).thenReturn(List.of(incident));

        List<Incident> result = sectorService.getIncidentsBySector(1L);

        assertEquals(List.of(incident), result);
        verify(incidentRepository).findAll(any(Specification.class));
    }

    @Test
    void toResponse_shouldMapSectorFields() {
        SectorResponse response = sectorService.toResponse(sector);

        assertEquals(1L, response.getId());
        assertEquals("Gueliz", response.getName());
        assertEquals(-8.0, response.getCenterLng());
    }
}
