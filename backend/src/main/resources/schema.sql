CREATE DATABASE IF NOT EXISTS maka CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE maka;

CREATE TABLE IF NOT EXISTS usuario (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  email       VARCHAR(120) NOT NULL UNIQUE,
  senha_hash  VARCHAR(100) NOT NULL,
  nome        VARCHAR(100) NOT NULL,
  perfil      ENUM('MANAGER','KITCHEN','DELIVERY') NOT NULL,
  criado_em   DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS funcionario (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome           VARCHAR(100) NOT NULL,
  cargo          ENUM('manager','kitchen','delivery','cashier','waiter') NOT NULL,
  telefone       VARCHAR(20),
  email          VARCHAR(120),
  status         ENUM('active','inactive','on_leave') DEFAULT 'active',
  escala         VARCHAR(60),
  data_admissao  DATE,
  observacoes    VARCHAR(255),
  usuario_id     BIGINT NULL,
  criado_em      DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE TABLE IF NOT EXISTS categoria (
  id      BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome    VARCHAR(60) NOT NULL UNIQUE,
  status  ENUM('ATIVO','INATIVO') NOT NULL DEFAULT 'ATIVO'
);

-- status: RN01 (só fica ATIVO com ficha técnica) e RN06 (exclusão lógica = INATIVO)
CREATE TABLE IF NOT EXISTS produto (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome           VARCHAR(120) NOT NULL,
  descricao      TEXT,
  preco          DECIMAL(10,2) NOT NULL,
  categoria_id   BIGINT NOT NULL,
  imagem         VARCHAR(500),
  destaque       BOOLEAN DEFAULT FALSE,
  status         ENUM('ATIVO','INATIVO','PAUSADO') NOT NULL DEFAULT 'PAUSADO',
  tempo_preparo  INT,
  calorias       INT,
  modo_preparo   TEXT,
  criado_em      DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (categoria_id) REFERENCES categoria(id)
);

CREATE TABLE IF NOT EXISTS produto_tag (
  produto_id  BIGINT NOT NULL,
  tag         VARCHAR(40) NOT NULL,
  PRIMARY KEY (produto_id, tag),
  FOREIGN KEY (produto_id) REFERENCES produto(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS ingrediente (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome            VARCHAR(100) NOT NULL,
  unidade_medida  VARCHAR(20) NOT NULL,
  custo_unitario  DECIMAL(12,4) NOT NULL
);

-- Ficha técnica: ingredientes de cada prato. quantidade na unidade_medida do ingrediente;
-- fator_correcao >= 1,0 (RN08, validado no Service)
CREATE TABLE IF NOT EXISTS ficha_tecnica (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  produto_id      BIGINT NOT NULL,
  ingrediente_id  BIGINT NOT NULL,
  quantidade      DECIMAL(12,4) NOT NULL,
  fator_correcao  DECIMAL(6,3) NOT NULL DEFAULT 1.000,
  UNIQUE (produto_id, ingrediente_id),
  FOREIGN KEY (produto_id) REFERENCES produto(id),
  FOREIGN KEY (ingrediente_id) REFERENCES ingrediente(id)
);

CREATE TABLE IF NOT EXISTS pedido (
  id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
  cliente_nome         VARCHAR(100) NOT NULL,
  cliente_telefone     VARCHAR(20) NOT NULL,
  cliente_endereco     VARCHAR(255),
  cliente_email        VARCHAR(120),
  forma_pagamento      ENUM('credit','debit','pix','cash') NOT NULL,
  tipo                 ENUM('delivery','mesa','retirada') DEFAULT 'delivery',
  status               ENUM('pending','confirmed','preparing','ready',
                            'delivering','delivered','cancelled') DEFAULT 'pending',
  taxa_entrega         DECIMAL(10,2) DEFAULT 0,
  total                DECIMAL(10,2) NOT NULL,
  observacoes          VARCHAR(255),
  numero_mesa          INT,
  tempo_estimado       INT,
  motivo_cancelamento  VARCHAR(255),
  entregador_id        BIGINT NULL,
  criado_em            DATETIME DEFAULT CURRENT_TIMESTAMP,
  atualizado_em        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (entregador_id) REFERENCES usuario(id),
  INDEX idx_status (status),
  INDEX idx_criado (criado_em)
);

CREATE TABLE IF NOT EXISTS item_pedido (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  pedido_id       BIGINT NOT NULL,
  produto_id      BIGINT NOT NULL,
  quantidade      INT NOT NULL,
  preco_unitario  DECIMAL(10,2) NOT NULL,
  observacoes     VARCHAR(255),
  FOREIGN KEY (pedido_id) REFERENCES pedido(id) ON DELETE CASCADE,
  FOREIGN KEY (produto_id) REFERENCES produto(id)
);