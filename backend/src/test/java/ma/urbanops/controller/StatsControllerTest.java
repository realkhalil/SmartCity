package ma.urbanops.controller;

import ma.urbanops.dto.response.StatsResponse;
import ma.urbanops.service.StatsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsControllerTest {

    @Mock private StatsService statsService;

    @InjectMocks private StatsController statsController;

    @Test
    void statsEndpoints_shouldReturnServiceData() {
        StatsResponse dashboard = StatsResponse.builder().totalIncidents(12).build();
        List<StatsResponse.CategoryCount> categories = List.of(new StatsResponse.CategoryCount("Voirie", 5));
        List<StatsResponse.SectorCount> sectors = List.of(new StatsResponse.SectorCount("Gueliz", 3));
        List<Map<String, Object>> hourly = List.of(Map.of("hour", 10, "count", 2));
        List<StatsResponse.ServiceHealth> health = List.of(new StatsResponse.ServiceHealth("Voirie", 80, "green"));

        when(statsService.getDashboardStats()).thenReturn(dashboard);
        when(statsService.getStatsByCategory()).thenReturn(categories);
        when(statsService.getStatsBySector()).thenReturn(sectors);
        when(statsService.getHourlyStats()).thenReturn(hourly);
        when(statsService.getServicesHealth()).thenReturn(health);
        when(statsService.getResolutionRate()).thenReturn(42.5);

        assertSame(dashboard, statsController.getDashboardStats().getBody());
        assertSame(categories, statsController.getStatsByCategory().getBody());
        assertSame(sectors, statsController.getStatsBySector().getBody());
        assertSame(hourly, statsController.getHourlyStats().getBody());
        assertSame(health, statsController.getServicesHealth().getBody());
        assertEquals(42.5, statsController.getResolutionRate().getBody().get("rate"));
    }

    @Test
    void statusEndpoints_shouldReturnStaticIntegrationStatus() {
        assertEquals(HttpStatus.OK, statsController.getJmsStatus().getStatusCode());
        assertEquals("active", statsController.getJmsStatus().getBody().get("status"));
        assertEquals("RMI (Java Remote Method Invocation)", statsController.getRmiStatus().getBody().get("protocol"));
        assertTrue(((List<?>) statsController.getRmiStatus().getBody().get("methods")).contains("ping()"));
    }
}
