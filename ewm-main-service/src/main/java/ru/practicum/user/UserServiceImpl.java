package ru.practicum.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

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
        return userMapper.convertUser(userRepository.save(user), 0L);
    }

    @Override
    public List<UserDto> getAll(List<Long> ids, Pageable page) {
        if (ids == null || ids.isEmpty()) {
            ids = List.of();
        }
        List<Object[]> userObj = userRepository.findAllInIds(ids, ids.size(), page.getPageSize(), page.getOffset());
        return userObj.stream()
                .map(result -> new UserDto(
                        ((BigInteger) result[0]).longValue(),
                        (String) result[1],
                        (String) result[2],
                        ((BigInteger) result[3]).longValue()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public void delete(long userId) {
        User userToDelete = userRepository.findById(userId).orElseThrow(() ->
                new NotFoundException("User with id=" + userId + " was not found"));
        userRepository.delete(userToDelete);
    }
}
