package com.maka.api.produto.dto;

import java.math.BigDecimal;

import com.maka.api.fichatecnica.CustoPratoService.CustoPrato;
import com.maka.api.fichatecnica.dto.FaixaJson;
import com.maka.api.model.Produto;

import lombok.Getter;
import lombok.Setter;

// Prato na gestão: os dados públicos mais o custo da ficha técnica (RF-012, RF-013, RN02).
// Os campos de custo ficam null quando o prato ainda não tem ficha.
@Getter
@Setter
public class AdminProdutoResponse extends ProdutoResponse {

    private Boolean hasTechnicalSheet;
    private BigDecimal cost;
    private BigDecimal foodCost;
    private String foodCostLevel;
    private Boolean foodCostWarning;

    public static AdminProdutoResponse from(Produto produto, CustoPrato custo) {
        AdminProdutoResponse dto = new AdminProdutoResponse();
        preencher(dto, produto);
        dto.setHasTechnicalSheet(custo != null);
        if (custo != null) {
            dto.setCost(custo.custo());
            dto.setFoodCost(custo.foodCost());
            dto.setFoodCostLevel(FaixaJson.toJson(custo.faixa()));
            dto.setFoodCostWarning(custo.alerta());
        }
        return dto;
    }
}
