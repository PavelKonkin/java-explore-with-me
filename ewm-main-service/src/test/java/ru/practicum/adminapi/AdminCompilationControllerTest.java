package ru.practicum.adminapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminCompilationController.class)
public class AdminCompilationControllerTest {
    @Autowired
    private ObjectMapper mapper;
    @MockBean
    private AdminCompilationService compilationService;
    @Autowired
    private MockMvc mvc;

    private NewCompilationDto newCompilationDto;
    private NewCompilationDto wrongNewCompilationDto;
    private CompilationDto compilationDto;
    private UpdateCompilationRequest updateCompilationRequest;
    private CompilationDto updatedCompilationDto;

    @BeforeEach
    public void setup() {
        newCompilationDto = NewCompilationDto.builder()
                .title("new title")
                .pinned(false)
                .build();
        wrongNewCompilationDto = NewCompilationDto.builder()
                .title("")
                .pinned(false)
                .build();
        compilationDto = CompilationDto.builder()
                .id(1L)
                .pinned(newCompilationDto.isPinned())
                .title(newCompilationDto.getTitle())
                .build();
        updateCompilationRequest = UpdateCompilationRequest.builder()
                .title("updated title")
                .pinned(true)
                .build();
        updatedCompilationDto = CompilationDto.builder()
                .id(compilationDto.getId())
                .title(updateCompilationRequest.getTitle())
                .pinned(updateCompilationRequest.getPinned())
                .build();
    }

    @Test
    public void add_whenSuccessful_thenReturnCompilationDto() throws Exception {
        when(compilationService.add(newCompilationDto)).thenReturn(compilationDto);

        mvc.perform(post("/admin/compilations")
                        .content(mapper.writeValueAsString(newCompilationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(compilationDto.getId()), Long.class))
                .andExpect(jsonPath("$.pinned", is(compilationDto.isPinned())))
                .andExpect(jsonPath("$.title", is(compilationDto.getTitle())));
        verify(compilationService, times(1)).add(newCompilationDto);
    }

    @Test
    public void add_whenRequestBodyNotValid_thenThrownException() throws Exception {
        mvc.perform(post("/admin/compilations")
                        .content(mapper.writeValueAsString(wrongNewCompilationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class,
                        result.getResolvedException()));
        verify(compilationService, never()).add(any(NewCompilationDto.class));
    }

    @Test
    public void delete_whenSuccessful_thenNoContentStatus() throws Exception {
        doNothing().when(compilationService).delete(compilationDto.getId());

        mvc.perform(delete("/admin/compilations/" + compilationDto.getId())
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        verify(compilationService, times(1)).delete(compilationDto.getId());
    }

    @Test
    public void delete_whenNotValidPathVariable_thenThrownException() throws Exception {
        String wrongId = "ad";
        mvc.perform(delete("/admin/compilations/" + wrongId)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentTypeMismatchException.class,
                        result.getResolvedException()));
    }

    @Test
    public void patch_whenSuccessful_thenReturnUpdatedCompilationDto() throws Exception {
        when(compilationService.patch(compilationDto.getId(), updateCompilationRequest))
                .thenReturn(updatedCompilationDto);

        mvc.perform(patch("/admin/compilations/" + compilationDto.getId())
                        .content(mapper.writeValueAsString(updateCompilationRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(updatedCompilationDto.getId()), Long.class))
                .andExpect(jsonPath("$.pinned", is(updatedCompilationDto.isPinned())))
                .andExpect(jsonPath("$.title", is(updatedCompilationDto.getTitle())));
        verify(compilationService, times(1))
                .patch(compilationDto.getId(), updateCompilationRequest);
    }
}
