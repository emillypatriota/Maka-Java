package com.maka.api.categoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// status é opcional: sem ele, a categoria nova nasce ativa e a editada mantém o status
public record CategoriaRequest(
        @NotBlank @Size(max = 60) String name,
        @Pattern(regexp = "active|inactive", message = "use active ou inactive") String status) {
}
