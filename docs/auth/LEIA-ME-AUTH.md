# Auth — Sprint 2 (Gabriel e Fernando)

Cadastro de cliente, rotas protegidas por perfil, guards e telas de login/cadastro.
Feito em cima da `develop` com o Flyway da Base (perfis `ADMIN` e `CLIENTE` já existem no banco). **Não precisa mexer no banco.**

## Como ligar (PowerShell, dentro da pasta do repositório)

```powershell
git checkout develop
git pull origin develop
git checkout -b feat/auth-register-rbac
Expand-Archive -Force -Path $HOME\Downloads\auth-sprint2-v3.zip -DestinationPath .
git status      # deve listar só os arquivos da seção "O que mudou"
```

Docker Desktop aberto ("Engine running"), depois:

```powershell
$env:DB_PASSWORD = "maka123"
docker compose up -d
cd backend
mvn test
mvn spring-boot:run
```

Em outro terminal:

```powershell
cd frontend
npm install
npm start
```

Se o Flyway reclamar de tabela já existente: `docker compose down -v` e suba de novo (apaga o banco local).

## Teste rápido (2 minutos)

| O quê | Esperado |
|---|---|
| `/checkout` sem login | vai para `/entrar?returnUrl=/checkout` |
| Criar conta em `/cadastro` | já fica logado e cai no `/checkout` |
| Cadastrar o mesmo e-mail de novo | "Este e-mail já tem cadastro" (API devolve 409) |
| Cliente logado abrindo `/gerente/insights` | volta para `/` |
| `admin@email.com` / `senha123` em `/login/manager` | entra em `/gerente/insights` |
| `gerente@maka.com` / `123456` em `/login/manager` | entra normal |
| Swagger: `POST /api/auth/login` → botão **Authorize** → colar o token | rotas protegidas funcionam |

PowerShell direto na API:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/register -ContentType 'application/json' -Body '{"name":"Ana","email":"ana@teste.com","password":"senha123"}'
```

## O que mudou

**Backend**
- `auth/AuthController`, `auth/AuthService`: `POST /api/auth/register` (201 com `{token, user}`; e-mail repetido = 409 "E-mail já cadastrado", RN10); e-mail com trim + minúsculo; `/me` com `@PreAuthorize`.
- `auth/PerfilRoles` (novo): `Perfil` → authority (`ROLE_MANAGER`...) e → role do front (`manager`, `client`...).
- `auth/dto/RegisterRequest` (novo): `{name, email, password}` com validação (senha de 6 a 72).
- `config/SecurityConfig`: `@EnableMethodSecurity` + **todas as regras de acesso** (tabela abaixo). Acabou o `anyRequest().permitAll()`.
- `config/JwtFilter`: ignora token com perfil desconhecido.
- `config/JwtAccessDeniedHandler` (novo): 403 em JSON `{status, message}`.
- `config/OpenApiConfig` (novo): botão Authorize no Swagger.
- Testes: `AuthServiceTest` (atualizado), `PerfilRolesTest`, `AuthControllerTest`, `SecurityRulesTest` (confere a tabela de permissões inteira, sem banco).
- `Perfil.java` e o banco **não mudaram**.

**Front (`frontend/src/app`)**
- `services/auth.service.ts`: `register()`, `ready` sempre atual (navega logo depois de logar, sem recarregar), `hasRole()`, `homeRoute()`, sessão vencida → login.
- `core/auth.interceptor.ts`: API respondeu 401 a um token nosso → limpa a sessão, avisa "Sua sessão expirou" e leva para o login da área.
- `guards/auth.guard.ts`: `authGuard` e `roleGuard('manager', ...)` com vários perfis; **ADMIN passa em qualquer área**.
- `pages/auth/client-login` e `pages/auth/register` (novos): rotas `/entrar` e `/cadastro`.
- `components/layout/main-layout`: "Entrar / Criar conta" ou nome + "Sair" no topo.
- `app.routes.ts`: `/checkout`, `/confirmacao`, `/meus-pedidos` com `authGuard`; rotas `/entrar` e `/cadastro`.
- `models/index.ts`: `UserRole` ganha `'admin'` e `'client'`.
- `employee-layout.component.ts`: ADMIN usa o menu do gerente.

## Permissões (SecurityConfig)

ADMIN passa em tudo que o MANAGER passa. MANAGER também passa nas áreas de cozinha e entrega.

| Rota | Quem |
|---|---|
| `POST /api/auth/login`, `/register`, `GET /api/cardapio/**`, Swagger | público |
| `GET /api/auth/me`, `GET /api/pedidos/{id}/status` | qualquer logado |
| `POST /api/pedidos` | CLIENTE, MANAGER, ADMIN |
| `GET /api/pedidos/meus` | CLIENTE |
| `/api/pedidos/**`, `/api/admin/pedidos/**` | KITCHEN, DELIVERY, MANAGER, ADMIN |
| `PATCH /api/pedidos/{id}/entregador` | DELIVERY, MANAGER, ADMIN |
| `POST/PUT/PATCH/DELETE /api/cardapio/**`, `/api/funcionarios/**`, `/api/insights/**` | MANAGER, ADMIN |
| `/api/admin/usuarios/**` | só ADMIN |
| resto de `/api/admin/**` (pratos, ingredientes, funcionários, fornecedores, estoque...) | MANAGER, ADMIN |
| qualquer outra rota | logado |

Sem o SRS na mão, a tabela de `/api/admin/**` é suposição. Se o professor liberar algo diferente, mexa na lista do `SecurityConfig` e na tabela do `SecurityRulesTest` no mesmo PR.

## Arquivos de outros times (revisão da Base/Gestão)

| Arquivo | Dono | Mudança |
|---|---|---|
| `models/index.ts` | Base | `UserRole` ganha `'admin'` e `'client'` |
| `app.routes.ts` | Base | `authGuard` em 3 rotas do cliente + 2 rotas novas (nenhuma rota existente mudou) |
| `employee-layout.component.ts` | Base/Gestão | ADMIN usa o menu do gerente (3 linhas) |
| `SecurityConfig.java` | Auth | regras novas; mantém `/api/admin/**` só para gerência (agora com ADMIN) |
| `AdminCategoriaController`, `AdminProdutoController`, `AdminFuncionarioController`, `IngredienteController`, `FichaTecnicaController` | Gestão | só **2 linhas** cada: `import` + `@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")` na classe |
| `InsightsController` | Insights | idem (2 linhas) |
| `PedidoController` | Trio | `@PreAuthorize` em cada método (cliente, painel, logado) + 1 import |

O PLANO pede `@PreAuthorize` nos endpoints existentes e novos. Quem criar controller novo: anotar na classe ou no método (modelo nos arquivos acima). A regra por URL do `SecurityConfig` continua valendo como primeira trava.

## Avisos para os outros times

- **Trio:** `GET /api/pedidos` agora é só de funcionário. Telas de cliente (`my-orders`, `checkout`) que sobem o `OrdersService` fazem polling nessa rota e vão levar 403 até usarem `/api/pedidos/meus`. No service do pedido, pegue o cliente logado por `Authentication auth` (`auth.getName()` = e-mail) e confira que o pedido é dele em `/pedidos/{id}/status`.
- **Todos:** endpoint novo em `/api/admin/**` já nasce protegido (MANAGER/ADMIN) pela URL. Coloque também o `@PreAuthorize` (sempre incluindo `'ADMIN'`: `hasAnyRole('ADMIN','MANAGER')`).
- **Trio:** se o `PedidoController` de vocês já mudou, mantenham os `@PreAuthorize` dos métodos ao resolver o conflito.

## Falta (Sprint 3 e 4)

CRUD de usuários internos (ADMIN cria gerente/cozinheiro, RF-041): o Flyway V3 já criou a coluna `status` em `usuario`; falta a entidade usar e o login bloquear `INATIVO`. Fornecedores e compras dependem das tabelas da V3 e do SRS.
