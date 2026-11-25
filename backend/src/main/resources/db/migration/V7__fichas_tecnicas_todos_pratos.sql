-- V7: fichas técnicas dos 19 pratos que ainda não tinham (os outros 8 vieram no V4),
-- ingredientes novos com estoque inicial e ativação dos pratos (RN01).
-- Só mexe em pratos que ainda não têm ficha; nada do que já existe é alterado.
SET NAMES utf8mb4;

-- 1. Ingredientes novos (INSERT IGNORE: o sku é único)
INSERT IGNORE INTO ingrediente (nome, sku, unidade_medida, estoque_minimo, custo_unitario) VALUES
('Wagyu A5', 'ING-024', 'g', 1000, 0.9500),
('Tomahawk Angus', 'ING-025', 'g', 3000, 0.0620),
('Ancho Angus', 'ING-026', 'g', 2000, 0.0680),
('Costela short rib', 'ING-027', 'g', 3000, 0.0550),
('Costela Angus', 'ING-028', 'g', 3000, 0.0480),
('Pimenta-do-reino', 'ING-029', 'g', 100, 0.0900),
('Mostarda Dijon', 'ING-030', 'g', 200, 0.0500),
('Alcaparras', 'ING-031', 'g', 200, 0.1200),
('Tomate', 'ING-032', 'g', 1000, 0.0120),
('Burrata', 'ING-033', 'g', 1000, 0.1400),
('Pesto de manjericão', 'ING-034', 'g', 300, 0.1000),
('Pão ciabatta', 'ING-035', 'un', 20, 2.5000),
('Queijos artesanais', 'ING-036', 'g', 1000, 0.1100),
('Presunto Parma', 'ING-037', 'g', 500, 0.2800),
('Salame italiano', 'ING-038', 'g', 500, 0.2000),
('Geleia de frutas vermelhas', 'ING-039', 'g', 300, 0.0400),
('Camarão jumbo', 'ING-040', 'g', 1500, 0.1200),
('Lagosta', 'ING-041', 'un', 5, 85.0000),
('Polvo', 'ING-042', 'g', 1500, 0.1600),
('Aspargos', 'ING-043', 'g', 1000, 0.0700),
('Bacon defumado', 'ING-044', 'g', 500, 0.0600),
('Farinha de mandioca', 'ING-045', 'g', 1000, 0.0100),
('Castanha de caju', 'ING-046', 'g', 200, 0.1200),
('Chocolate Valrhona', 'ING-047', 'g', 500, 0.2500),
('Farinha de trigo', 'ING-048', 'g', 1000, 0.0040),
('Sorvete de baunilha', 'ING-049', 'g', 1000, 0.0300),
('Fava de baunilha', 'ING-050', 'un', 10, 6.0000),
('Lavanda', 'ING-051', 'g', 20, 0.4000),
('Vinho Malbec', 'ING-052', 'ml', 1500, 0.0450),
('Fruta para suco', 'ING-053', 'g', 2000, 0.0100),
('Chopp artesanal', 'ING-054', 'ml', 5000, 0.0120),
('Água San Pellegrino', 'ING-055', 'un', 20, 6.0000),
('Fubá', 'ING-056', 'g', 500, 0.0080);

-- 2. Estoque inicial: uma ENTRADA (ajuste) por ingrediente novo, feita pelo admin
INSERT INTO estoque_movimentacao (ingrediente_id, tipo, motivo, quantidade, lote, custo_unitario, usuario_id)
SELECT i.id, 'ENTRADA', 'AJUSTE', v.qtd, 'LOTE-INICIAL', i.custo_unitario,
       (SELECT id FROM usuario WHERE email = 'admin@email.com')
FROM (
  SELECT 'ING-024' AS sku, 4000 AS qtd
  UNION ALL SELECT 'ING-025', 15000
  UNION ALL SELECT 'ING-026', 10000
  UNION ALL SELECT 'ING-027', 15000
  UNION ALL SELECT 'ING-028', 18000
  UNION ALL SELECT 'ING-029', 800
  UNION ALL SELECT 'ING-030', 1500
  UNION ALL SELECT 'ING-031', 1500
  UNION ALL SELECT 'ING-032', 6000
  UNION ALL SELECT 'ING-033', 5000
  UNION ALL SELECT 'ING-034', 2000
  UNION ALL SELECT 'ING-035', 150
  UNION ALL SELECT 'ING-036', 5000
  UNION ALL SELECT 'ING-037', 3000
  UNION ALL SELECT 'ING-038', 3000
  UNION ALL SELECT 'ING-039', 2000
  UNION ALL SELECT 'ING-040', 8000
  UNION ALL SELECT 'ING-041', 40
  UNION ALL SELECT 'ING-042', 6000
  UNION ALL SELECT 'ING-043', 5000
  UNION ALL SELECT 'ING-044', 4000
  UNION ALL SELECT 'ING-045', 8000
  UNION ALL SELECT 'ING-046', 2000
  UNION ALL SELECT 'ING-047', 4000
  UNION ALL SELECT 'ING-048', 8000
  UNION ALL SELECT 'ING-049', 5000
  UNION ALL SELECT 'ING-050', 60
  UNION ALL SELECT 'ING-051', 200
  UNION ALL SELECT 'ING-052', 20000
  UNION ALL SELECT 'ING-053', 30000
  UNION ALL SELECT 'ING-054', 60000
  UNION ALL SELECT 'ING-055', 200
  UNION ALL SELECT 'ING-056', 5000
) v JOIN ingrediente i ON i.sku = v.sku;

-- 3. Fichas técnicas (rendimento 1 porção). INSERT IGNORE: produto_id é único, então
--    prato que já tem ficha continua com a dele.
INSERT IGNORE INTO ficha_tecnica (produto_id, rendimento, modo_preparo)
SELECT p.id, 1, v.modo
FROM (
  SELECT 'Wagyu A5 Ribeye 200g' AS prato, 'Selar o wagyu em fogo muito alto, 1 minuto de cada lado, finalizar com manteiga e sal. Descansar 3 minutos.' AS modo
  UNION ALL SELECT 'Tomahawk Angus 1.2kg', 'Temperar, selar na brasa e finalizar em forno baixo. Servir com chimichurri de alho, alecrim e azeite.'
  UNION ALL SELECT 'Ancho Angus 400g', 'Temperar com sal, grelhar no ponto desejado e finalizar com manteiga e alho.'
  UNION ALL SELECT 'Costela Short Rib 600g', 'Assar em baixa temperatura por 12 horas com ervas e vinho tinto. Caramelizar na brasa antes de servir.'
  UNION ALL SELECT 'Costela Angus 48h', 'Assar por 48 horas em baixa temperatura, desfiar na hora e servir com polenta cremosa.'
  UNION ALL SELECT 'Steak Tartare', 'Cortar o filé à faca, misturar com gema, mostarda e alcaparras. Servir com torradas.'
  UNION ALL SELECT 'Burrata Artesanal', 'Servir a burrata sobre tomates confit com pesto, azeite e ciabatta tostada.'
  UNION ALL SELECT 'Tábua de Frios Premium', 'Montar a tábua com queijos, presunto Parma, salame, geleia e pães.'
  UNION ALL SELECT 'Camarão na Manteiga de Ervas', 'Saltear os camarões na manteiga com alho e ervas, finalizar com limão e pimenta.'
  UNION ALL SELECT 'Lagosta Grelhada', 'Grelhar a lagosta com manteiga e ervas e servir com bisque cremoso.'
  UNION ALL SELECT 'Polvo Grelhado ao Azeite', 'Cozinhar o polvo em baixa temperatura, finalizar na brasa e servir com batatas rústicas e azeite.'
  UNION ALL SELECT 'Aspargos na Brasa', 'Grelhar os aspargos com azeite, finalizar com limão, parmesão e sal.'
  UNION ALL SELECT 'Farofa Gourmet de Bacon', 'Dourar o bacon, juntar manteiga, a farinha de mandioca e a castanha de caju.'
  UNION ALL SELECT 'Petit Gâteau de Valrhona', 'Derreter o chocolate com manteiga, misturar ovos, açúcar e farinha. Assar até o centro ficar líquido e servir com sorvete.'
  UNION ALL SELECT 'Crème Brûlée de Lavanda', 'Infusionar o creme com lavanda e baunilha, assar em banho-maria e caramelizar o açúcar na hora.'
  UNION ALL SELECT 'Vinho Malbec Taça', 'Servir 150 ml à temperatura de adega.'
  UNION ALL SELECT 'Suco Prensado a Frio 400ml', 'Prensar a frio as frutas e servir gelado.'
  UNION ALL SELECT 'Chopp Artesanal 500ml', 'Tirar o chopp na hora, com colarinho cremoso.'
  UNION ALL SELECT 'Água San Pellegrino 500ml', 'Servir gelada, com gelo e fatia de limão.'
) v JOIN produto p ON p.nome = v.prato;

-- 4. Itens das fichas (só das fichas que ainda não têm nenhum item)
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao)
SELECT ft.id, i.id, v.qtd, v.un, v.fc
FROM (
  SELECT 'Wagyu A5 Ribeye 200g' AS prato, 'ING-024' AS sku, 200 AS qtd, 'g' AS un, 1.10 AS fc
  UNION ALL SELECT 'Wagyu A5 Ribeye 200g', 'ING-005', 15, 'g', 1.00
  UNION ALL SELECT 'Wagyu A5 Ribeye 200g', 'ING-023', 3, 'g', 1.00
  UNION ALL SELECT 'Tomahawk Angus 1.2kg', 'ING-025', 1200, 'g', 1.15
  UNION ALL SELECT 'Tomahawk Angus 1.2kg', 'ING-023', 10, 'g', 1.00
  UNION ALL SELECT 'Tomahawk Angus 1.2kg', 'ING-005', 30, 'g', 1.00
  UNION ALL SELECT 'Tomahawk Angus 1.2kg', 'ING-008', 4, 'g', 1.00
  UNION ALL SELECT 'Tomahawk Angus 1.2kg', 'ING-010', 10, 'g', 1.10
  UNION ALL SELECT 'Tomahawk Angus 1.2kg', 'ING-007', 20, 'ml', 1.00
  UNION ALL SELECT 'Ancho Angus 400g', 'ING-026', 400, 'g', 1.10
  UNION ALL SELECT 'Ancho Angus 400g', 'ING-023', 5, 'g', 1.00
  UNION ALL SELECT 'Ancho Angus 400g', 'ING-005', 20, 'g', 1.00
  UNION ALL SELECT 'Ancho Angus 400g', 'ING-010', 5, 'g', 1.10
  UNION ALL SELECT 'Costela Short Rib 600g', 'ING-027', 600, 'g', 1.20
  UNION ALL SELECT 'Costela Short Rib 600g', 'ING-023', 6, 'g', 1.00
  UNION ALL SELECT 'Costela Short Rib 600g', 'ING-008', 3, 'g', 1.00
  UNION ALL SELECT 'Costela Short Rib 600g', 'ING-010', 8, 'g', 1.10
  UNION ALL SELECT 'Costela Short Rib 600g', 'ING-052', 100, 'ml', 1.00
  UNION ALL SELECT 'Costela Angus 48h', 'ING-028', 600, 'g', 1.25
  UNION ALL SELECT 'Costela Angus 48h', 'ING-023', 6, 'g', 1.00
  UNION ALL SELECT 'Costela Angus 48h', 'ING-005', 20, 'g', 1.00
  UNION ALL SELECT 'Costela Angus 48h', 'ING-006', 50, 'ml', 1.00
  UNION ALL SELECT 'Costela Angus 48h', 'ING-056', 100, 'g', 1.00
  UNION ALL SELECT 'Steak Tartare', 'ING-002', 150, 'g', 1.15
  UNION ALL SELECT 'Steak Tartare', 'ING-018', 1, 'un', 1.00
  UNION ALL SELECT 'Steak Tartare', 'ING-030', 10, 'g', 1.00
  UNION ALL SELECT 'Steak Tartare', 'ING-031', 10, 'g', 1.00
  UNION ALL SELECT 'Steak Tartare', 'ING-023', 3, 'g', 1.00
  UNION ALL SELECT 'Steak Tartare', 'ING-035', 0.5, 'un', 1.00
  UNION ALL SELECT 'Burrata Artesanal', 'ING-033', 125, 'g', 1.00
  UNION ALL SELECT 'Burrata Artesanal', 'ING-032', 100, 'g', 1.10
  UNION ALL SELECT 'Burrata Artesanal', 'ING-034', 30, 'g', 1.00
  UNION ALL SELECT 'Burrata Artesanal', 'ING-007', 10, 'ml', 1.00
  UNION ALL SELECT 'Burrata Artesanal', 'ING-035', 0.5, 'un', 1.00
  UNION ALL SELECT 'Tábua de Frios Premium', 'ING-036', 150, 'g', 1.05
  UNION ALL SELECT 'Tábua de Frios Premium', 'ING-037', 80, 'g', 1.05
  UNION ALL SELECT 'Tábua de Frios Premium', 'ING-038', 60, 'g', 1.05
  UNION ALL SELECT 'Tábua de Frios Premium', 'ING-039', 40, 'g', 1.00
  UNION ALL SELECT 'Tábua de Frios Premium', 'ING-035', 1, 'un', 1.00
  UNION ALL SELECT 'Camarão na Manteiga de Ervas', 'ING-040', 250, 'g', 1.30
  UNION ALL SELECT 'Camarão na Manteiga de Ervas', 'ING-005', 30, 'g', 1.00
  UNION ALL SELECT 'Camarão na Manteiga de Ervas', 'ING-010', 8, 'g', 1.10
  UNION ALL SELECT 'Camarão na Manteiga de Ervas', 'ING-011', 0.5, 'un', 1.00
  UNION ALL SELECT 'Camarão na Manteiga de Ervas', 'ING-029', 1, 'g', 1.00
  UNION ALL SELECT 'Lagosta Grelhada', 'ING-041', 1, 'un', 1.00
  UNION ALL SELECT 'Lagosta Grelhada', 'ING-005', 40, 'g', 1.00
  UNION ALL SELECT 'Lagosta Grelhada', 'ING-006', 60, 'ml', 1.00
  UNION ALL SELECT 'Lagosta Grelhada', 'ING-010', 5, 'g', 1.10
  UNION ALL SELECT 'Lagosta Grelhada', 'ING-008', 2, 'g', 1.00
  UNION ALL SELECT 'Polvo Grelhado ao Azeite', 'ING-042', 300, 'g', 1.15
  UNION ALL SELECT 'Polvo Grelhado ao Azeite', 'ING-007', 25, 'ml', 1.00
  UNION ALL SELECT 'Polvo Grelhado ao Azeite', 'ING-004', 200, 'g', 1.20
  UNION ALL SELECT 'Polvo Grelhado ao Azeite', 'ING-010', 8, 'g', 1.10
  UNION ALL SELECT 'Polvo Grelhado ao Azeite', 'ING-023', 3, 'g', 1.00
  UNION ALL SELECT 'Aspargos na Brasa', 'ING-043', 200, 'g', 1.20
  UNION ALL SELECT 'Aspargos na Brasa', 'ING-007', 15, 'ml', 1.00
  UNION ALL SELECT 'Aspargos na Brasa', 'ING-011', 0.5, 'un', 1.00
  UNION ALL SELECT 'Aspargos na Brasa', 'ING-012', 20, 'g', 1.00
  UNION ALL SELECT 'Aspargos na Brasa', 'ING-023', 2, 'g', 1.00
  UNION ALL SELECT 'Farofa Gourmet de Bacon', 'ING-045', 120, 'g', 1.00
  UNION ALL SELECT 'Farofa Gourmet de Bacon', 'ING-044', 60, 'g', 1.05
  UNION ALL SELECT 'Farofa Gourmet de Bacon', 'ING-046', 20, 'g', 1.00
  UNION ALL SELECT 'Farofa Gourmet de Bacon', 'ING-005', 20, 'g', 1.00
  UNION ALL SELECT 'Farofa Gourmet de Bacon', 'ING-023', 2, 'g', 1.00
  UNION ALL SELECT 'Petit Gâteau de Valrhona', 'ING-047', 100, 'g', 1.00
  UNION ALL SELECT 'Petit Gâteau de Valrhona', 'ING-005', 50, 'g', 1.00
  UNION ALL SELECT 'Petit Gâteau de Valrhona', 'ING-018', 2, 'un', 1.00
  UNION ALL SELECT 'Petit Gâteau de Valrhona', 'ING-017', 40, 'g', 1.00
  UNION ALL SELECT 'Petit Gâteau de Valrhona', 'ING-048', 30, 'g', 1.00
  UNION ALL SELECT 'Petit Gâteau de Valrhona', 'ING-049', 80, 'g', 1.00
  UNION ALL SELECT 'Crème Brûlée de Lavanda', 'ING-006', 150, 'ml', 1.00
  UNION ALL SELECT 'Crème Brûlée de Lavanda', 'ING-018', 3, 'un', 1.00
  UNION ALL SELECT 'Crème Brûlée de Lavanda', 'ING-017', 40, 'g', 1.00
  UNION ALL SELECT 'Crème Brûlée de Lavanda', 'ING-050', 0.25, 'un', 1.00
  UNION ALL SELECT 'Crème Brûlée de Lavanda', 'ING-051', 1, 'g', 1.00
  UNION ALL SELECT 'Crème Brûlée de Lavanda', 'ING-016', 30, 'g', 1.05
  UNION ALL SELECT 'Vinho Malbec Taça', 'ING-052', 150, 'ml', 1.00
  UNION ALL SELECT 'Suco Prensado a Frio 400ml', 'ING-053', 450, 'g', 1.10
  UNION ALL SELECT 'Chopp Artesanal 500ml', 'ING-054', 500, 'ml', 1.05
  UNION ALL SELECT 'Água San Pellegrino 500ml', 'ING-055', 1, 'un', 1.00
) v
JOIN produto p ON p.nome = v.prato
JOIN ficha_tecnica ft ON ft.produto_id = p.id
JOIN ingrediente i ON i.sku = v.sku
WHERE ft.id NOT IN (SELECT ficha_tecnica_id FROM ficha_tecnica_item);

-- 5. RN01 cumprida: prato PAUSADO que agora tem ficha com ingrediente volta a ATIVO.
--    INATIVO (exclusão lógica) continua INATIVO.
UPDATE produto SET status = 'ATIVO'
WHERE status = 'PAUSADO'
  AND id IN (
    SELECT ft.produto_id FROM ficha_tecnica ft
    JOIN ficha_tecnica_item it ON it.ficha_tecnica_id = ft.id
  );
