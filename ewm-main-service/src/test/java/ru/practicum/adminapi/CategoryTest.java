package ru.practicum.adminapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryMapper;
import ru.practicum.category.CategoryRepository;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.event.*;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import javax.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
@SpringBootTest
public class CategoryTest {
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private UserRepository userRepository;

    private NewCategoryDto newCategoryDto;
    private NewCategoryDto existentNewCategoryDto;
    private Category category;
    private Category categoryWithEvent;


    @BeforeEach
    public void setup() {
        newCategoryDto = NewCategoryDto.builder()
                .name("new name")
                .build();
        existentNewCategoryDto = NewCategoryDto.builder()
                .name("existent")
                .build();
        category = Category.builder()
                .name(existentNewCategoryDto.getName())
                .build();
        category = categoryRepository.save(category);
        categoryWithEvent = Category.builder()
                .name("category with event")
                .build();
        categoryWithEvent = categoryRepository.save(categoryWithEvent);
        Location location = Location.builder()
                .lat(22.22f)
                .lon(33.33f)
                .build();
        location = locationRepository.save(location);
        User user = User.builder()
                .name("test user")
                .email("test@email.user")
                .build();
        user = userRepository.save(user);
        Event event = Event.builder()
                .state(EventState.PENDING)
                .category(categoryWithEvent)
                .location(location)
                .paid(false)
                .participantLimit(0)
                .initiator(user)
                .requestModeration(false)
                .title("test")
                .description("test")
                .annotation("test")
                .createdOn(LocalDateTime.now())
                .eventDate(LocalDateTime.now().plusDays(1))
                .publishedOn(LocalDateTime.now().plusHours(2))
                .build();
        eventRepository.save(event);
    }

    @Test
    public void add_whenSuccessful_thenReturnCategoryDto() {
        CategoryDto actualCategoryDto = categoryService.add(newCategoryDto);

        assertThat(actualCategoryDto.getClass(), is(CategoryDto.class));
        assertThat(actualCategoryDto, notNullValue());
        assertThat(actualCategoryDto.getId(), notNullValue());
        assertThat(actualCategoryDto.getName(), is(newCategoryDto.getName()));
    }

    @Test
    public void add_whenCategoryNameAlreadyExists_thenThrownException() {
        assertThrows(DataIntegrityViolationException.class,
                () -> categoryService.add(existentNewCategoryDto));
    }

    @Test
    public void delete_whenSuccessful_thenDoNothing() {
        long id = category.getId();

        assertDoesNotThrow(() -> categoryService.delete(id));

        assertThat(categoryRepository.findById(id), is(Optional.empty()));
    }

    @Test
    public void delete_whenCategoryNotFound_thenThrownException() {
        long wrongId = 666L;

        assertThrows(NotFoundException.class, () -> categoryService.delete(wrongId));
    }

    @Test
    public void delete_whenThereIsEventInCategory_thenThrownException() {
        long id = categoryWithEvent.getId();

        assertThrows(ConflictException.class, () -> categoryService.delete(id));
    }

    @Test
    public void patch_whenSuccessful_thenReturnCategoryDto() {
        long id = categoryWithEvent.getId();
        CategoryDto updateDto = categoryMapper.convertCategory(categoryWithEvent.toBuilder()
                .name("updated name")
                .build());

        CategoryDto actualUpdatedCategoryDto = categoryService.patch(id, updateDto);

        assertThat(updateDto.getName(), is(actualUpdatedCategoryDto.getName()));
        assertThat(updateDto, is(actualUpdatedCategoryDto));
    }

    @Test
    public void patch_whenCategoryNotFound_thenThrownException() {
        long wrongId = 666;
        CategoryDto updateDto = categoryMapper.convertCategory(categoryWithEvent.toBuilder()
                .name("updated name")
                .build());

        assertThrows(NotFoundException.class, () -> categoryService.patch(wrongId, updateDto));
    }

    @Test
    public void patch_whenCategoryNameAlreadyExist_thenThrownException() {
        long id = category.getId();
        CategoryDto updateDto = categoryMapper.convertCategory(category.toBuilder()
                .name(categoryWithEvent.getName())
                .build());

        assertThrows(DataIntegrityViolationException.class, () -> {
            categoryService.patch(id, updateDto);
            categoryRepository.flush();
        });
    }
}
