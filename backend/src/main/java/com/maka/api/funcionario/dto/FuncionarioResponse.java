package com.maka.api.funcionario.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.maka.api.model.Cargo;
import com.maka.api.model.Funcionario;
import com.maka.api.model.StatusFuncionario;

import lombok.Getter;
import lombok.Setter;

// Espelha o StaffMember do front (frontend/src/app/services/staff.service.ts)
@Getter
@Setter
public class FuncionarioResponse {

    private String id;
    private String name;
    private Cargo role;
    private String phone;
    private String email;
    private StatusFuncionario status;
    private String schedule;
    private LocalDate hireDate;
    private String notes;
    private LocalDateTime createdAt;

    public static FuncionarioResponse from(Funcionario funcionario) {
        FuncionarioResponse dto = new FuncionarioResponse();
        dto.setId(String.valueOf(funcionario.getId()));
        dto.setName(funcionario.getNome());
        dto.setRole(funcionario.getCargo());
        dto.setPhone(funcionario.getTelefone());
        dto.setEmail(funcionario.getEmail());
        dto.setStatus(funcionario.getStatus());
        dto.setSchedule(funcionario.getEscala());
        dto.setHireDate(funcionario.getDataAdmissao());
        dto.setNotes(funcionario.getObservacoes());
        dto.setCreatedAt(funcionario.getCriadoEm());
        return dto;
    }
}
