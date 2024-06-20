package ru.practicum.publicapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryMapper;
import ru.practicum.category.CategoryRepository;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.page.OffsetPage;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CategoryMapper categoryMapper;
    @InjectMocks
    private CategoryServiceImpl categoryService;


    private Category category1;
    private Category category2;
    private CategoryDto categoryDto1;
    private CategoryDto categoryDto2;

    private final Sort sort = Sort.by("id").descending();
    private final Pageable page = new OffsetPage(0, 10, sort);

    @BeforeEach
    public void setup() {
        category1 = Category.builder()
                .id(1L)
                .name("test1")
                .build();
        categoryDto1 = CategoryDto.builder()
                .id(category1.getId())
                .name(category1.getName())
                .build();
        category2 = Category.builder()
                .id(2L)
                .name("test2")
                .build();
        categoryDto2 = CategoryDto.builder()
                .id(category2.getId())
                .name(category2.getName())
                .build();
    }

    @Test
    public void getAll_whenFoundCategories_thenReturnListOfCategoryDto() {
        when(categoryRepository.findBy(page)).thenReturn(List.of(category1, category2));
        when(categoryMapper.convertCategory(category1)).thenReturn(categoryDto1);
        when(categoryMapper.convertCategory(category2)).thenReturn(categoryDto2);

        List<CategoryDto> actualListOfCategoryDto = categoryService.getAll(page);

        assertThat(List.of(categoryDto1, categoryDto2), is(actualListOfCategoryDto));
        verify(categoryRepository, times(1)).findBy(page);
        verify(categoryMapper, times(2)).convertCategory(any(Category.class));
    }

    @Test
    public void getAll_whenCategoriesNotFound_thenReturnEmptyList() {
        when(categoryRepository.findBy(page)).thenReturn(List.of());

        List<CategoryDto> actualListOfCategoryDto = categoryService.getAll(page);

        assertThat(List.of(), is(actualListOfCategoryDto));
        verify(categoryRepository, times(1)).findBy(page);
        verify(categoryMapper, never()).convertCategory(any(Category.class));
    }

    @Test
    public void get_whenSuccessful_thenReturnCategoryDto() {
        when(categoryRepository.findById(category1.getId())).thenReturn(Optional.of(category1));
        when(categoryMapper.convertCategory(category1)).thenReturn(categoryDto1);

        CategoryDto actualCategoryDto = categoryService.get(category1.getId());

        assertThat(categoryDto1, is(actualCategoryDto));
        verify(categoryRepository, times(1)).findById(category1.getId());
        verify(categoryMapper, times(1)).convertCategory(category1);
    }

    @Test
    public void get_whenCategoryNotFound_thenThrownException() {
        when(categoryRepository.findById(category1.getId())).thenReturn(Optional.empty());;

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> categoryService.get(category1.getId()));

        assertThat(exception.getMessage(), is("Category with id=" + category1.getId() + " was not found"));
        verify(categoryRepository, times(1)).findById(category1.getId());
        verify(categoryMapper, never()).convertCategory(category1);
    }
}
