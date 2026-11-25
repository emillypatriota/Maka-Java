import { HttpErrorResponse } from '@angular/common/http';

// Lê o campo message do erro padrão do backend ({ status, message })
export function errorMessage(e: unknown, fallback: string): string {
  if (e instanceof HttpErrorResponse && typeof e.error?.message === 'string') {
    return e.error.message;
  }
  return fallback;
}

// Igual ao errorMessage, mas mostra o primeiro campo inválido de um 400 de validação
// ({ status: 400, message: "Dados inválidos", campos: { cnpj: "Informe o CNPJ" } })
export function apiErrorMessage(e: unknown, fallback: string): string {
  if (e instanceof HttpErrorResponse) {
    if (e.status === 0) return 'Sem conexão com o servidor. Tente novamente.';
    const campos = e.error?.campos;
    if (campos && typeof campos === 'object') {
      const primeiro = Object.values(campos)[0];
      if (typeof primeiro === 'string') return primeiro;
    }
    if (typeof e.error?.message === 'string') return e.error.message;
  }
  return fallback;
}
