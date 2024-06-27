package ru.practicum.category;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.page.OffsetPage;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;

@RestController
@Slf4j
@Validated
public class CategoryController {
    private final CategoryService categoryService;
    private final Sort sort = Sort.by("id").descending();


    @Autowired
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping("/admin/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto add(@Valid @RequestBody NewCategoryDto newCategoryDto) {
        log.info("Получен запрос на создание новой категории {}", newCategoryDto);
        CategoryDto categoryDto = categoryService.add(newCategoryDto);
        log.info("Создана категория {}", categoryDto);
        return categoryDto;
    }

    @DeleteMapping("/admin/categories/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long catId) {
        log.info("Получен запрос на удаление категории с id {}", catId);
        categoryService.delete(catId);
        log.info("Удалена категория с id {}", catId);
    }

    @PatchMapping("/admin/categories/{catId}")
    public CategoryDto patch(@Valid @RequestBody CategoryDto categoryDto, @PathVariable long catId) {
        log.info("Получен запрос на изменение категории с id {}, данные для изменения {}", catId, categoryDto);
        CategoryDto patchedCategory = categoryService.patch(catId, categoryDto);
        log.info("Изменена категория {}", patchedCategory);
        return patchedCategory;
    }

    @GetMapping("/categories")
    public List<CategoryDto> getAll(@RequestParam(defaultValue = "0") @Min(0) int from,
                                    @RequestParam(defaultValue = "10") @Min(1) int size) {
        log.info("Получен запрос на категории начиная с записи {}, количество записей {}", from, size);
        Pageable page = new OffsetPage(from, size, sort);
        List<CategoryDto> result = categoryService.getAll(page);
        log.info("Получен список категорий {}", result);
        return result;
    }

    @GetMapping("/categories/{catId}")
    public CategoryDto get(@PathVariable long catId) {
        log.info("Получен запрос на категорию с id {}", catId);
        CategoryDto categoryDto = categoryService.get(catId);
        log.info("Найдена категория {}", categoryDto);
        return categoryDto;
    }
}
