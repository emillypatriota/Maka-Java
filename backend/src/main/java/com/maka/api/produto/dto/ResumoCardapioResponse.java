package com.maka.api.produto.dto;

// Contadores dos cards do topo da gestão do cardápio
public record ResumoCardapioResponse(long total, long active, long paused, long inactive) {
}
