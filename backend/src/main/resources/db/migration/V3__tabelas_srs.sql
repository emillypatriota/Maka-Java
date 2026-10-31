-- V3: tabelas e colunas novas pedidas pelo SRS do professor (Comanda Digital v3.2).
-- Só adiciona coisas: nada do que já existe é renomeado ou removido,
-- então o código dos outros times continua funcionando.
-- Saldo de estoque de um ingrediente = soma das ENTRADA e ESTORNO - soma das SAIDA.

-- Perfis novos: ADMIN e CLIENTE (MANAGER = gerente, KITCHEN = cozinheiro).
ALTER TABLE usuario
  MODIFY COLUMN perfil ENUM('MANAGER','KITCHEN','DELIVERY','ADMIN','CLIENTE') NOT NULL,
  ADD COLUMN telefone      VARCHAR(20)  NULL,
  ADD COLUMN endereco      VARCHAR(255) NULL,
  ADD COLUMN status        ENUM('ATIVO','INATIVO') NOT NULL DEFAULT 'ATIVO',
  ADD COLUMN atualizado_em DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

CREATE TABLE categoria (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome       VARCHAR(60) NOT NULL UNIQUE,
  descricao  VARCHAR(255) NULL,
  ordem      INT NOT NULL DEFAULT 0,
  status     ENUM('ATIVO','INATIVO') NOT NULL DEFAULT 'ATIVO',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- produto = "prato" do SRS. Mantemos o nome da tabela e as colunas antigas
-- (categoria texto, ativo) até a Gestão migrar para categoria_id e status.
ALTER TABLE produto
  ADD COLUMN categoria_id BIGINT NULL,
  ADD COLUMN status ENUM('ATIVO','INATIVO','PAUSADO') NOT NULL DEFAULT 'ATIVO',
  ADD CONSTRAINT fk_produto_categoria FOREIGN KEY (categoria_id) REFERENCES categoria(id);

-- ingrediente (criada pela Insights na V1) ganha as colunas do SRS.
-- unidade_medida guarda g, ml, un, kg ou L.
ALTER TABLE ingrediente
  ADD COLUMN sku            VARCHAR(40) NULL,
  ADD COLUMN estoque_minimo DECIMAL(12,3) NOT NULL DEFAULT 0,
  ADD COLUMN status         ENUM('ATIVO','INATIVO') NOT NULL DEFAULT 'ATIVO',
  ADD COLUMN created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD CONSTRAINT uk_ingrediente_sku UNIQUE (sku);

CREATE TABLE ficha_tecnica (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  produto_id   BIGINT NOT NULL UNIQUE,
  rendimento   INT NOT NULL DEFAULT 1,
  modo_preparo TEXT NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (produto_id) REFERENCES produto(id)
);

CREATE TABLE ficha_tecnica_item (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  ficha_tecnica_id BIGINT NOT NULL,
  ingrediente_id   BIGINT NOT NULL,
  quantidade       DECIMAL(12,3) NOT NULL,
  unidade          VARCHAR(10) NOT NULL,
  fator_correcao   DECIMAL(5,2) NOT NULL DEFAULT 1.00,
  FOREIGN KEY (ficha_tecnica_id) REFERENCES ficha_tecnica(id) ON DELETE CASCADE,
  FOREIGN KEY (ingrediente_id) REFERENCES ingrediente(id),
  CONSTRAINT ck_ficha_item_fator CHECK (fator_correcao >= 1.00)
);

CREATE TABLE fornecedor (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  razao_social VARCHAR(150) NOT NULL,
  cnpj         VARCHAR(18) NOT NULL UNIQUE,
  telefone     VARCHAR(20) NULL,
  email        VARCHAR(120) NULL,
  categorias   VARCHAR(255) NULL,
  status       ENUM('ATIVO','INATIVO') NOT NULL DEFAULT 'ATIVO',
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE fornecedor_produto (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  fornecedor_id  BIGINT NOT NULL,
  ingrediente_id BIGINT NOT NULL,
  preco          DECIMAL(10,4) NOT NULL,
  unidade_venda  VARCHAR(10) NOT NULL,
  UNIQUE KEY uk_fornecedor_ingrediente (fornecedor_id, ingrediente_id),
  FOREIGN KEY (fornecedor_id) REFERENCES fornecedor(id),
  FOREIGN KEY (ingrediente_id) REFERENCES ingrediente(id)
);

-- Histórico de preço por fornecedor e ingrediente (RF-026, gráfico de evolução).
CREATE TABLE historico_preco (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  fornecedor_id  BIGINT NOT NULL,
  ingrediente_id BIGINT NOT NULL,
  preco          DECIMAL(10,4) NOT NULL,
  registrado_em  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (fornecedor_id) REFERENCES fornecedor(id),
  FOREIGN KEY (ingrediente_id) REFERENCES ingrediente(id),
  INDEX idx_historico_ing (ingrediente_id, registrado_em)
);

CREATE TABLE pedido_compra (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  fornecedor_id BIGINT NOT NULL,
  status        ENUM('RASCUNHO','ENVIADO','RECEBIDO','CANCELADO') NOT NULL DEFAULT 'RASCUNHO',
  valor_total   DECIMAL(12,2) NOT NULL DEFAULT 0,
  usuario_id    BIGINT NOT NULL,
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (fornecedor_id) REFERENCES fornecedor(id),
  FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE TABLE pedido_compra_item (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  pedido_compra_id BIGINT NOT NULL,
  ingrediente_id   BIGINT NOT NULL,
  quantidade       DECIMAL(12,3) NOT NULL,
  preco_unitario   DECIMAL(10,4) NOT NULL,
  subtotal         DECIMAL(12,2) NOT NULL,
  FOREIGN KEY (pedido_compra_id) REFERENCES pedido_compra(id) ON DELETE CASCADE,
  FOREIGN KEY (ingrediente_id) REFERENCES ingrediente(id)
);

CREATE TABLE estoque_movimentacao (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  ingrediente_id   BIGINT NOT NULL,
  tipo             ENUM('ENTRADA','SAIDA','ESTORNO') NOT NULL,
  motivo           ENUM('COMPRA','VENDA','DESPERDICIO','VENCIMENTO','QUEBRA','USO_INTERNO','AJUSTE','ESTORNO') NOT NULL,
  quantidade       DECIMAL(12,3) NOT NULL,
  lote             VARCHAR(40) NULL,
  validade         DATE NULL,
  custo_unitario   DECIMAL(10,4) NULL,
  pedido_compra_id BIGINT NULL,
  pedido_id        BIGINT NULL,
  usuario_id       BIGINT NULL,
  created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (ingrediente_id) REFERENCES ingrediente(id),
  FOREIGN KEY (pedido_compra_id) REFERENCES pedido_compra(id),
  FOREIGN KEY (pedido_id) REFERENCES pedido(id),
  FOREIGN KEY (usuario_id) REFERENCES usuario(id),
  INDEX idx_mov_ingrediente (ingrediente_id),
  INDEX idx_mov_created (created_at)
);

-- Pedido feito por cliente logado (null = pedido antigo, sem login).
ALTER TABLE pedido
  ADD COLUMN cliente_id BIGINT NULL,
  ADD CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES usuario(id),
  ADD INDEX idx_pedido_cliente (cliente_id);
