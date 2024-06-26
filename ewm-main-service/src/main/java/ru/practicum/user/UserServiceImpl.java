package ru.practicum.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.event.EventUserRatingRepository;
import ru.practicum.event.dto.RatingDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final EventUserRatingRepository eventUserRatingRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           UserMapper userMapper,
                           EventUserRatingRepository eventUserRatingRepository) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.eventUserRatingRepository = eventUserRatingRepository;
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

        List<UserDto> users = userRepository.findAllInIds(ids, ids.size(), page);
        Map<Long, Long> usersRating = eventUserRatingRepository.findUsersRatingByUserIds(users.stream()
                .map(UserDto::getId)
                .collect(Collectors.toList())).stream()
                .collect(Collectors.toMap(
                        RatingDto::getId,  // eventId
                        RatingDto::getRating    // rating
                ));;
        return users.stream()
                .peek(dto -> dto.setRating(usersRating.getOrDefault(dto.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    public void delete(long userId) {
        User userToDelete = userRepository.findById(userId).orElseThrow(() ->
                new NotFoundException("User with id=" + userId + " was not found"));
        userRepository.delete(userToDelete);
    }
}
