import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../models';

// Para onde cada perfil vai quando tenta entrar numa área que não é dele
const ROLE_ROUTES: Record<string, string> = {
  admin:    '/gerente/insights',
  manager:  '/gerente/insights',
  kitchen:  '/cozinha/painel',
  delivery: '/entregador/painel',
  cliente:  '/',
};

// Portal de login de cada área
const LOGIN_ROUTES: Record<string, string> = {
  admin:    '/login/manager',
  manager:  '/login/manager',
  kitchen:  '/login/kitchen',
  delivery: '/login/delivery',
};

export const authGuard: CanActivateFn = async () => {
  const auth   = inject(AuthService);
  const router = inject(Router);
  await auth.ready;
  // Lê o usuário DEPOIS do ready: `ready` resolve uma vez só (no start), então
  // quem logou depois só aparece em getUser().
  if (auth.getUser()) return true;
  return router.createUrlTree(['/portais']);
};

// Área de funcionário: só entra quem tem um dos perfis informados (ex.: roleGuard('manager', 'admin')).
// Sem login vai para o portal do primeiro perfil; logado com perfil errado vai para a área dele.
export function roleGuard(...roles: Exclude<UserRole, null>[]): CanActivateFn {
  return async () => {
    const auth   = inject(AuthService);
    const router = inject(Router);
    await auth.ready;
    const user = auth.getUser();

    if (!user) {
      return router.createUrlTree([LOGIN_ROUTES[roles[0]] ?? '/portais']);
    }
    if (!user.role || !roles.includes(user.role)) {
      return router.createUrlTree([ROLE_ROUTES[user.role ?? ''] ?? '/portais']);
    }
    return true;
  };
}

// Área do cliente (checkout, meus pedidos): exige estar logado e volta para onde estava depois do login.
// Funcionário logado não faz pedido: vai para a área dele.
export const clientGuard: CanActivateFn = async (_route, state) => {
  const auth   = inject(AuthService);
  const router = inject(Router);
  await auth.ready;
  const user = auth.getUser();

  if (!user) {
    return router.createUrlTree(['/entrar'], { queryParams: { redirect: state.url } });
  }
  if (user.role !== 'cliente') {
    return router.createUrlTree([ROLE_ROUTES[user.role ?? ''] ?? '/']);
  }
  return true;
};
