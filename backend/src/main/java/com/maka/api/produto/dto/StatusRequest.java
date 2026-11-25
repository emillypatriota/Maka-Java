package com.maka.api.produto.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record StatusRequest(
        @NotNull @Pattern(regexp = StatusPratoJson.PADRAO, message = "use active, paused ou inactive") String status) {
}
