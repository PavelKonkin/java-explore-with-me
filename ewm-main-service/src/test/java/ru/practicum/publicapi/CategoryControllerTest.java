package ru.practicum.publicapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.page.OffsetPage;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ru.practicum.publicapi.CategoryController.class)
public class CategoryControllerTest {
    @MockBean
    @Qualifier(value = "publicCategoryService")
    private CategoryService categoryService;
    @Autowired
    private MockMvc mvc;

    private CategoryDto categoryDto1;
    private CategoryDto categoryDto2;
    private final Sort sort = Sort.by("id").descending();
    private final int from = 0;
    private final int size = 10;
    private final Pageable page = new OffsetPage(from, size, sort);

    @BeforeEach
    void setup() {
        categoryDto1 = CategoryDto.builder()
                .id(1L)
                .name("test1")
                .build();
        categoryDto2 = CategoryDto.builder()
                .id(2L)
                .name("test2")
                .build();
    }

    @Test
    public void getAll_whenSuccessful_thenReturnCategoryDto() throws Exception {
        when(categoryService.getAll(page)).thenReturn(List.of(categoryDto1, categoryDto2));

        mvc.perform(get("/categories?from=" + from + "&size=" + size)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].id", is(categoryDto1.getId()), Long.class))
                .andExpect(jsonPath("$.[0].name", is(categoryDto1.getName())))
                .andExpect(jsonPath("$.[1].id", is(categoryDto2.getId()), Long.class))
                .andExpect(jsonPath("$.[1].name", is(categoryDto2.getName())));

        verify(categoryService, times(1)).getAll(page);
    }

    @Test
    public void getAll_whenNotValidRequestParam_thenThrownException() throws Exception {
        String wrongFromParam = "ad";

        mvc.perform(get("/categories?from=" + wrongFromParam + "&size=" + size)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentTypeMismatchException.class,
                        result.getResolvedException()));

        verify(categoryService, never()).getAll(any(Pageable.class));
    }

    @Test
    public void get_whenSuccessful_thenReturnCategoryDto() throws Exception {
        when(categoryService.get(categoryDto1.getId())).thenReturn(categoryDto1);

        mvc.perform(get("/categories" + "/" + categoryDto1.getId())
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(categoryDto1.getId()), Long.class))
                .andExpect(jsonPath("$.name", is(categoryDto1.getName())));


        verify(categoryService, times(1)).get(categoryDto1.getId());
    }

    @Test
    public void get_whenCategoryNotFound_thenThrownException() throws Exception {
        long wrongId = 66L;

        doThrow(new NotFoundException("Category with id=" + wrongId + " was not found"))
                .when(categoryService).get(wrongId);

        mvc.perform(get("/categories" + "/" + wrongId)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertInstanceOf(NotFoundException.class,
                        result.getResolvedException()));

        verify(categoryService, times(1)).get(wrongId);
    }

    @Test
    public void get_whenNotValidPathVariable_thenThrownException() throws Exception {
        String wrongFormatId = "ad";

        mvc.perform(get("/categories" + "/" + wrongFormatId)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentTypeMismatchException.class,
                        result.getResolvedException()));

        verify(categoryService, never()).get(any(Long.class));
    }
}
