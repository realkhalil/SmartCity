package ma.urbanops.config;

import ma.urbanops.entity.Category;
import ma.urbanops.entity.Incident;
import ma.urbanops.entity.Sector;
import ma.urbanops.repository.CategoryRepository;
import ma.urbanops.repository.IncidentRepository;
import ma.urbanops.repository.SectorRepository;
import ma.urbanops.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataSeederTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private SectorRepository sectorRepository;
    @Mock private UserRepository userRepository;
    @Mock private IncidentRepository incidentRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private DataSeeder dataSeeder;

    @BeforeEach
    void setUp() {
        dataSeeder = new DataSeeder(categoryRepository, sectorRepository, userRepository, incidentRepository, passwordEncoder);
    }

    @Test
    void run_whenRepositoriesAreEmpty_shouldSeedCatalogUsersAndIncidents() {
        when(categoryRepository.count()).thenReturn(0L);
        when(sectorRepository.count()).thenReturn(0L);
        when(userRepository.count()).thenReturn(0L);
        when(incidentRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        stubCategoryLookups();
        stubSectorLookups();

        dataSeeder.run();

        verify(categoryRepository).saveAll(anyList());
        verify(sectorRepository).saveAll(anyList());
        verify(userRepository, times(2)).save(any());
        verify(incidentRepository, times(2)).saveAll(anyList());
    }

    @Test
    void run_whenDataAlreadyExists_shouldOnlyRemoveLegacyDuplicates() {
        Category legacy = Category.builder().name("Transport & Trafic").build();
        when(categoryRepository.findByName("Transport & Trafic")).thenReturn(Optional.of(legacy));
        when(categoryRepository.count()).thenReturn(5L);
        when(sectorRepository.count()).thenReturn(5L);
        when(userRepository.count()).thenReturn(5L);
        when(incidentRepository.count()).thenReturn(5L);

        dataSeeder.run();

        verify(categoryRepository).delete(legacy);
        verify(categoryRepository, never()).saveAll(anyList());
        verify(sectorRepository, never()).saveAll(anyList());
        verify(userRepository, never()).save(any());
        verify(incidentRepository, never()).saveAll(anyList());
    }

    private void stubCategoryLookups() {
        when(categoryRepository.findByName(anyString()))
                .thenAnswer(invocation -> Optional.of(Category.builder()
                        .name(invocation.getArgument(0))
                        .build()));
    }

    private void stubSectorLookups() {
        when(sectorRepository.findByName(anyString()))
                .thenAnswer(invocation -> Optional.of(Sector.builder()
                        .name(invocation.getArgument(0))
                        .build()));
    }
}
