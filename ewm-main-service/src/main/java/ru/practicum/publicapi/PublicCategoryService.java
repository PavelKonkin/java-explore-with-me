package ru.practicum.publicapi;

import org.springframework.data.domain.Pageable;
import ru.practicum.category.dto.CategoryDto;

import java.util.List;

public interface PublicCategoryService {
    List<CategoryDto> getAll(Pageable page);

    CategoryDto get(long catId);
}
