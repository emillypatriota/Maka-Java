-- V6 (Auth): status do usuario para exclusao logica (RN06). Usuario INATIVO nao consegue logar.
ALTER TABLE usuario
  ADD COLUMN status ENUM('ATIVO','INATIVO') NOT NULL DEFAULT 'ATIVO';
