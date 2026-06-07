package com.smartinventory.dto.supplier;

import com.smartinventory.entity.Supplier;
import lombok.Builder;

@Builder
public record SupplierResponse(
        Long id,
        String name,
        String contactPerson,
        String email,
        String phone,
        String address,
        boolean active
) {
    public static SupplierResponse from(Supplier s) {
        return SupplierResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .contactPerson(s.getContactPerson())
                .email(s.getEmail())
                .phone(s.getPhone())
                .address(s.getAddress())
                .active(s.isActive())
                .build();
    }
}
