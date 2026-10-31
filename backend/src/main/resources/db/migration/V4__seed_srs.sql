-- V4: seed do SRS: admin, categorias, ingredientes com estoque, fichas técnicas
-- de 8 pratos, 2 fornecedores com catálogo. Roda uma vez só (o Flyway controla).
SET NAMES utf8mb4;

-- Admin pedido pelo professor: admin@email.com / senha123 (BCrypt)
INSERT IGNORE INTO usuario (email, senha_hash, nome, perfil)
VALUES ('admin@email.com', '$2a$10$YZ7DP4qjM1dIL/ItKtfMc.Y4FOmfIU0C5vWNVbxiMWn.0MmAQ.bLy', 'Administrador', 'ADMIN');

-- Categorias (texto exato das categorias do cardápio) e vínculo com os pratos
INSERT IGNORE INTO categoria (nome, ordem) VALUES
('Carnes Nobres', 1),
('Entradas', 2),
('Peixes & Frutos do Mar', 3),
('Acompanhamentos', 4),
('Sobremesas', 5),
('Bebidas', 6);

UPDATE produto p JOIN categoria c ON c.nome = p.categoria SET p.categoria_id = c.id;
UPDATE produto SET status = IF(ativo, 'ATIVO', 'INATIVO');

-- Ingredientes (custo por unidade_medida)
INSERT INTO ingrediente (nome, sku, unidade_medida, estoque_minimo, custo_unitario) VALUES
('Picanha maturada',   'ING-001', 'g',  2000, 0.0780),
('Filé mignon',        'ING-002', 'g',  2000, 0.0890),
('Salmão fresco',      'ING-003', 'g',  1500, 0.0950),
('Batata',             'ING-004', 'g',  3000, 0.0060),
('Manteiga',           'ING-005', 'g',   500, 0.0420),
('Creme de leite',     'ING-006', 'ml', 1000, 0.0140),
('Azeite extra virgem','ING-007', 'ml',  500, 0.0450),
('Alecrim',            'ING-008', 'g',   100, 0.0600),
('Óleo de trufa',      'ING-009', 'ml',  100, 0.2500),
('Alho',               'ING-010', 'g',   200, 0.0180),
('Limão',              'ING-011', 'un',   20, 0.8000),
('Queijo parmesão',    'ING-012', 'g',   300, 0.1200),
('Rúcula',             'ING-013', 'g',   200, 0.0400),
('Cream cheese',       'ING-014', 'g',   500, 0.0380),
('Biscoito maisena',   'ING-015', 'g',   500, 0.0220),
('Frutas vermelhas',   'ING-016', 'g',   500, 0.0750),
('Açúcar',             'ING-017', 'g',  1000, 0.0050),
('Ovos',               'ING-018', 'un',   30, 0.9500),
('Mascarpone',         'ING-019', 'g',   500, 0.0650),
('Café',               'ING-020', 'ml',  500, 0.0100),
('Biscoito champanhe', 'ING-021', 'g',   300, 0.0450),
('Cacau em pó',        'ING-022', 'g',   100, 0.0600),
('Sal',                'ING-023', 'g',   500, 0.0020);

-- Estoque inicial: uma ENTRADA (ajuste) por ingrediente, feita pelo admin
INSERT INTO estoque_movimentacao (ingrediente_id, tipo, motivo, quantidade, lote, custo_unitario, usuario_id)
SELECT i.id, 'ENTRADA', 'AJUSTE', v.qtd, 'LOTE-INICIAL', i.custo_unitario,
       (SELECT id FROM usuario WHERE email = 'admin@email.com')
FROM (
  SELECT 'ING-001' AS sku, 10000 AS qtd UNION ALL SELECT 'ING-002', 8000 UNION ALL SELECT 'ING-003', 6000
  UNION ALL SELECT 'ING-004', 20000 UNION ALL SELECT 'ING-005', 3000 UNION ALL SELECT 'ING-006', 8000
  UNION ALL SELECT 'ING-007', 3000 UNION ALL SELECT 'ING-008', 500 UNION ALL SELECT 'ING-009', 500
  UNION ALL SELECT 'ING-010', 1500 UNION ALL SELECT 'ING-011', 100 UNION ALL SELECT 'ING-012', 1500
  UNION ALL SELECT 'ING-013', 1000 UNION ALL SELECT 'ING-014', 4000 UNION ALL SELECT 'ING-015', 3000
  UNION ALL SELECT 'ING-016', 2500 UNION ALL SELECT 'ING-017', 8000 UNION ALL SELECT 'ING-018', 200
  UNION ALL SELECT 'ING-019', 3000 UNION ALL SELECT 'ING-020', 3000 UNION ALL SELECT 'ING-021', 2000
  UNION ALL SELECT 'ING-022', 500 UNION ALL SELECT 'ING-023', 5000
) v JOIN ingrediente i ON i.sku = v.sku;

-- Fichas técnicas (8 pratos). RN01: só estes devem ficar ATIVO quando a Gestão aplicar a regra.
INSERT INTO ficha_tecnica (produto_id, rendimento, modo_preparo)
SELECT p.id, 1, v.modo
FROM (
  SELECT 'Picanha Maturada 400g' AS prato, 'Temperar com sal, selar em fogo alto, finalizar com manteiga e descansar 5 minutos.' AS modo
  UNION ALL SELECT 'Filet Mignon 300g', 'Grelhar o filé com alho e manteiga até o ponto desejado.'
  UNION ALL SELECT 'Carpaccio de Filé Mignon', 'Fatiar o filé bem fino, montar sobre a rúcula, finalizar com parmesão, azeite e limão.'
  UNION ALL SELECT 'Salmão Grelhado 300g', 'Grelhar o salmão com manteiga, finalizar com limão e azeite.'
  UNION ALL SELECT 'Purê de Batata Trufado', 'Cozinhar e amassar as batatas, misturar manteiga e creme de leite, finalizar com óleo de trufa.'
  UNION ALL SELECT 'Batata Rústica ao Alecrim', 'Assar as batatas com azeite, alho e alecrim até dourar.'
  UNION ALL SELECT 'Cheesecake de Frutas Vermelhas', 'Base de biscoito, creme de cream cheese assado em banho-maria, cobertura de frutas vermelhas.'
  UNION ALL SELECT 'Tiramisu Clássico', 'Intercalar biscoito champanhe molhado no café com creme de mascarpone e finalizar com cacau.'
) v JOIN produto p ON p.nome = v.prato;

INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao)
SELECT ft.id, i.id, v.qtd, v.un, v.fc
FROM (
  SELECT 'Picanha Maturada 400g' AS prato, 'ING-001' AS sku, 400 AS qtd, 'g' AS un, 1.20 AS fc
  UNION ALL SELECT 'Picanha Maturada 400g', 'ING-023',   5, 'g', 1.00
  UNION ALL SELECT 'Picanha Maturada 400g', 'ING-005',  20, 'g', 1.00
  UNION ALL SELECT 'Filet Mignon 300g', 'ING-002', 300, 'g', 1.15
  UNION ALL SELECT 'Filet Mignon 300g', 'ING-005',  20, 'g', 1.00
  UNION ALL SELECT 'Filet Mignon 300g', 'ING-010',   5, 'g', 1.10
  UNION ALL SELECT 'Filet Mignon 300g', 'ING-023',   4, 'g', 1.00
  UNION ALL SELECT 'Carpaccio de Filé Mignon', 'ING-002', 120, 'g', 1.15
  UNION ALL SELECT 'Carpaccio de Filé Mignon', 'ING-013',  30, 'g', 1.25
  UNION ALL SELECT 'Carpaccio de Filé Mignon', 'ING-012',  20, 'g', 1.00
  UNION ALL SELECT 'Carpaccio de Filé Mignon', 'ING-007',  15, 'ml', 1.00
  UNION ALL SELECT 'Carpaccio de Filé Mignon', 'ING-011',   1, 'un', 1.00
  UNION ALL SELECT 'Salmão Grelhado 300g', 'ING-003', 300, 'g', 1.15
  UNION ALL SELECT 'Salmão Grelhado 300g', 'ING-005',  15, 'g', 1.00
  UNION ALL SELECT 'Salmão Grelhado 300g', 'ING-011',   1, 'un', 1.00
  UNION ALL SELECT 'Salmão Grelhado 300g', 'ING-007',  10, 'ml', 1.00
  UNION ALL SELECT 'Salmão Grelhado 300g', 'ING-023',   4, 'g', 1.00
  UNION ALL SELECT 'Purê de Batata Trufado', 'ING-004', 300, 'g', 1.25
  UNION ALL SELECT 'Purê de Batata Trufado', 'ING-005',  40, 'g', 1.00
  UNION ALL SELECT 'Purê de Batata Trufado', 'ING-006',  60, 'ml', 1.00
  UNION ALL SELECT 'Purê de Batata Trufado', 'ING-009',   5, 'ml', 1.00
  UNION ALL SELECT 'Purê de Batata Trufado', 'ING-023',   3, 'g', 1.00
  UNION ALL SELECT 'Batata Rústica ao Alecrim', 'ING-004', 350, 'g', 1.20
  UNION ALL SELECT 'Batata Rústica ao Alecrim', 'ING-007',  20, 'ml', 1.00
  UNION ALL SELECT 'Batata Rústica ao Alecrim', 'ING-008',   3, 'g', 1.00
  UNION ALL SELECT 'Batata Rústica ao Alecrim', 'ING-010',   5, 'g', 1.10
  UNION ALL SELECT 'Cheesecake de Frutas Vermelhas', 'ING-014',  80, 'g', 1.00
  UNION ALL SELECT 'Cheesecake de Frutas Vermelhas', 'ING-015',  30, 'g', 1.00
  UNION ALL SELECT 'Cheesecake de Frutas Vermelhas', 'ING-017',  30, 'g', 1.00
  UNION ALL SELECT 'Cheesecake de Frutas Vermelhas', 'ING-016',  40, 'g', 1.05
  UNION ALL SELECT 'Cheesecake de Frutas Vermelhas', 'ING-018',   1, 'un', 1.00
  UNION ALL SELECT 'Cheesecake de Frutas Vermelhas', 'ING-006',  30, 'ml', 1.00
  UNION ALL SELECT 'Tiramisu Clássico', 'ING-019', 100, 'g', 1.00
  UNION ALL SELECT 'Tiramisu Clássico', 'ING-021',  40, 'g', 1.00
  UNION ALL SELECT 'Tiramisu Clássico', 'ING-020',  50, 'ml', 1.00
  UNION ALL SELECT 'Tiramisu Clássico', 'ING-018',   1, 'un', 1.00
  UNION ALL SELECT 'Tiramisu Clássico', 'ING-017',  25, 'g', 1.00
  UNION ALL SELECT 'Tiramisu Clássico', 'ING-022',   5, 'g', 1.00
) v
JOIN produto p ON p.nome = v.prato
JOIN ficha_tecnica ft ON ft.produto_id = p.id
JOIN ingrediente i ON i.sku = v.sku;

-- Fornecedores de exemplo (CNPJs válidos) e catálogo de preços
INSERT INTO fornecedor (razao_social, cnpj, telefone, email, categorias) VALUES
('Casa de Carnes Prime Ltda', '11.222.333/0001-81', '(11) 3000-1111', 'vendas@carnesprime.com.br', 'Carnes e peixes'),
('Hortifruti Boa Safra ME',   '44.555.666/0001-81', '(11) 3000-2222', 'contato@boasafra.com.br',   'Hortifruti e laticínios');

INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda)
SELECT f.id, i.id, v.preco, v.un
FROM (
  SELECT '11.222.333/0001-81' AS cnpj, 'ING-001' AS sku, 0.0780 AS preco, 'g' AS un
  UNION ALL SELECT '11.222.333/0001-81', 'ING-002', 0.0890, 'g'
  UNION ALL SELECT '11.222.333/0001-81', 'ING-003', 0.0950, 'g'
  UNION ALL SELECT '44.555.666/0001-81', 'ING-004', 0.0060, 'g'
  UNION ALL SELECT '44.555.666/0001-81', 'ING-013', 0.0400, 'g'
  UNION ALL SELECT '44.555.666/0001-81', 'ING-014', 0.0380, 'g'
  UNION ALL SELECT '44.555.666/0001-81', 'ING-005', 0.0420, 'g'
  UNION ALL SELECT '44.555.666/0001-81', 'ING-003', 0.1020, 'g'
) v
JOIN fornecedor f ON f.cnpj = v.cnpj
JOIN ingrediente i ON i.sku = v.sku;

INSERT INTO historico_preco (fornecedor_id, ingrediente_id, preco)
SELECT fornecedor_id, ingrediente_id, preco FROM fornecedor_produto;
