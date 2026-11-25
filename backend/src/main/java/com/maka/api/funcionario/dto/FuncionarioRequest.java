package com.maka.api.funcionario.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.maka.api.model.Cargo;
import com.maka.api.model.StatusFuncionario;

// Mesmos nomes do StaffMember do front (frontend/src/app/services/staff.service.ts)
public record FuncionarioRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Cargo role,
        @Size(max = 20) String phone,
        @Email @Size(max = 120) String email,
        StatusFuncionario status,
        @Size(max = 60) String schedule,
        LocalDate hireDate,
        @Size(max = 255) String notes) {
}
