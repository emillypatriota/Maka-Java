package com.maka.api.insights.controller;

import com.maka.api.insights.dto.InsightsResumoResponseDTO;
import com.maka.api.insights.service.InsightsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insights")
@Tag(name = "Insights", description = "Endpoints de relatórios e métricas de desempenho")
public class InsightsController {

    private final InsightsService insightsService;

    public InsightsController(InsightsService insightsService) {
        this.insightsService = insightsService;
    }

    @GetMapping("/resumo")
    @Operation(summary = "Obter resumo estatístico e financeiro para o painel de insights")
    public ResponseEntity<InsightsResumoResponseDTO> obterResumo() {
        InsightsResumoResponseDTO resumo = insightsService.obterResumo();
        return ResponseEntity.ok(resumo);
    }
}
