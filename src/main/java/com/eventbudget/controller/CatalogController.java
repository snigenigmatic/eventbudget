package com.eventbudget.controller;

import com.eventbudget.dto.ExpenseCategoryResponse;
import com.eventbudget.service.CatalogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/expense-categories")
    public List<ExpenseCategoryResponse> getExpenseCategories() {
        return catalogService.getExpenseCategories();
    }
}
