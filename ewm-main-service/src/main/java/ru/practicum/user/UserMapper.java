package ru.practicum.user;

import org.springframework.stereotype.Component;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserShortDto;

@Component
public class UserMapper {
    public User convertNewUserRequest(NewUserRequest newUserRequest) {
        return User.builder()
                .email(newUserRequest.getEmail())
                .name(newUserRequest.getName())
                .build();
    }

    public UserDto convertUser(User user, Long rating) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .rating(rating)
                .build();
    }

    public UserShortDto convertUserToShortDto(User user, Long rating) {
        return UserShortDto.builder()
                .id(user.getId())
                .name(user.getName())
                .rating(rating)
                .build();
    }
}
