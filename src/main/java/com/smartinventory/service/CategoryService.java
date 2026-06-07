package com.smartinventory.service;

import com.smartinventory.dto.PageResponse;
import com.smartinventory.dto.category.CategoryRequest;
import com.smartinventory.dto.category.CategoryResponse;
import com.smartinventory.entity.Category;
import com.smartinventory.exception.DuplicateResourceException;
import com.smartinventory.exception.ResourceNotFoundException;
import com.smartinventory.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse create(CategoryRequest req) {
        if (categoryRepository.existsByNameIgnoreCase(req.name())) {
            throw new DuplicateResourceException("Category already exists: " + req.name());
        }
        Category category = Category.builder()
                .name(req.name())
                .description(req.description())
                .build();
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest req) {
        Category category = getEntity(id);
        categoryRepository.findByNameIgnoreCase(req.name())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new DuplicateResourceException("Category already exists: " + req.name()); });
        category.setName(req.name());
        category.setDescription(req.description());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        Category category = getEntity(id);
        categoryRepository.delete(category);
        log.info("Deleted category id={}", id);
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(Long id) {
        return CategoryResponse.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream().map(CategoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> search(String keyword, Pageable pageable) {
        String kw = keyword == null ? "" : keyword;
        return PageResponse.from(
                categoryRepository.findByNameContainingIgnoreCase(kw, pageable),
                CategoryResponse::from);
    }

    public Category getEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
    }
}
