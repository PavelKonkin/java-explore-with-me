package ru.practicum.publicapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryRepository;
import ru.practicum.compilation.Compilation;
import ru.practicum.compilation.CompilationRepository;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.event.*;
import ru.practicum.exception.NotFoundException;
import ru.practicum.page.OffsetPage;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
@SpringBootTest
public class CompilationTest {
    @Autowired
    private CompilationRepository compilationRepository;
    @Autowired
    @Qualifier(value = "publicCompilationService")
    private CompilationService compilationService;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private EventRepository eventRepository;

    private Compilation existentCompilation;
    private final Sort sort = Sort.by("id").descending();
    private final Pageable page = new OffsetPage(0, 10, sort);

    @BeforeEach
    public void setup() {
        Category category = Category.builder()
                .name("test")
                .build();
        category = categoryRepository.save(category);
        User user = User.builder()
                .email("email@user.test")
                .name("test")
                .build();
        user = userRepository.save(user);
        Location location = Location.builder()
                .lat(22.22f)
                .lon(33.33f)
                .build();
        location = locationRepository.save(location);
        Event event = Event.builder()
                .state(EventState.PUBLISHED)
                .category(category)
                .initiator(user)
                .location(location)
                .paid(false)
                .participantLimit(0)
                .requestModeration(false)
                .title("test")
                .description("description")
                .annotation("annotation")
                .createdOn(LocalDateTime.now())
                .publishedOn(LocalDateTime.now().plusHours(1))
                .eventDate(LocalDateTime.now().plusDays(1))
                .build();
        event = eventRepository.save(event);

        existentCompilation = Compilation.builder()
                .title("existent")
                .pinned(false)
                .events(Set.of(event))
                .build();
        existentCompilation = compilationRepository.save(existentCompilation);
    }

    @Test
    public void getAll_whenSuccessful_thenReturnListOfCompilationDto() {
        List<CompilationDto> actualListOfCompilationDto = compilationService.getAll(false, page);

        assertThat(actualListOfCompilationDto.get(0).getClass(), is(CompilationDto.class));
        assertThat(actualListOfCompilationDto, notNullValue());
        assertThat(actualListOfCompilationDto.get(0).getId(), notNullValue());
        assertThat(actualListOfCompilationDto.get(0).getTitle(), is(existentCompilation.getTitle()));
    }

    @Test
    public void getAll_whenNoCompilationFound_thenReturnEmptyList() {
        List<CompilationDto> actualListOfCompilationDto = compilationService.getAll(true, page);

        assertThat(actualListOfCompilationDto.isEmpty(), is(true));
        assertThat(actualListOfCompilationDto, notNullValue());
    }

    @Test
    public void get_whenSuccessful_thenReturnCompilationDto() {
        CompilationDto actualCompilationDto = compilationService.get(existentCompilation.getId());

        assertThat(actualCompilationDto.getClass(), is(CompilationDto.class));
        assertThat(actualCompilationDto, notNullValue());
        assertThat(actualCompilationDto.getId(), is(existentCompilation.getId()));
        assertThat(actualCompilationDto.getTitle(), is(existentCompilation.getTitle()));
    }

    @Test
    public void get_whenCompilationNotFound_thenThrownException() {
        long wrongId = 66L;

        NotFoundException exception = assertThrows(NotFoundException.class, () -> compilationService.get(wrongId));

        assertThat(exception.getMessage(), is("Compilation with id=" + wrongId + " was not found"));
    }

}
