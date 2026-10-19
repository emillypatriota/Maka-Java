-- Zera todas as tabelas do banco "maka".
-- Depois de rodar este arquivo, rode o data.sql para recarregar os dados iniciais.
USE maka;

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE item_pedido;
TRUNCATE TABLE pedido;
TRUNCATE TABLE produto_tag;
TRUNCATE TABLE produto;
TRUNCATE TABLE funcionario;
TRUNCATE TABLE usuario;

SET FOREIGN_KEY_CHECKS = 1;