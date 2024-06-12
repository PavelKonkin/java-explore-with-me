package ru.practicum.publicapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryMapper;
import ru.practicum.category.CategoryRepository;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.page.OffsetPage;

import javax.transaction.Transactional;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
@SpringBootTest
public class PublicCategoryTest {
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private PublicCategoryService categoryService;

    private Category category1;
    private Category category2;
    private CategoryDto categoryDto1;
    private CategoryDto categoryDto2;
    private final Sort sort = Sort.by("id").descending();
    private final int from = 0;
    private final int size = 10;
    private final Pageable page = new OffsetPage(from, size, sort);

    @BeforeEach
    public void setup() {
        category1 = Category.builder()
                .name("test1")
                .build();
        category1 = categoryRepository.save(category1);
        categoryDto1 = categoryMapper.convertCategory(category1);
        category2 = Category.builder()
                .name("test2")
                .build();
        category2 = categoryRepository.save(category2);
        categoryDto2 = categoryMapper.convertCategory(category2);
    }

    @Test
    public void getAll_whenCategoriesFound_thenReturnListOfCategoryDto() {
        List<CategoryDto> actualListOfCategoryDto = categoryService.getAll(page);

        assertThat(List.of(categoryDto2, categoryDto1), is(actualListOfCategoryDto));
    }

    @Test
    public void getAll_whenCategoriesNotFound_thenReturnEmptyList() {
        Pageable emptyListPage = new OffsetPage(10, 1, sort);

        List<CategoryDto> actualListOfCategoryDto = categoryService.getAll(emptyListPage);

        assertThat(List.of(), is(actualListOfCategoryDto));
    }

    @Test
    public void get_whenCategoryFound_thenReturnCategoryDto() {
        CategoryDto actualCategoryDto = categoryService.get(category1.getId());

        assertThat(categoryDto1, is(actualCategoryDto));
    }

    @Test
    public void get_whenCategoryNotFound_thenThrownException() {
        long wrongId = 66L;

        NotFoundException exception = assertThrows(NotFoundException.class, () -> categoryService.get(wrongId));

        assertThat(exception.getMessage(), is("Category with id=" + wrongId + " was not found"));
    }
}
