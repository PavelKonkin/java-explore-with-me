package ru.practicum.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.adminapi.UserController;
import ru.practicum.adminapi.UserService;
import ru.practicum.exception.NotFoundException;
import ru.practicum.page.OffsetPage;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
public class UserControllerTest {
    @Autowired
    private ObjectMapper mapper;
    @MockBean
    private UserService userService;
    @Autowired
    private MockMvc mvc;
    private UserDto userDto;
    private NewUserRequest newUserRequest;
    private final Sort sort = Sort.by("id").ascending();
    private final Pageable page = new OffsetPage(0, 10, sort);
    private final Pageable wrongPage = new OffsetPage(-1, 10, sort);

    @BeforeEach
    void setup() {
        userDto = UserDto.builder()
                .id(1L)
                .name("user test")
                .email("user@test.test")
                .build();
        newUserRequest = NewUserRequest.builder()
                .name(userDto.getName())
                .email(userDto.getEmail())
                .build();
    }

    @Test
    void getAll_whenSuccessful_thenReturnListUserDto() throws Exception {
        when(userService.getAll(null, page)).thenReturn(List.of(userDto));

        mvc.perform(get("/admin/users/")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].id", is(userDto.getId()), Long.class))
                .andExpect(jsonPath("$.[0].name", is(userDto.getName())))
                .andExpect(jsonPath("$.[0].email", equalTo(userDto.getEmail())));
        verify(userService, times(1)).getAll(null, page);
    }

    @Test
    void getAll_whenInvalidRequestParam_thenThrownException() throws Exception {
        when(userService.getAll(null, wrongPage))
                .thenThrow(new IllegalArgumentException());

        mvc.perform(get("/admin/users?from=-1")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(IllegalArgumentException.class,
                        result.getResolvedException()));
        verify(userService, times(1)).getAll(null, wrongPage);
    }

    @Test
    void create_whenSuccessful_thenReturnUserDto() throws Exception {
        when(userService.add(newUserRequest)).thenReturn(userDto);

        mvc.perform(post("/admin/users")
                .content(mapper.writeValueAsString(newUserRequest))
                .characterEncoding(StandardCharsets.UTF_8)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(userDto.getId()), Long.class))
                .andExpect(jsonPath("$.name", is(userDto.getName())))
                .andExpect(jsonPath("$.email", equalTo(userDto.getEmail())));
        verify(userService, times(1)).add(newUserRequest);
    }

    @Test
    void getAll_whenSuccessful_thenReturnListOfUserDtos() throws Exception {
        when(userService.getAll(null, page)).thenReturn(List.of(userDto));

        mvc.perform(get("/admin/users")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].id", is(userDto.getId()), Long.class))
                .andExpect(jsonPath("$.[0].name", is(userDto.getName())))
                .andExpect(jsonPath("$.[0].email", is(userDto.getEmail())));
        verify(userService, times(1)).getAll(null, page);
    }

    @Test
    void delete_whenSuccessful_thenOkStatus() throws Exception {
        doNothing().when(userService).delete(1);
        mvc.perform(delete("/admin/users/1")
                .characterEncoding(StandardCharsets.UTF_8)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        verify(userService, times(1)).delete(1);
    }

    @Test
    void delete_whenUserNotFound_thenThrownException() throws Exception {
        doThrow(new NotFoundException("")).when(userService).delete(2);
        mvc.perform(delete("/admin/users/2")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertInstanceOf(NotFoundException.class,
                        result.getResolvedException()));
        verify(userService, times(1)).delete(2);
    }
}
