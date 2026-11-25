import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, ROLE_HOME, ROLE_LOGIN } from '../services/auth.service';
import { UserRole } from '../models';

// Qualquer pessoa logada (cliente ou funcionário). Sem login vai para /entrar
// e volta para a página pedida depois de entrar (ex.: /checkout).
export const authGuard: CanActivateFn = async (_route, state) => {
  const auth   = inject(AuthService);
  const router = inject(Router);

  await auth.ready;
  if (auth.getUser()) return true;
  return router.createUrlTree(['/entrar'], { queryParams: { returnUrl: state.url } });
};

// Só os perfis listados (ADMIN passa em qualquer área, como no backend).
// Ex.: roleGuard('manager')  |  roleGuard('kitchen', 'delivery')
export function roleGuard(...roles: UserRole[]): CanActivateFn {
  return async () => {
    const auth   = inject(AuthService);
    const router = inject(Router);

    await auth.ready;
    const user = auth.getUser();

    if (!user) {
      return router.createUrlTree([ROLE_LOGIN[roles[0] ?? 'manager'] ?? '/portais']);
    }
    if (user.role === 'admin' || roles.includes(user.role)) {
      return true;
    }
    // Perfil errado para esta área (ex.: cliente tentando abrir /gerente): volta para a casa dele
    return router.createUrlTree([ROLE_HOME[user.role ?? ''] ?? '/portais']);
  };
}
