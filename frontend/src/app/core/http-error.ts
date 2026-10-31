import { HttpErrorResponse } from '@angular/common/http';

// Lê o campo message do erro padrão do backend ({ status, message })
export function errorMessage(e: unknown, fallback: string): string {
  if (e instanceof HttpErrorResponse && typeof e.error?.message === 'string') {
    return e.error.message;
  }
  return fallback;
}
