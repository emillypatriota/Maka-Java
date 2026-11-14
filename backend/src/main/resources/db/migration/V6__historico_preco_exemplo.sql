INSERT INTO historico_preco (fornecedor_id, ingrediente_id, preco, registrado_em)
SELECT fp.fornecedor_id,
       fp.ingrediente_id,
       ROUND(fp.preco * (v.fator + MOD(fp.fornecedor_id, 2) * 0.03), 4),
       DATE_SUB(NOW(), INTERVAL v.dias DAY)
FROM fornecedor_produto fp
CROSS JOIN (
  SELECT 28 AS dias, 0.92 AS fator
  UNION ALL SELECT 21, 0.96
  UNION ALL SELECT 14, 0.95
  UNION ALL SELECT 7, 1.03
) v;
