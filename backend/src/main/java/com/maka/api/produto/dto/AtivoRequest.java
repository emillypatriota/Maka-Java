package com.maka.api.produto.dto;

import jakarta.validation.constraints.NotNull;

public record AtivoRequest(@NotNull Boolean active) {
}
