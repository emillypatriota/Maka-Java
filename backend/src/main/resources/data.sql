USE maka;

INSERT IGNORE INTO usuario (email, senha_hash, nome, perfil) VALUES
('gerente@maka.com',    '$2a$10$4x2xL1lBcaSerMXVLW5BFOSa4n/4p312GX9FeQmpAqdtogDsMVuma', 'Gerente',    'MANAGER'),
('cozinha@maka.com',    '$2a$10$4x2xL1lBcaSerMXVLW5BFOSa4n/4p312GX9FeQmpAqdtogDsMVuma', 'Cozinha',    'KITCHEN'),
('entregador@maka.com', '$2a$10$4x2xL1lBcaSerMXVLW5BFOSa4n/4p312GX9FeQmpAqdtogDsMVuma', 'Entregador', 'DELIVERY');

-- ============================================================
-- Seed da Gestão: cardápio (27 pratos, antigo INITIAL_MENU) e
-- funcionários (6, antigo SEED_STAFF). Ids fixos para o INSERT IGNORE
-- não duplicar se o arquivo rodar de novo.
-- As categorias têm o texto exato de CATEGORIES do front.
-- Nenhum prato do INITIAL_MENU tinha tags, então produto_tag fica vazio.
-- ============================================================
SET NAMES utf8mb4;

INSERT IGNORE INTO produto (id, nome, descricao, preco, categoria, imagem, destaque, ativo, tempo_preparo) VALUES
-- Carnes Nobres
(1,  'Wagyu A5 Ribeye 200g', 'O nível máximo de marmoreio japonês. Textura sedosa que derrete na boca. Servido com sal negro havaiano, manteiga clarificada e brioche tostado artesanal.', 490.00, 'Carnes Nobres', 'https://images.unsplash.com/photo-1785733690832-eabe6e6d5c84?w=800&q=85', TRUE,  TRUE, 25),
(2,  'Tomahawk Angus 1.2kg', 'Corte espetacular com osso longo de 30cm, maturado 30 dias em câmara própria. Chimichurri artesanal e farofa crocante de panko.', 380.00, 'Carnes Nobres', 'https://images.unsplash.com/photo-1600891964092-4316c288032e?w=800&q=85', TRUE,  TRUE, 35),
(3,  'Picanha Maturada 400g', 'A rainha das carnes brasileiras, selecionada e maturada 21 dias. Gordura dourada e crocante, interior rosado e suculento. Acompanha vinagrete artesanal.', 148.00, 'Carnes Nobres', 'https://images.unsplash.com/photo-1529692236671-f1f6cf9683ba?w=800&q=85', TRUE,  TRUE, 20),
(4,  'Ancho Angus 400g', 'Corte nobre argentino, famoso pelo marmoreio generoso e maciez incomparável. Maturado 21 dias, grelhado na temperatura exata do seu ponto.', 148.00, 'Carnes Nobres', 'https://images.unsplash.com/photo-1558030006-450675393462?w=800&q=85', FALSE, TRUE, 20),
(5,  'Filet Mignon 300g', 'O corte mais macio do boi, envolto em fatias de bacon defumado artesanal e flambado na manteiga clarificada com tomilho e alecrim frescos.', 138.00, 'Carnes Nobres', 'https://images.unsplash.com/photo-1588168333986-5078d3ae3976?w=800&q=85', FALSE, TRUE, 18),
(6,  'Costela Short Rib 600g', 'Assada em baixa temperatura por 12 horas com ervas e vinho tinto. Desfia no garfo com facilidade e finaliza caramelizada na brasa para textura perfeita.', 128.00, 'Carnes Nobres', 'https://images.unsplash.com/photo-1544025162-d76694265947?w=800&q=85', FALSE, TRUE, 15),
(7,  'Costela Angus 48h', 'Maturada e assada por 48 horas em forno a lenha em baixíssima temperatura. Desfiada na hora, servida com polenta cremosa e molho de pimenta defumada.', 118.00, 'Carnes Nobres', 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=800&q=85', FALSE, TRUE, 15),
-- Entradas
(8,  'Steak Tartare', 'Filé cortado à faca na hora, temperado com gema curada em sal e açúcar, alcaparras, mostarda Dijon e tabasco. Acompanha torradas artesanais crocantes.', 68.00, 'Entradas', 'https://images.unsplash.com/photo-1571513062809-2ac71eab2656?w=800&q=85', FALSE, TRUE, 10),
(9,  'Carpaccio de Filé Mignon', 'Finíssimas lâminas de filé mignon cru, cama de rúcula selvagem, parmesão 36 meses em lascas, alcaparras e finalização com azeite trufado da Itália.', 62.00, 'Entradas', 'https://images.unsplash.com/photo-1588944651162-57080995b92d?w=800&q=85', FALSE, TRUE, 8),
(10, 'Burrata Artesanal', 'Burrata produzida artesanalmente com leite integral, tomates confit em azeite de ervas da Provence, pesto de manjericão fresco e ciabatta tostada.', 56.00, 'Entradas', 'https://images.unsplash.com/photo-1769458313937-b5ad8f84942e?w=800&q=85', FALSE, TRUE, 8),
(11, 'Tábua de Frios Premium', 'Seleção de queijos artesanais, presunto Parma 24 meses, salame italiano Felino, copa, geleias de frutas vermelhas e pães de fermentação natural.', 82.00, 'Entradas', 'https://images.unsplash.com/photo-1774689338470-e4ae7d63670d?w=800&q=85', FALSE, TRUE, 5),
(12, 'Camarão na Manteiga de Ervas', 'Camarões jumbo VG salteados em manteiga de ervas com alho negro caramelizado, limão siciliano e pimenta-do-reino fresca. Servidos em panela de ferro fundido.', 76.00, 'Entradas', 'https://images.unsplash.com/photo-1599021419847-d8a7a6aba5b4?w=800&q=85', FALSE, TRUE, 12),
-- Peixes & Frutos do Mar
(13, 'Salmão Grelhado 300g', 'Salmão norueguês premium grelhado na manteiga de ervas frescas. Acompanha risoto cremoso de limão siciliano com aspargos e alcaparras fritas.', 98.00, 'Peixes & Frutos do Mar', 'https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?w=800&q=85', FALSE, TRUE, 20),
(14, 'Lagosta Grelhada', 'Lagosta inteira grelhada com manteiga clarificada e hervas frescas de Provence. Acompanha molho bisque cremoso feito com as cascas e legumes ao vapor.', 198.00, 'Peixes & Frutos do Mar', 'https://images.unsplash.com/photo-1687144171995-e5d148fdfbad?w=800&q=85', FALSE, TRUE, 25),
(15, 'Polvo Grelhado ao Azeite', 'Polvo português de alta qualidade, cozido em baixa temperatura e finalizado na brasa. Azeite extra virgem português, batatas rústicas ao murro e vinagrete de pimentões.', 142.00, 'Peixes & Frutos do Mar', 'https://images.unsplash.com/photo-1534080564583-6be75777b70a?w=800&q=85', FALSE, TRUE, 22),
-- Acompanhamentos
(16, 'Purê de Batata Trufado', 'Batata Asterix cozida e espremida com manteiga clarificada francesa, creme de leite fresco e trufa negra italiana Périgord ralada generosamente na hora.', 38.00, 'Acompanhamentos', 'https://images.unsplash.com/photo-1624968814155-236efede1cec?w=800&q=85', FALSE, TRUE, 10),
(17, 'Aspargos na Brasa', 'Aspargos verdes frescos importados, grelhados com fio de azeite extra virgem, raspas de limão siciliano e cobertos com parmesão em lascas e flor de sal.', 32.00, 'Acompanhamentos', 'https://images.unsplash.com/photo-1579216641267-17502e4ac88d?w=800&q=85', FALSE, TRUE, 10),
(18, 'Farofa Gourmet de Bacon', 'Farofa de mandioca torrada na manteiga de garrafa nordestina com bacon defumado artesanal crocante, castanha de caju e ervas frescas picadas.', 28.00, 'Acompanhamentos', 'https://images.unsplash.com/photo-1518977676601-b53f82aba655?w=800&q=85', FALSE, TRUE, 8),
(19, 'Batata Rústica ao Alecrim', 'Batatas cortadas à mão em gomos, assadas com alecrim fresco, alho confitado, sal grosso e finalização generosa em azeite extra virgem. Crocante por fora, macia por dentro.', 30.00, 'Acompanhamentos', 'https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=800&q=85', FALSE, TRUE, 25),
-- Sobremesas
(20, 'Petit Gâteau de Valrhona', 'Bolo de chocolate Valrhona 70% cacau com centro vulcânico líquido, servido com sorvete artesanal de baunilha Madagascar e calda de caramelo fleur de sel.', 42.00, 'Sobremesas', 'https://images.unsplash.com/photo-1606313564200-e75d5e30476c?w=800&q=85', FALSE, TRUE, 12),
(21, 'Crème Brûlée de Lavanda', 'Creme sedoso infusionado com lavanda francesa e fava de baunilha Bourbon, caramelizado com maçarico na hora. Servido com frutas vermelhas frescas da estação.', 38.00, 'Sobremesas', 'https://images.unsplash.com/photo-1676300184943-09b2a08319a3?w=800&q=85', FALSE, TRUE, 10),
(22, 'Cheesecake de Frutas Vermelhas', 'Base amanteigada de biscoito Graham crocante, recheio aveludado de cream cheese com baunilha e calda generosa de frutas vermelhas frescas e coulis de framboesa.', 36.00, 'Sobremesas', 'https://images.unsplash.com/photo-1565958011703-44f9829ba187?w=800&q=85', FALSE, TRUE, 8),
(23, 'Tiramisu Clássico', 'Receita italiana original: mascarpone fresco batido, biscoito champagne embebido em espresso duplo e licor de Amaretto, finalizado com cacau belga em pó.', 40.00, 'Sobremesas', 'https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?w=800&q=85', FALSE, TRUE, 8),
-- Bebidas
(24, 'Vinho Malbec Taça', 'Malbec argentino selecionado da região de Mendoza. Notas intensas de frutas vermelhas escuras, especiarias e toque amadeirado. Servido à temperatura ideal 16–18°C.', 38.00, 'Bebidas', 'https://images.unsplash.com/photo-1510812431401-41d2bd2722f3?w=800&q=85', FALSE, TRUE, 2),
(25, 'Suco Prensado a Frio 400ml', 'Laranja com maracujá, abacaxi com hortelã ou melancia com gengibre. Extraídos a frio para preservar 100% dos nutrientes e vitaminas. Zero açúcar adicionado.', 24.00, 'Bebidas', 'https://images.unsplash.com/photo-1600271886742-f049cd451bba?w=800&q=85', FALSE, TRUE, 3),
(26, 'Chopp Artesanal 500ml', 'Chopp pilsen gelado de cervejaria artesanal local, leve e refrescante com espuma cremosa perfeita. Tirado na hora diretamente do barril pressurizado.', 22.00, 'Bebidas', 'https://images.unsplash.com/photo-1608270586620-248524c67de9?w=800&q=85', FALSE, TRUE, 2),
(27, 'Água San Pellegrino 500ml', 'Água mineral italiana premium, natural da fonte San Pellegrino nas Alpes Lombardas. Com ou sem gás. Servida gelada com gelo e fatia de limão siciliano a pedido.', 18.00, 'Bebidas', 'https://images.unsplash.com/photo-1523362628745-0c100150b504?w=800&q=85', FALSE, TRUE, 1);

-- usuario_id fica NULL: o vínculo funcionário → login é tarefa da Sprint 4
INSERT IGNORE INTO funcionario (id, nome, cargo, telefone, email, status, escala, data_admissao) VALUES
(1, 'Roberto Silva',  'manager',  '(11) 98765-4321', 'gerente@maka.com',    'active',   'Seg–Sex 09h–18h', '2022-03-01'),
(2, 'Ana Souza',      'kitchen',  '(11) 97654-3210', 'ana.souza@maka.com',  'active',   'Ter–Dom 12h–21h', '2022-06-15'),
(3, 'Carlos Mendes',  'delivery', '(11) 96543-2109', 'entregador@maka.com', 'active',   'Seg–Sáb 18h–23h', '2023-01-10'),
(4, 'Fernanda Lima',  'cashier',  '(11) 95432-1098', 'f.lima@maka.com',     'active',   'Seg–Sex 11h–20h', '2023-04-20'),
(5, 'Paulo Ferreira', 'waiter',   '(11) 94321-0987', 'p.ferreira@maka.com', 'active',   'Qua–Dom 18h–23h', '2023-07-05'),
(6, 'Mariana Costa',  'kitchen',  '(11) 93210-9876', 'm.costa@maka.com',    'on_leave', 'Seg–Sex 08h–17h', '2022-09-12');