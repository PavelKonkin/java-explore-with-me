package ru.practicum.adminapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryMapper;
import ru.practicum.category.CategoryRepository;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.event.Event;
import ru.practicum.event.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

import java.util.Optional;

@Service(value = "adminCategoryService")
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final EventRepository eventRepository;

    @Autowired
    public CategoryServiceImpl(CategoryRepository categoryRepository, CategoryMapper categoryMapper,
                               EventRepository eventRepository) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
        this.eventRepository = eventRepository;
    }

    @Override
    public CategoryDto add(NewCategoryDto newCategoryDto) {
        Category category = categoryMapper.convertNewCategoryDto(newCategoryDto);
        return categoryMapper.convertCategory(categoryRepository.save(category));
    }

    @Override
    public void delete(long catId) {
        Category category = categoryRepository.findById(catId).orElseThrow(() ->
                new NotFoundException("Category with id=" + catId + " was not found"));
        Optional<Event> eventFound = eventRepository.findFirstByCategoryId(catId);
        if (eventFound.isPresent()) {
            throw new ConflictException("The category is not empty");
        }
        categoryRepository.delete(category);
    }

    @Override
    public CategoryDto patch(long catId, CategoryDto categoryDto) {
        Category existentCategory = categoryRepository.findById(catId).orElseThrow(() ->
                new NotFoundException("Category with id=" + catId + " was not found"));
        Category updatedCategory = existentCategory.toBuilder()
                .name(categoryDto.getName())
                .build();
        return categoryMapper.convertCategory(categoryRepository.save(updatedCategory));
    }
}
