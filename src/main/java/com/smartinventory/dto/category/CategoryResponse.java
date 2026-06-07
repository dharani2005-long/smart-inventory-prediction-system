package com.smartinventory.dto.category;

import com.smartinventory.entity.Category;
import lombok.Builder;

@Builder
public record CategoryResponse(Long id, String name, String description) {

    public static CategoryResponse from(Category c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .build();
    }
}
