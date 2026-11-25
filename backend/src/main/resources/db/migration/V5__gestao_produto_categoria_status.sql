-- V5 (Gestão): o produto passa a usar só categoria_id e status, criados no V3.
-- Sai a coluna de texto "categoria" e o boolean "ativo"; categoria_id vira obrigatório.
-- Também aplica a RN01 (prato sem ficha técnica fica PAUSADO) e corrige fotos do cardápio.
SET NAMES utf8mb4;

-- 1. Pratos criados depois do V4 ainda gravavam a categoria só como texto:
--    garante que a categoria exista e preenche o categoria_id antes de torná-lo obrigatório.
INSERT IGNORE INTO categoria (nome, ordem)
SELECT DISTINCT p.categoria, 99 FROM produto p WHERE p.categoria_id IS NULL;

UPDATE produto p JOIN categoria c ON c.nome = p.categoria
SET p.categoria_id = c.id
WHERE p.categoria_id IS NULL;

-- 2. Pratos pausados ou removidos depois do V4 só mudavam o "ativo".
--    Sem como saber qual dos dois foi, ficam PAUSADO: some do cardápio e o gerente pode reativar.
UPDATE produto SET status = 'PAUSADO' WHERE ativo = FALSE AND status = 'ATIVO';

-- 3. RN01: prato sem ficha técnica (com pelo menos um ingrediente) não fica ATIVO.
--    INATIVO continua INATIVO (exclusão lógica, RN06).
UPDATE produto SET status = 'PAUSADO'
WHERE status = 'ATIVO'
  AND id NOT IN (
    SELECT ft.produto_id FROM ficha_tecnica ft
    JOIN ficha_tecnica_item i ON i.ficha_tecnica_id = ft.id
  );

-- 4. Colunas antigas saem; categoria_id passa a ser obrigatório.
ALTER TABLE produto
  DROP COLUMN categoria,
  DROP COLUMN ativo,
  MODIFY COLUMN categoria_id BIGINT NOT NULL;

-- 5. Fotos que não batiam com o prato (o Steak Tartare mostrava um passaporte, a Burrata um bolo...).
--    Conferidas uma a uma.
UPDATE produto SET imagem = 'https://images.unsplash.com/photo-1785733690832-eabe6e6d5c84?w=800&q=85' WHERE nome = 'Wagyu A5 Ribeye 200g';
UPDATE produto SET imagem = 'https://images.unsplash.com/photo-1588168333986-5078d3ae3976?w=800&q=85' WHERE nome = 'Filet Mignon 300g';
UPDATE produto SET imagem = 'https://images.unsplash.com/photo-1571513062809-2ac71eab2656?w=800&q=85' WHERE nome = 'Steak Tartare';
UPDATE produto SET imagem = 'https://images.unsplash.com/photo-1769458313937-b5ad8f84942e?w=800&q=85' WHERE nome = 'Burrata Artesanal';
UPDATE produto SET imagem = 'https://images.unsplash.com/photo-1774689338470-e4ae7d63670d?w=800&q=85' WHERE nome = 'Tábua de Frios Premium';
UPDATE produto SET imagem = 'https://images.unsplash.com/photo-1687144171995-e5d148fdfbad?w=800&q=85' WHERE nome = 'Lagosta Grelhada';
UPDATE produto SET imagem = 'https://images.unsplash.com/photo-1579216641267-17502e4ac88d?w=800&q=85' WHERE nome = 'Aspargos na Brasa';
UPDATE produto SET imagem = 'https://images.unsplash.com/photo-1676300184943-09b2a08319a3?w=800&q=85' WHERE nome = 'Crème Brûlée de Lavanda';
UPDATE produto SET imagem = 'https://images.unsplash.com/photo-1608270586620-248524c67de9?w=800&q=85' WHERE nome = 'Chopp Artesanal 500ml';
