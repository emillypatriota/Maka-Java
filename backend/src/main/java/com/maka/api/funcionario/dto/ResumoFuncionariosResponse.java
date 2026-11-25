package com.maka.api.funcionario.dto;

import java.util.Map;

// Contadores do topo da tela de funcionários. byRole usa os cargos do front (manager, kitchen...)
public record ResumoFuncionariosResponse(long total, long active, Map<String, Long> byRole) {
}
