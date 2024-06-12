package ru.practicum.publicapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.page.OffsetPage;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(controllers = PublicCompilationController.class)
public class PublicCompilationControllerTest {
    @MockBean
    private PublicCompilationService compilationService;
    @Autowired
    private MockMvc mvc;

    private CompilationDto compilationDto1;
    private CompilationDto compilationDto2;
    private final Sort sort = Sort.by("id").descending();
    private final int from = 0;
    private final int size = 10;
    private final boolean pinned = true;

    @BeforeEach
    public void setup() {
        compilationDto1 = CompilationDto.builder()
                .id(1L)
                .title("comp test1")
                .pinned(true)
                .events(List.of())
                .build();
        compilationDto2 = CompilationDto.builder()
                .id(2L)
                .title("comp test2")
                .pinned(false)
                .build();
    }

    @Test
    public void getAll_whenSuccessful_thenReturnListOfCompilationDto() throws Exception {
        Pageable page = new OffsetPage(from, size, sort);
        when(compilationService.getAll(pinned, page)).thenReturn(List.of(compilationDto1));

        mvc.perform(get("/compilations?from=" + from + "&size=" + size + "&pinned=" + pinned)
                .characterEncoding(StandardCharsets.UTF_8)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].id", is(compilationDto1.getId()), Long.class))
                .andExpect(jsonPath("$.[0].pinned", is(compilationDto1.isPinned())))
                .andExpect(jsonPath("$.[0].title", is(compilationDto1.getTitle())));
        verify(compilationService, times(1)).getAll(pinned, page);
    }

    @Test
    public void getAll_whenNotValidRequestParam_thenThrownException() throws Exception {
        String wrongSize = "ad";

        mvc.perform(get("/compilations?from=" + from + "&size=" + wrongSize + "&pinned=" + pinned)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentTypeMismatchException.class,
                        result.getResolvedException()));
        verify(compilationService, never()).getAll(anyBoolean(), any(Pageable.class));
    }

    @Test
    public void get_whenSuccessful_thenReturnCompilationDto() throws Exception {
        when(compilationService.get(compilationDto2.getId())).thenReturn(compilationDto2);

        mvc.perform(get("/compilations/" + compilationDto2.getId())
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(compilationDto2.getId()), Long.class))
                .andExpect(jsonPath("$.pinned", is(compilationDto2.isPinned())))
                .andExpect(jsonPath("$.title", is(compilationDto2.getTitle())));
        verify(compilationService, times(1)).get(compilationDto2.getId());
    }

    @Test
    public void get_whenPathVariableNotValid_thenThrownException() throws Exception {
        String wrongId = "ad";

        mvc.perform(get("/compilations/" + wrongId)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentTypeMismatchException.class,
                        result.getResolvedException()));
        verify(compilationService, never()).get(anyLong());
    }
}
