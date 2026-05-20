package ma.urbanops.controller;

import ma.urbanops.entity.Sector;
import ma.urbanops.service.SectorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SectorControllerTest {

    @Mock private SectorService sectorService;

    @InjectMocks private SectorController sectorController;

    private final Sector sector = Sector.builder()
            .id(1L)
            .name("Gueliz")
            .city("Marrakech")
            .centerLat(31.63)
            .centerLng(-8.0)
            .build();

    @Test
    void getAllSectors_shouldMapSectors() {
        when(sectorService.findAllActive()).thenReturn(List.of(sector));

        assertEquals("Gueliz", sectorController.getAllSectors().getBody().get(0).getName());
    }

    @Test
    void getSectorById_shouldReturnMappedSector() {
        when(sectorService.findById(1L)).thenReturn(sector);

        assertEquals("Marrakech", sectorController.getSectorById(1L).getBody().getCity());
    }

    @Test
    void getByCity_shouldMapCitySectors() {
        when(sectorService.findByCity("Marrakech")).thenReturn(List.of(sector));

        assertEquals(31.63, sectorController.getByCity("Marrakech").getBody().get(0).getCenterLat());
    }
}
