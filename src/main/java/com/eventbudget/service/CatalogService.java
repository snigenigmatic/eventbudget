package com.eventbudget.service;

import com.eventbudget.dto.ExpenseCategoryResponse;
import com.eventbudget.repository.ExpenseCategoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final ExpenseCategoryRepository expenseCategoryRepository;

    @Transactional(readOnly = true)
    public List<ExpenseCategoryResponse> getExpenseCategories() {
        return expenseCategoryRepository.findAll().stream()
                .map(ExpenseCategoryResponse::from)
                .toList();
    }
}
