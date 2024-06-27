package ru.practicum.category;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.page.OffsetPage;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CategoryController.class)
public class CategoryControllerTest {
    @Autowired
    private ObjectMapper mapper;
    @MockBean
    private CategoryService categoryService;
    @Autowired
    private MockMvc mvc;

    private NewCategoryDto newCategoryDto;
    private CategoryDto categoryDto;
    private NewCategoryDto wrongNewCategoryDto;
    private CategoryDto updatedCategoryDto;
    private CategoryDto categoryDto1;
    private CategoryDto categoryDto2;
    private final Sort sort = Sort.by("id").descending();
    private final int from = 0;
    private final int size = 10;
    private final Pageable page = new OffsetPage(from, size, sort);

    @BeforeEach
    void setup() {
        categoryDto = CategoryDto.builder()
                .id(1L)
                .name("test")
                .build();
        newCategoryDto = NewCategoryDto.builder()
                .name(categoryDto.getName())
                .build();
        wrongNewCategoryDto = NewCategoryDto.builder().build();
        updatedCategoryDto = CategoryDto.builder()
                .id(categoryDto.getId())
                .name("updated name")
                .build();
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
    public void add_whenSuccessful_thenReturnCategoryDto() throws Exception {
        when(categoryService.add(newCategoryDto)).thenReturn(categoryDto);

        mvc.perform(post("/admin/categories")
                        .content(mapper.writeValueAsString(newCategoryDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(categoryDto.getId()), Long.class))
                .andExpect(jsonPath("$.name", is(categoryDto.getName())));
        verify(categoryService, times(1)).add(newCategoryDto);
    }

    @Test
    public void add_whenRequestBodyNotValid_thenThrownException() throws Exception {
        mvc.perform(post("/admin/categories")
                        .content(mapper.writeValueAsString(wrongNewCategoryDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class,
                        result.getResolvedException()));
        verify(categoryService, never()).add(wrongNewCategoryDto);
    }

    @Test
    public void add_whenCategoryNameAlreadyExist_thenThrownException() throws Exception {
        doThrow(new DataIntegrityViolationException("")).when(categoryService).add(newCategoryDto);

        mvc.perform(post("/admin/categories")
                        .content(mapper.writeValueAsString(newCategoryDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(result -> assertInstanceOf(DataIntegrityViolationException.class,
                        result.getResolvedException()));
        verify(categoryService, times(1)).add(newCategoryDto);
    }

    @Test
    public void delete_whenSuccessful_thenNoContentStatus() throws Exception {
        doNothing().when(categoryService).delete(categoryDto.getId());

        mvc.perform(delete("/admin/categories/" + categoryDto.getId())
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        verify(categoryService, times(1)).delete(categoryDto.getId());
    }

    @Test
    public void delete_whenCategoryNotFound_thenThrownException() throws Exception {
        doThrow(new NotFoundException("")).when(categoryService).delete(categoryDto.getId());

        mvc.perform(delete("/admin/categories/" + categoryDto.getId())
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertInstanceOf(NotFoundException.class,
                        result.getResolvedException()));
        verify(categoryService, times(1)).delete(categoryDto.getId());
    }

    @Test
    public void delete_whenCategoryHasEvent_thenThrownException() throws Exception {
        doThrow(new ConflictException("")).when(categoryService).delete(categoryDto.getId());

        mvc.perform(delete("/admin/categories/" + categoryDto.getId())
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(result -> assertInstanceOf(ConflictException.class,
                        result.getResolvedException()));
        verify(categoryService, times(1)).delete(categoryDto.getId());
    }

    @Test
    public void patch_whenSuccessful_thenReturnCategoryDto() throws Exception {
        when(categoryService.patch(categoryDto.getId(), updatedCategoryDto)).thenReturn(updatedCategoryDto);

        mvc.perform(patch("/admin/categories/" + categoryDto.getId())
                        .content(mapper.writeValueAsString(updatedCategoryDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(categoryDto.getId()), Long.class))
                .andExpect(jsonPath("$.name", is(updatedCategoryDto.getName())));
        verify(categoryService, times(1)).patch(categoryDto.getId(), updatedCategoryDto);
    }

    @Test
    public void patch_whenCategoryNotFound_thenThrownException() throws Exception {
        doThrow(new NotFoundException("")).when(categoryService).patch(categoryDto.getId(), updatedCategoryDto);

        mvc.perform(patch("/admin/categories/" + categoryDto.getId())
                        .content(mapper.writeValueAsString(updatedCategoryDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertInstanceOf(NotFoundException.class,
                        result.getResolvedException()));
        verify(categoryService, times(1)).patch(categoryDto.getId(), updatedCategoryDto);
    }

    @Test
    public void patch_whenCategoryNameAlreadyExists_thenThrownException() throws Exception {
        doThrow(new DataIntegrityViolationException("")).when(categoryService).patch(categoryDto.getId(), updatedCategoryDto);

        mvc.perform(patch("/admin/categories/" + categoryDto.getId())
                        .content(mapper.writeValueAsString(updatedCategoryDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(result -> assertInstanceOf(DataIntegrityViolationException.class,
                        result.getResolvedException()));
        verify(categoryService, times(1)).patch(categoryDto.getId(), updatedCategoryDto);
    }

    @Test
    public void patch_whenNameIsBlank_thenThrownException() throws Exception {
        updatedCategoryDto.setName("");

        mvc.perform(patch("/admin/categories/" + categoryDto.getId())
                        .content(mapper.writeValueAsString(updatedCategoryDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class,
                        result.getResolvedException()));
        verify(categoryService, never()).patch(any(Long.class), any(CategoryDto.class));
    }

    @Test
    public void patch_whenPathVariableCategoryIdNotValid_thenThrownException() throws Exception {
        mvc.perform(patch("/admin/categories/" + "ad")
                        .content(mapper.writeValueAsString(updatedCategoryDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentTypeMismatchException.class,
                        result.getResolvedException()));
        verify(categoryService, never()).patch(any(Long.class), any(CategoryDto.class));
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
