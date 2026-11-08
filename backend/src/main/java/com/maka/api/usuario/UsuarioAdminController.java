package com.maka.api.usuario;

import java.net.URI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.maka.api.usuario.dto.UsuarioCreateRequest;
import com.maka.api.usuario.dto.UsuarioResponse;
import com.maka.api.usuario.dto.UsuarioStatusRequest;
import com.maka.api.usuario.dto.UsuarioUpdateRequest;

@RestController
@Validated
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioAdminController {

    private final UsuarioService usuarioService;

    public UsuarioAdminController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public Page<UsuarioResponse> listar(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return usuarioService.listar(PageRequest.of(page, size, Sort.by("nome").ascending()));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioCreateRequest request) {
        UsuarioResponse criado = usuarioService.criar(request);
        return ResponseEntity.created(URI.create("/api/admin/usuarios/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioUpdateRequest request) {
        return usuarioService.atualizar(id, request);
    }

    @PatchMapping("/{id}/status")
    public UsuarioResponse alterarStatus(@PathVariable Long id,
                                         @Valid @RequestBody UsuarioStatusRequest request,
                                         Authentication authentication) {
        return usuarioService.alterarStatus(id, request.status(), authentication.getName());
    }

    /** Exclusão lógica (RN06): marca INATIVO. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, Authentication authentication) {
        usuarioService.alterarStatus(id, com.maka.api.model.StatusUsuario.INATIVO, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
