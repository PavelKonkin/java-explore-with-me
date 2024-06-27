package ru.practicum.category;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.event.*;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.page.OffsetPage;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
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
    private Category category1;
    private Category category2;
    private CategoryDto categoryDto1;
    private CategoryDto categoryDto2;
    private final Sort sort = Sort.by("id").descending();
    private final int from = 0;
    private final int size = 10;
    private final Pageable page = new OffsetPage(from, size, sort);


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
        category1 = Category.builder()
                .name("test1")
                .build();
        category1 = categoryRepository.save(category1);
        categoryDto1 = categoryMapper.convertCategory(category1);
        category2 = Category.builder()
                .name("test2")
                .build();
        category2 = categoryRepository.save(category2);
        categoryDto2 = categoryMapper.convertCategory(category2);
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

    @Test
    public void getAll_whenCategoriesFound_thenReturnListOfCategoryDto() {
        List<CategoryDto> actualListOfCategoryDto = categoryService.getAll(page);

        assertThat(actualListOfCategoryDto.size(), is(4));
    }

    @Test
    public void getAll_whenCategoriesNotFound_thenReturnEmptyList() {
        Pageable emptyListPage = new OffsetPage(10, 1, sort);

        List<CategoryDto> actualListOfCategoryDto = categoryService.getAll(emptyListPage);

        assertThat(List.of(), is(actualListOfCategoryDto));
    }

    @Test
    public void get_whenCategoryFound_thenReturnCategoryDto() {
        CategoryDto actualCategoryDto = categoryService.get(category1.getId());

        assertThat(categoryDto1, is(actualCategoryDto));
    }

    @Test
    public void get_whenCategoryNotFound_thenThrownException() {
        long wrongId = 66L;

        NotFoundException exception = assertThrows(NotFoundException.class, () -> categoryService.get(wrongId));

        assertThat(exception.getMessage(), is("Category with id=" + wrongId + " was not found"));
    }
}
