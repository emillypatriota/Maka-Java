package com.maka.api.fichatecnica;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maka.api.fichatecnica.dto.FichaTecnicaRequest;
import com.maka.api.fichatecnica.dto.FichaTecnicaResponse;

// Ficha técnica de um prato (MANAGER, pela regra /api/admin/** do SecurityConfig)
@RestController
@RequestMapping("/api/admin/pratos/{produtoId}/ficha-tecnica")
public class FichaTecnicaController {

    private final FichaTecnicaService fichaTecnicaService;

    public FichaTecnicaController(FichaTecnicaService fichaTecnicaService) {
        this.fichaTecnicaService = fichaTecnicaService;
    }

    @GetMapping
    public FichaTecnicaResponse buscar(@PathVariable Long produtoId) {
        return fichaTecnicaService.buscar(produtoId);
    }

    @PutMapping
    public FichaTecnicaResponse salvar(@PathVariable Long produtoId, @Valid @RequestBody FichaTecnicaRequest request) {
        return fichaTecnicaService.salvar(produtoId, request);
    }
}
