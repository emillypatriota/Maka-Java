USE maka;

INSERT IGNORE INTO usuario (email, senha_hash, nome, perfil) VALUES
('gerente@maka.com',    '$2a$10$4x2xL1lBcaSerMXVLW5BFOSa4n/4p312GX9FeQmpAqdtogDsMVuma', 'Gerente',    'MANAGER'),
('cozinha@maka.com',    '$2a$10$4x2xL1lBcaSerMXVLW5BFOSa4n/4p312GX9FeQmpAqdtogDsMVuma', 'Cozinha',    'KITCHEN'),
('entregador@maka.com', '$2a$10$4x2xL1lBcaSerMXVLW5BFOSa4n/4p312GX9FeQmpAqdtogDsMVuma', 'Entregador', 'DELIVERY');