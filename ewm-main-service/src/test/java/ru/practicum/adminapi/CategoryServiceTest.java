package ru.practicum.adminapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryMapper;
import ru.practicum.category.CategoryRepository;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.event.EventRepository;
import ru.practicum.exception.NotFoundException;

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

}
