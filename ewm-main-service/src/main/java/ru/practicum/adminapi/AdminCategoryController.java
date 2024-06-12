package ru.practicum.adminapi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;

import javax.validation.Valid;

@RestController
@RequestMapping("/admin/categories")
@Slf4j
@Validated
public class AdminCategoryController {
    private final AdminCategoryService adminCategoryService;

    @Autowired
    public AdminCategoryController(AdminCategoryService adminCategoryService) {
        this.adminCategoryService = adminCategoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto add(@Valid @RequestBody NewCategoryDto newCategoryDto) {
        log.info("Получен запрос на создание новой категории {}", newCategoryDto);
        CategoryDto categoryDto = adminCategoryService.add(newCategoryDto);
        log.info("Создана категория {}", categoryDto);
        return categoryDto;
    }

    @DeleteMapping("/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long catId) {
        log.info("Получен запрос на удаление категории с id {}", catId);
        adminCategoryService.delete(catId);
        log.info("Удалена категория с id {}", catId);
    }

    @PatchMapping("/{catId}")
    public CategoryDto patch(@Valid @RequestBody CategoryDto categoryDto, @PathVariable long catId) {
        log.info("Получен запрос на изменение категории с id {}, данные для изменения {}", catId, categoryDto);
        CategoryDto patchedCategory = adminCategoryService.patch(catId, categoryDto);
        log.info("Изменена категория {}", patchedCategory);
        return patchedCategory;
    }
}
