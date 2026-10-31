package com.maka.api.categoria.dto;

import com.maka.api.model.Categoria;
import com.maka.api.model.StatusCategoria;

public record CategoriaResponse(String id, String name, String status) {

    public static CategoriaResponse from(Categoria categoria) {
        return new CategoriaResponse(
                String.valueOf(categoria.getId()),
                categoria.getNome(),
                categoria.getStatus() == StatusCategoria.ATIVO ? "active" : "inactive");
    }
}
