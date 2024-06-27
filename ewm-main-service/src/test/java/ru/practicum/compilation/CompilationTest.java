package ru.practicum.compilation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryRepository;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.event.*;
import ru.practicum.exception.NotFoundException;
import ru.practicum.page.OffsetPage;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
@SpringBootTest
public class CompilationTest {
    @Autowired
    private CompilationRepository compilationRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private CompilationService compilationService;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LocationRepository locationRepository;

    private NewCompilationDto newCompilationDto;
    private Event event;
    private Category category;
    private User user;
    private Location location;
    private Compilation existentCompilation;
    private UpdateCompilationRequest updateCompilationRequest;


    private Compilation existentCompilationPub;
    private final Sort sort = Sort.by("id").descending();
    private final Pageable page = new OffsetPage(0, 10, sort);

    @BeforeEach
    public void setup() {
        category = Category.builder()
                .name("test")
                .build();
        category = categoryRepository.save(category);
        user = User.builder()
                .email("email@user.test")
                .name("test")
                .build();
        user = userRepository.save(user);
        location = Location.builder()
                .lat(22.22f)
                .lon(33.33f)
                .build();
        location = locationRepository.save(location);
        event = Event.builder()
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
        newCompilationDto = NewCompilationDto.builder()
                .events(List.of(event.getId()))
                .title("test comp")
                .pinned(false)
                .build();
        existentCompilation = Compilation.builder()
                .title("existent")
                .pinned(false)
                .build();
        existentCompilation = compilationRepository.save(existentCompilation);
        updateCompilationRequest = UpdateCompilationRequest.builder()
                .title("updated title")
                .pinned(true)
                .events(List.of())
                .build();


        Category categoryPub = Category.builder()
                .name("testPub")
                .build();
        categoryPub = categoryRepository.save(categoryPub);
        User userPub = User.builder()
                .email("email@userPub.test")
                .name("test")
                .build();
        userPub = userRepository.save(userPub);
        Location locationPub = Location.builder()
                .lat(22.22f)
                .lon(33.33f)
                .build();
        locationPub = locationRepository.save(locationPub);
        Event eventPub = Event.builder()
                .state(EventState.PUBLISHED)
                .category(categoryPub)
                .initiator(userPub)
                .location(locationPub)
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
        eventPub = eventRepository.save(eventPub);

        existentCompilationPub = Compilation.builder()
                .title("existentPub")
                .pinned(false)
                .events(Set.of(eventPub))
                .build();
        existentCompilationPub = compilationRepository.save(existentCompilationPub);
    }

    @Test
    public void add_whenSuccessful_thenReturnCompilationDto() {
        CompilationDto actualCompilationDto = compilationService.add(newCompilationDto);

        assertThat(actualCompilationDto.getClass(), is(CompilationDto.class));
        assertThat(actualCompilationDto, notNullValue());
        assertThat(actualCompilationDto.getId(), notNullValue());
        assertThat(actualCompilationDto.getTitle(), is(newCompilationDto.getTitle()));
    }

    @Test
    public void add_whenCompilationTitleAlreadyExists_thenThrownException() {
        newCompilationDto.setTitle(existentCompilation.getTitle());

        assertThrows(DataIntegrityViolationException.class,
                () -> compilationService.add(newCompilationDto));
    }

    @Test
    public void delete_whenSuccessful_thenDoNothing() {
        long id = existentCompilation.getId();

        assertDoesNotThrow(() -> compilationService.delete(id));

        assertThat(compilationRepository.findById(id), is(Optional.empty()));
    }

    @Test
    public void delete_whenCompilationNotFound_thenThrownException() {
        long wrongId = 666L;

        assertThrows(NotFoundException.class, () -> compilationService.delete(wrongId));
    }

    @Test
    public void patch_whenSuccessful_thenReturnUpdatedCompilationDto() {
        CompilationDto actualUpdatedCompilationDto = compilationService
                .patch(existentCompilation.getId(), updateCompilationRequest);

        assertThat(actualUpdatedCompilationDto.getClass(), is(CompilationDto.class));
        assertThat(actualUpdatedCompilationDto, notNullValue());
        assertThat(actualUpdatedCompilationDto.getId(), notNullValue());
        assertThat(actualUpdatedCompilationDto.getTitle(), is(updateCompilationRequest.getTitle()));
        assertThat(actualUpdatedCompilationDto.getEvents().size(), is(0));
    }

    @Test
    public void patch_whenCompilationNotFound_thenThrownException() {
        long wrongId = 666L;

        assertThrows(NotFoundException.class,
                () -> compilationService.patch(wrongId, updateCompilationRequest));
    }


    @Test
    public void pub_getAll_whenSuccessful_thenReturnListOfCompilationDto() {
        List<CompilationDto> actualListOfCompilationDto = compilationService.getAll(false, page);

        assertThat(actualListOfCompilationDto.get(0).getClass(), is(CompilationDto.class));
        assertThat(actualListOfCompilationDto, notNullValue());
        assertThat(actualListOfCompilationDto.get(0).getId(), notNullValue());
        assertThat(actualListOfCompilationDto.get(0).getTitle(), is(existentCompilationPub.getTitle()));
    }

    @Test
    public void pub_getAll_whenNoCompilationFound_thenReturnEmptyList() {
        List<CompilationDto> actualListOfCompilationDto = compilationService.getAll(true, page);

        assertThat(actualListOfCompilationDto.isEmpty(), is(true));
        assertThat(actualListOfCompilationDto, notNullValue());
    }

    @Test
    public void pub_get_whenSuccessful_thenReturnCompilationDto() {
        CompilationDto actualCompilationDto = compilationService.get(existentCompilationPub.getId());

        assertThat(actualCompilationDto.getClass(), is(CompilationDto.class));
        assertThat(actualCompilationDto, notNullValue());
        assertThat(actualCompilationDto.getId(), is(existentCompilationPub.getId()));
        assertThat(actualCompilationDto.getTitle(), is(existentCompilationPub.getTitle()));
    }

    @Test
    public void pub_get_whenCompilationNotFound_thenThrownException() {
        long wrongId = 66L;

        NotFoundException exception = assertThrows(NotFoundException.class, () -> compilationService.get(wrongId));

        assertThat(exception.getMessage(), is("Compilation with id=" + wrongId + " was not found"));
    }

}
