// RN07 no front: mesma conta de dígitos verificadores do backend (Cnpj.java).
// O backend valida de novo; aqui é só para avisar antes de enviar.

const PESOS_1 = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
const PESOS_2 = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];

export function cnpjDigits(value: string | null | undefined): string {
  return (value ?? '').replace(/\D/g, '').slice(0, 14);
}

function digitoVerificador(digits: string, pesos: number[]): number {
  const soma = pesos.reduce((acc, peso, i) => acc + Number(digits[i]) * peso, 0);
  const resto = soma % 11;
  return resto < 2 ? 0 : 11 - resto;
}

export function isValidCnpj(value: string | null | undefined): boolean {
  const d = cnpjDigits(value);
  if (d.length !== 14 || /^(\d)\1{13}$/.test(d)) return false;
  return digitoVerificador(d, PESOS_1) === Number(d[12]) && digitoVerificador(d, PESOS_2) === Number(d[13]);
}

// Máscara enquanto digita: 11.222.333/0001-81
export function formatCnpj(value: string | null | undefined): string {
  const d = cnpjDigits(value);
  let out = d.slice(0, 2);
  if (d.length > 2)  out += '.' + d.slice(2, 5);
  if (d.length > 5)  out += '.' + d.slice(5, 8);
  if (d.length > 8)  out += '/' + d.slice(8, 12);
  if (d.length > 12) out += '-' + d.slice(12, 14);
  return out;
}
