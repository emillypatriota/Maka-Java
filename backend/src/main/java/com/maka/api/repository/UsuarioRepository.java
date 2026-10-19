package com.maka.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maka.api.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);
}