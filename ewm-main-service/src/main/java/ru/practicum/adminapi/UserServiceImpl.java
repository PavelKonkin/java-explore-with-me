package ru.practicum.adminapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.User;
import ru.practicum.user.UserMapper;
import ru.practicum.user.UserRepository;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public UserDto add(NewUserRequest newUserRequest) {
        User user = userMapper.convertNewUserRequest(newUserRequest);
        return userMapper.convertUser(userRepository.save(user));
    }

    @Override
    public List<UserDto> getAll(List<Long> ids, Pageable page) {
        if (ids == null || ids.isEmpty()) {
            ids = null;
        }
        return userRepository.findAllInIds(ids, page);
    }

    @Override
    public void delete(long userId) {
        User userToDelete = userRepository.findById(userId).orElseThrow(() ->
                new NotFoundException("User with id=" + userId + " was not found"));
        userRepository.delete(userToDelete);
    }
}
