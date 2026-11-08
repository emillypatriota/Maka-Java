package com.maka.api.estoque;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maka.api.exception.RegraNegocioException;
import com.maka.api.estoque.dto.MovimentacaoRequest;
import com.maka.api.estoque.dto.MovimentacaoResponse;
import com.maka.api.estoque.dto.SaldoEstoqueResponse;
import com.maka.api.estoque.dto.SaldoEstoqueResumoResponse;
import com.maka.api.model.EstoqueMovimentacao;
import com.maka.api.model.Ingrediente;
import com.maka.api.model.MotivoMovimentacao;
import com.maka.api.model.StatusIngrediente;
import com.maka.api.model.TipoMovimentacao;
import com.maka.api.repository.EstoqueMovimentacaoRepository;
import com.maka.api.repository.IngredienteRepository;
import com.maka.api.repository.UsuarioRepository;

@Service
public class EstoqueService {

    private static final Set<MotivoMovimentacao> MOTIVOS_SAIDA_MANUAL = Set.of(
            MotivoMovimentacao.DESPERDICIO,
            MotivoMovimentacao.VENCIMENTO,
            MotivoMovimentacao.QUEBRA,
            MotivoMovimentacao.USO_INTERNO);

    private final IngredienteRepository ingredienteRepository;
    private final EstoqueMovimentacaoRepository movimentacaoRepository;
    private final UsuarioRepository usuarioRepository;

    public EstoqueService(
            IngredienteRepository ingredienteRepository,
            EstoqueMovimentacaoRepository movimentacaoRepository,
            UsuarioRepository usuarioRepository) {
        this.ingredienteRepository = ingredienteRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public SaldoEstoqueResumoResponse saldos() {
        List<SaldoEstoqueResponse> itens = movimentacaoRepository.listarSaldosAtivos().stream()
                .map(SaldoEstoqueResponse::from)
                .toList();
        long alertas = itens.stream().filter(SaldoEstoqueResponse::abaixoDoMinimo).count();
        return new SaldoEstoqueResumoResponse(itens, alertas);
    }

    @Transactional(readOnly = true)
    public Page<MovimentacaoResponse> movimentacoes(Long ingredienteId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<EstoqueMovimentacao> movimentacoes = ingredienteId == null
                ? movimentacaoRepository.findAll(pageable)
                : movimentacaoRepository.findByIngredienteId(ingredienteId, pageable);
        return movimentacoes.map(MovimentacaoResponse::from);
    }

    @Transactional
    public MovimentacaoResponse entrada(MovimentacaoRequest request, String emailUsuario) {
        Ingrediente ingrediente = buscarBloqueado(request.ingredienteId());
        validarIngredienteAtivo(ingrediente);
        EstoqueMovimentacao movimentacao = novaMovimentacao(
                ingrediente, TipoMovimentacao.ENTRADA, MotivoMovimentacao.AJUSTE, request.quantidade(), emailUsuario);
        return MovimentacaoResponse.from(movimentacaoRepository.save(movimentacao));
    }

    @Transactional
    public MovimentacaoResponse saida(MovimentacaoRequest request, String emailUsuario) {
        Ingrediente ingrediente = buscarBloqueado(request.ingredienteId());
        validarIngredienteAtivo(ingrediente);
        if (request.motivo() == null || !MOTIVOS_SAIDA_MANUAL.contains(request.motivo())) {
            throw new RegraNegocioException("Informe um motivo válido para a saída manual.");
        }

        BigDecimal saldoAtual = movimentacaoRepository.calcularSaldo(ingrediente.getId());
        if (saldoAtual.compareTo(request.quantidade()) < 0) {
            throw new RegraNegocioException("Estoque insuficiente para realizar esta saída.");
        }

        EstoqueMovimentacao movimentacao = novaMovimentacao(
                ingrediente, TipoMovimentacao.SAIDA, request.motivo(), request.quantidade(), emailUsuario);
        return MovimentacaoResponse.from(movimentacaoRepository.save(movimentacao));
    }

    private Ingrediente buscarBloqueado(Long ingredienteId) {
        return ingredienteRepository.findByIdForUpdate(ingredienteId)
                .orElseThrow(() -> new NoSuchElementException("Ingrediente não encontrado: " + ingredienteId));
    }

    private void validarIngredienteAtivo(Ingrediente ingrediente) {
        if (ingrediente.getStatus() != StatusIngrediente.ATIVO) {
            throw new RegraNegocioException("Não é possível movimentar estoque de ingrediente inativo.");
        }
    }

    private EstoqueMovimentacao novaMovimentacao(
            Ingrediente ingrediente,
            TipoMovimentacao tipo,
            MotivoMovimentacao motivo,
            BigDecimal quantidade,
            String emailUsuario) {
        EstoqueMovimentacao movimentacao = new EstoqueMovimentacao();
        movimentacao.setIngrediente(ingrediente);
        movimentacao.setTipo(tipo);
        movimentacao.setMotivo(motivo);
        movimentacao.setQuantidade(quantidade);
        movimentacao.setCreatedAt(LocalDateTime.now());
        usuarioRepository.findByEmail(emailUsuario).ifPresent(usuario -> movimentacao.setUsuarioId(usuario.getId()));
        return movimentacao;
    }
}
