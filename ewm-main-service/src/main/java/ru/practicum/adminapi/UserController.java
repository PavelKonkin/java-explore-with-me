package ru.practicum.adminapi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.page.OffsetPage;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/admin/users")
@Slf4j
@Validated
public class UserController {
    private final UserService userService;
    private final Sort sort = Sort.by("id").ascending();


    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto add(@Valid @RequestBody NewUserRequest newUserRequest) {
        log.info("Получен запрос на создание пользователя {}", newUserRequest);
        UserDto userDto = userService.add(newUserRequest);
        log.info("Создан пользователь {}", userDto);
        return userDto;
    }

    @GetMapping
    public List<UserDto> getAll(@RequestParam(required = false) List<Long> ids,
                                @RequestParam(defaultValue = "0") int from,
                                @RequestParam(defaultValue = "10") int size) {
        log.info("Получен запрос на список пользователей из списка id {}, начиная с номера записи {}," +
                " количество записей {}", ids, from, size);
        Pageable page = new OffsetPage(from, size, sort);
        List<UserDto> result = userService.getAll(ids, page);
        log.info("получен список пользователей {}", result);
        return result;
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long userId) {
        log.info("Получен запрос на удаление пользователя с id {}", userId);
        userService.delete(userId);
        log.info("Удален пользователь с id {}", userId);
    }
}
