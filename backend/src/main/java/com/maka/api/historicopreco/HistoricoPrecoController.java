package com.maka.api.historicopreco;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maka.api.historicopreco.dto.HistoricoPrecoResponse;

@RestController
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@RequestMapping("/api/admin/ingredientes")
public class HistoricoPrecoController {

    private final HistoricoPrecoService historicoPrecoService;

    public HistoricoPrecoController(HistoricoPrecoService historicoPrecoService) {
        this.historicoPrecoService = historicoPrecoService;
    }

    // RF-026: evolução do preço de um ingrediente por fornecedor (série para o gráfico)
    @GetMapping("/{id}/historico-preco")
    public List<HistoricoPrecoResponse> listar(
            @PathVariable Long id,
            @RequestParam(required = false) Long fornecedorId) {
        return historicoPrecoService.listar(id, fornecedorId);
    }
}
