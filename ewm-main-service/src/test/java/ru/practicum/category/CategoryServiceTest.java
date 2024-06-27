package ru.practicum.category;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.event.EventRepository;
import ru.practicum.exception.NotFoundException;
import ru.practicum.page.OffsetPage;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private EventRepository eventRepository;
    @InjectMocks
    private CategoryServiceImpl categoryService;

    private NewCategoryDto newCategoryDto;
    private Category category;
    private Category updatedCategory;
    private Category newCategory;
    private CategoryDto categoryDto;
    private CategoryDto updatedCategoryDto;
    private Category category1;
    private Category category2;
    private CategoryDto categoryDto1;
    private CategoryDto categoryDto2;
    private final Sort sort = Sort.by("id").descending();
    private final Pageable page = new OffsetPage(0, 10, sort);

    @BeforeEach
    public void setup() {
        category = Category.builder()
                .id(1L)
                .name("test1")
                .build();
        updatedCategory = category.toBuilder()
                .name("updated name")
                .build();
        categoryDto = CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
        newCategoryDto = NewCategoryDto.builder()
                .name(category.getName())
                .build();
        updatedCategoryDto = CategoryDto.builder()
                .id(updatedCategory.getId())
                .name(updatedCategory.getName())
                .build();
        newCategory = Category.builder()
                .name(newCategoryDto.getName())
                .build();
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
    public void add_whenSuccessful_thenReturnCategoryDto() {
        when(categoryMapper.convertNewCategoryDto(newCategoryDto)).thenReturn(newCategory);
        when(categoryRepository.save(newCategory)).thenReturn(category);
        when(categoryMapper.convertCategory(category)).thenReturn(categoryDto);

        CategoryDto actualCategoryDto = categoryService.add(newCategoryDto);

        assertThat(categoryDto, equalTo(actualCategoryDto));
        verify(categoryMapper, times(1)).convertNewCategoryDto(newCategoryDto);
        verify(categoryRepository, times(1)).save(newCategory);
        verify(categoryMapper, times(1)).convertCategory(category);
    }

    @Test
    public void add_whenCategoryNameAlreadyExists_thenThrownException() {
        when(categoryMapper.convertNewCategoryDto(newCategoryDto)).thenReturn(newCategory);
        when(categoryRepository.save(newCategory)).thenThrow(new DataIntegrityViolationException(
                "could not execute statement; SQL [n/a]; constraint [uq_category_name];" +
                        " nested exception is org.hibernate.exception.ConstraintViolationException:" +
                        " could not execute statement"));


        DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
                () -> categoryService.add(newCategoryDto));

        verify(categoryMapper, times(1)).convertNewCategoryDto(newCategoryDto);
        verify(categoryRepository, times(1)).save(newCategory);
        verify(categoryMapper, never()).convertCategory(any(Category.class));
        assertThat(exception.getMessage(),
                is("could not execute statement; SQL [n/a]; constraint [uq_category_name];" +
                        " nested exception is org.hibernate.exception.ConstraintViolationException:" +
                        " could not execute statement"));
    }

    @Test
    public void delete_whenSuccessful_thenDoNothing() {
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(eventRepository.findFirstByCategoryId(category.getId())).thenReturn(Optional.empty());
        doNothing().when(categoryRepository).delete(category);

        categoryService.delete(category.getId());

        verify(categoryRepository, times(1)).findById(category.getId());
        verify(categoryRepository, times(1)).delete(category);
    }

    @Test
    public void delete_whenCategoryNotFound_thenThrowException() {
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> categoryService.delete(category.getId()));

        verify(categoryRepository, times(1)).findById(category.getId());
        verify(categoryRepository, never()).delete(any(Category.class));
        assertThat(exception.getMessage(), is("Category with id=" + category.getId() + " was not found"));
    }

    @Test
    public void patch_whenSuccessful_thenReturnCategoryDto() {
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(categoryRepository.save(updatedCategory)).thenReturn(updatedCategory);
        when(categoryMapper.convertCategory(updatedCategory)).thenReturn(updatedCategoryDto);

        CategoryDto actualUpdatedCategoryDto = categoryService.patch(category.getId(), updatedCategoryDto);

        assertThat(updatedCategoryDto, is(actualUpdatedCategoryDto));
        verify(categoryRepository, times(1)).findById(category.getId());
        verify(categoryRepository, times(1)).save(updatedCategory);
        verify(categoryMapper, times(1)).convertCategory(updatedCategory);
    }

    @Test
    public void patch_whenCategoryNotFound_thenThrownException() {
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> categoryService.patch(category.getId(), updatedCategoryDto));

        verify(categoryRepository, times(1)).findById(category.getId());
        verify(categoryRepository, never()).save(any(Category.class));
        verify(categoryMapper, never()).convertCategory(any(Category.class));
        assertThat(exception.getMessage(), is("Category with id=" + category.getId() + " was not found"));
    }

    @Test
    public void patch_whenCategoryNameAlreadyExists_thenThrownException() {
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(categoryRepository.save(updatedCategory)).thenThrow(new DataIntegrityViolationException(
                "could not execute statement; SQL [n/a]; constraint [uq_category_name];" +
                        " nested exception is org.hibernate.exception.ConstraintViolationException:" +
                        " could not execute statement"));

        DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
                () -> categoryService.patch(category.getId(), updatedCategoryDto));

        verify(categoryRepository, times(1)).findById(category.getId());
        verify(categoryRepository, times(1)).save(updatedCategory);
        verify(categoryMapper, never()).convertCategory(updatedCategory);
        assertThat(exception.getMessage(),
                is("could not execute statement; SQL [n/a]; constraint [uq_category_name];" +
                        " nested exception is org.hibernate.exception.ConstraintViolationException:" +
                        " could not execute statement"));
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
        when(categoryRepository.findById(category1.getId())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> categoryService.get(category1.getId()));

        assertThat(exception.getMessage(), is("Category with id=" + category1.getId() + " was not found"));
        verify(categoryRepository, times(1)).findById(category1.getId());
        verify(categoryMapper, never()).convertCategory(category1);
    }
}
