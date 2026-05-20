package ma.urbanops.controller;

import ma.urbanops.dto.response.CategoryResponse;
import ma.urbanops.entity.Category;
import ma.urbanops.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock private CategoryService categoryService;

    @InjectMocks private CategoryController categoryController;

    @Test
    void getAllCategories_shouldMapActiveCategories() {
        Category category = Category.builder().id(1L).name("Voirie").build();
        CategoryResponse response = CategoryResponse.builder().id(1L).name("Voirie").build();
        when(categoryService.findAllActive()).thenReturn(List.of(category));
        when(categoryService.toResponse(category)).thenReturn(response);

        List<CategoryResponse> result = categoryController.getAllCategories().getBody();

        assertEquals(1, result.size());
        assertEquals("Voirie", result.get(0).getName());
    }

    @Test
    void getCategoryById_shouldReturnMappedCategory() {
        Category category = Category.builder().id(1L).name("Voirie").build();
        CategoryResponse response = CategoryResponse.builder().id(1L).name("Voirie").build();
        when(categoryService.findById(1L)).thenReturn(category);
        when(categoryService.toResponse(category)).thenReturn(response);

        assertEquals("Voirie", categoryController.getCategoryById(1L).getBody().getName());
    }
}
