package ru.practicum.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import ru.practicum.adminapi.UserServiceImpl;
import ru.practicum.exception.NotFoundException;
import ru.practicum.page.OffsetPage;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @InjectMocks
    private UserServiceImpl userService;

    private User user1;
    private User user2;
    private User wrongUser;
    private UserDto userDto1;
    private UserDto userDto2;
    private NewUserRequest newUserRequest;
    private final Sort sort = Sort.by("id").descending();
    private final Pageable page = new OffsetPage(0, 10, sort);

    @BeforeEach
    void setup() {
        user1 = User.builder()
                .id(1L)
                .name("user name")
                .email("mail@user.test")
                .build();
        user2 = User.builder()
                .id(2L)
                .name("user name2")
                .email("mail@user2.test")
                .build();
        wrongUser = User.builder()
                .id(66L)
                .name("wrong user name")
                .email("wrong_mail@user.test")
                .build();
        newUserRequest = NewUserRequest.builder()
                .name(user1.getName())
                .email(user1.getEmail())
                .build();
        userDto1 = UserDto.builder()
                .id(user1.getId())
                .name(user1.getName())
                .email(user1.getEmail())
                .build();
        userDto2 = UserDto.builder()
                .id(user2.getId())
                .name(user2.getName())
                .email(user2.getEmail())
                .build();
    }

    @Test
    void create_whenSuccessful_thenReturnUserDto() {
        when(userMapper.convertNewUserRequest(newUserRequest)).thenReturn(user1);
        when(userRepository.save(user1)).thenReturn(user1);
        when(userMapper.convertUser(user1)).thenReturn(userDto1);

        UserDto actualUserDto = userService.add(newUserRequest);

        assertThat(userDto1, equalTo(actualUserDto));
        verify(userMapper, times(1)).convertNewUserRequest(newUserRequest);
        verify(userMapper, times(1)).convertUser(user1);
        verify(userRepository, times(1)).save(user1);
    }

    @Test
    void delete_whenSuccessful_thenDoNothing() {
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        doNothing().when(userRepository).delete(user1);

        userService.delete(user1.getId());

        verify(userRepository, times(1)).delete(user1);
        verify(userRepository, times(1)).findById(user1.getId());
    }

    @Test
    void delete_whenUserNotFound_thenThrownException() {
        when(userRepository.findById(wrongUser.getId())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.delete(wrongUser.getId()));

        verify(userRepository, never()).delete(wrongUser);
        verify(userRepository, times(1)).findById(wrongUser.getId());
        assertThat(exception.getMessage(), is("User with id=" + wrongUser.getId() + " was not found"));
    }

    @Test
    void getAll_whenThereAreUsers_thenReturnListOfUserDtos() {
        List<UserDto> expectedUserDtos = List.of(userDto1, userDto2);
        when(userRepository.findAllInIds(null, page)).thenReturn(List.of(userDto1, userDto2));


        List<UserDto> actualUserDtos = userService.getAll(null, page);

        assertThat(actualUserDtos, is(expectedUserDtos));
        verify(userRepository, times(1)).findAllInIds(null, page);
    }

    @Test
    void getAll_whenThereAreNoUsers_thenReturnEmptyList() {
        List<UserDto> expectedUserDtos = List.of();
        when(userRepository.findAllInIds(null, page)).thenReturn(List.of());

        List<UserDto> actualUserDtos = userService.getAll(null, page);

        assertThat(actualUserDtos, is(expectedUserDtos));
        verify(userRepository, times(1)).findAllInIds(null, page);
    }
}
