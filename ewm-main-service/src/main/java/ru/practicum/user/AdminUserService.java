package ru.practicum.user;

import org.springframework.data.domain.Pageable;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;

import java.util.List;

public interface AdminUserService {
    UserDto add(NewUserRequest newUserRequest);

    List<UserDto> getAll(List<Long> ids, Pageable page);

    void delete(long userId);
}
