package com.smartinventory.dto.supplier;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplierRequest(

        @NotBlank(message = "Supplier name is required")
        @Size(max = 120)
        String name,

        @Size(max = 100)
        String contactPerson,

        @Email(message = "Email must be valid")
        @Size(max = 120)
        String email,

        @Size(max = 20)
        String phone,

        @Size(max = 255)
        String address
) {}
