package ru.practicum.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.practicum.page.OffsetPage;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
@SpringBootTest
public class UserTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserService userService;

    private User user;
    private UserDto userDto;
    private NewUserRequest userToCreate;
    private final Sort sort = Sort.by("id").descending();
    private final Pageable page = new OffsetPage(0, 10, sort);

    @BeforeEach
    void setup() {
        user = User.builder()
                .name("user name")
                .email("user@email.test")
                .build();
        userRepository.save(user);
        userDto = userMapper.convertUser(user, 0L);
        userToCreate = NewUserRequest.builder()
                .name("user create name")
                .email("user_create@email.test")
                .build();
    }

    @Test
    void getAll_whenSuccessful_thenReturnListOfUserDto() {
        List<UserDto> actualUsers = userService.getAll(List.of(user.getId()), page);

        assertThat(List.of(userDto), is(actualUsers));
    }

    @Test
    void delete_whenSuccessful_thenNoUserFoundInRepository() {
        userService.delete(user.getId());

        Optional<User> actualUser = userRepository.findById(user.getId());

        assertTrue(actualUser.isEmpty());
    }

    @Test
    void create_whenSuccessful_thenReturnUserDto() {
        UserDto actualUserDto = userService.add(userToCreate);

        List<UserDto> savedUsersDto = userService.getAll(List.of(actualUserDto.getId()), page);

        assertThat(savedUsersDto, is(List.of(actualUserDto)));
    }
}
