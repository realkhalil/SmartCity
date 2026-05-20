package ma.urbanops.service;

import ma.urbanops.dto.response.CategoryResponse;
import ma.urbanops.entity.Category;
import ma.urbanops.exception.ResourceNotFoundException;
import ma.urbanops.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;

    @InjectMocks private CategoryService categoryService;

    @Test
    void findMethods_shouldDelegateToRepository() {
        Category category = Category.builder().id(1L).name("Voirie").build();
        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertEquals(List.of(category), categoryService.findAll());
        assertEquals(List.of(category), categoryService.findAllActive());
        assertSame(category, categoryService.findById(1L));
    }

    @Test
    void findById_whenMissing_shouldThrow() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.findById(99L));
    }

    @Test
    void createUpdateDeleteAndMap_shouldUseRepository() {
        Category existing = Category.builder()
                .id(1L)
                .name("Old")
                .icon("old")
                .defaultAuthority("Old Authority")
                .authorityEmail("old@test.ma")
                .build();
        Category update = Category.builder()
                .name("Voirie")
                .icon("road")
                .defaultAuthority("Travaux Publics")
                .authorityEmail("voirie@test.ma")
                .build();
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertSame(update, categoryService.create(update));
        Category result = categoryService.update(1L, update);
        categoryService.delete(1L);
        CategoryResponse response = categoryService.toResponse(result);

        assertEquals("Voirie", result.getName());
        assertEquals("road", response.getIcon());
        assertEquals("Travaux Publics", response.getDefaultAuthority());
        verify(categoryRepository).deleteById(1L);
    }
}
