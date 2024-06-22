package ru.practicum.category;

import org.springframework.data.domain.Pageable;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto add(NewCategoryDto newCategoryDto);

    void delete(long catId);

    CategoryDto patch(long catId, CategoryDto categoryDto);

    List<CategoryDto> getAll(Pageable page);

    CategoryDto get(long catId);
}
