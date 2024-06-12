package ru.practicum.adminapi;

import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;

public interface AdminCategoryService {
    CategoryDto add(NewCategoryDto newCategoryDto);

    void delete(long catId);

    CategoryDto patch(long catId, CategoryDto categoryDto);
}