package ru.practicum.publicapi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.page.OffsetPage;

import javax.validation.constraints.Min;
import java.util.List;

@RestController
@RequestMapping("/categories")
@Slf4j
public class PublicCategoryController {
    private final PublicCategoryService categoryService;
    private final Sort sort = Sort.by("id").descending();

    public PublicCategoryController(PublicCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryDto> getAll(@RequestParam(defaultValue = "0") @Min(0) int from,
                                    @RequestParam(defaultValue = "10") @Min(1) int size) {
        log.info("Получен запрос на категории начиная с записи {}, количество записей {}", from, size);
        Pageable page = new OffsetPage(from, size, sort);
        List<CategoryDto> result = categoryService.getAll(page);
        log.info("Получен список категорий {}", result);
        return result;
    }

    @GetMapping("/{catId}")
    public CategoryDto get(@PathVariable long catId) {
        log.info("Получен запрос на категорию с id {}", catId);
        CategoryDto categoryDto = categoryService.get(catId);
        log.info("Найдена категория {}", categoryDto);
        return categoryDto;
    }

}
