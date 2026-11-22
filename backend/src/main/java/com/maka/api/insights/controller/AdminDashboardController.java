package com.maka.api.insights.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.maka.api.insights.dto.DashboardResumoResponseDTO;
import com.maka.api.insights.dto.TopPratoResponseDTO;
import com.maka.api.insights.service.InsightsService;

@RestController
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final InsightsService insightsService;

    public AdminDashboardController(InsightsService insightsService) {
        this.insightsService = insightsService;
    }

    @GetMapping("/resumo")
    public ResponseEntity<DashboardResumoResponseDTO> obterResumo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        try {
            return ResponseEntity.ok(insightsService.obterDashboardResumo(dataInicio, dataFim));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @GetMapping("/top-pratos")
    public ResponseEntity<List<TopPratoResponseDTO>> obterTopPratos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        try {
            return ResponseEntity.ok(insightsService.obterTopPratos(dataInicio, dataFim));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }
}
