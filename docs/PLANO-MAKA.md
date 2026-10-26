# MAKA — Plano de Migração (versão atualizada em 19/10/2026)

> Firebase + Angular → **Java Spring Boot + MySQL**, mantendo o front Angular.
> Documento único de referência do grupo. Leia inteiro antes de começar qualquer tarefa.

---

## 0. Regras de ouro do grupo

1. **Leia este documento inteiro antes de começar.** Ele tem o contrato da API, as decisões do grupo e os erros que já aconteceram.
2. **Trabalhe só nas tarefas e nos arquivos do seu time** (seções 2 e 7.3). Precisa mexer no arquivo de outro time? Peça por issue ou PR marcando o dono.
3. **Nada de inventar rota, campo de JSON ou coluna.** Use o contrato (seção 6) e o `schema.sql`.
4. **Respeite as armadilhas da seção 4.** Em especial: não usar `@AllArgsConstructor`, `@NoArgsConstructor` nem `@Builder` do Lombok, e controller nunca devolve entidade, sempre DTO.
5. **Só diga que está pronto depois de testar:** o backend precisa compilar, subir e responder no Swagger; o front precisa funcionar no navegador.
6. **Sem senha no código**, sem `System.out.println` nem `console.log` esquecido.
7. **Travou ou achou algo estranho no plano?** Avise no grupo em vez de resolver sozinho em silêncio.

---

## 1. O projeto e o objetivo

**MAKA** é um sistema de delivery para uma *dark kitchen* (cozinha sem salão). O cliente monta o pedido no site; a cozinha vê e prepara; o entregador leva; o gerente administra cardápio, funcionários e vê relatórios.

**Projeto antigo (ponto de partida):**
- Repositório original: https://github.com/KauanMedeirosz/MakaFinal (o Kauan, do grupo, é o autor).
- Stack antiga: **Angular 17** + **Firebase** (Firebase Auth para login, Firestore para pedidos, cardápio e funcionários, com `onSnapshot` para tempo real).
- Site antigo no ar: https://maka-delivery-7382b.firebaseapp.com/
- Logins antigos: `gerente`, `cozinha`, `entregador` com senha `maka2024` (viviam num `ROLE_MAP` fixo no código do front).
- Esse projeto **já funciona e já tem todas as telas prontas**. Não vamos refazer o front.

**Objetivo do trabalho (projeto novo):**
Transformar o projeto antigo em um sistema **sem Firebase**, em que:
- O **Angular continua** (mesmas telas, rotas e componentes), mas passa a falar **HTTP com uma API REST**;
- A API é feita em **Java 17 + Spring Boot 3.5.6** (Maven);
- Os dados ficam num **banco MySQL 8** (relacional);
- O login usa **JWT** com senhas em **BCrypt**, e o perfil vem do banco.

**Repositório novo (é aqui que todos trabalham):**
- GitHub: https://github.com/emillypatriota/Maka-Java
- Pastas: `frontend/` (Angular 17, copiado do MakaFinal) e `backend/` (Spring Boot).
- Branches: `main` (protegida, só recebe PR) e `develop` (branch de trabalho).
- Cada tarefa vai numa branch própria a partir da `develop` (`feat/auth-login`, `fix/token-expirado`...).

**Arquitetura:** `Angular (frontend/)  →  HTTP/JSON  →  Spring Boot (backend/)  →  JPA  →  MySQL (Docker)`

**Entrega final:** sexta 27/11/2026, com tag `v1.0` na `main` e apresentação com demo ao vivo.

---

## 2. Quem faz o quê (resumo)

| Time | Integrantes | Missão |
|---|---|---|
| **Dupla Base** | Emilly e Letícia | Projeto Spring Boot, banco, entidades, repositories, segurança base, front base. Donas da `develop`. |
| **Dupla Auth** | Gabriel e Fernando | JWT, login, `/auth/me`, perfis, guards, interceptor, "meus pedidos" |
| **Dupla Gestão** | Mauricio e Kauan | Cardápio e funcionários (backend + front), seed dos dados |
| **Trio Pedidos** | Lucas, Enzo e Enzo Cruz | Pedido do checkout até "entregue": total no servidor, máquina de status, painéis da cozinha e do entregador |
| **Dupla Insights** | Murilo e Guilherme | Consultas de relatório, tela de insights, Swagger, README, roteiro de testes |

Quem depende de quem: **Base → Auth, Gestão → Pedidos → Insights**. A Base já entregou o que destrava os outros.

---

## 3. PASSO 0 — Preparar o computador (todos, uma vez só)

### 3.1 Instalar
1. **JDK 17 (Temurin)** — durante a instalação, marque a opção que configura o **JAVA_HOME**. Confira com `java -version` (deve mostrar 17).
2. **Docker Desktop** — deixe aberto sempre que for rodar o banco.
3. **VS Code** com a extensão **Extension Pack for Java**. O Maven já vem embutido nela.
4. **Node.js** (LTS) — traz o `npm`, usado pelo front.
5. **Git**.

### 3.2 Baixar o código
Use o terminal (no Windows, `cmd` ou o terminal do VS Code). **Um comando por linha:**
```
git clone -b develop https://github.com/emillypatriota/Maka-Java.git
cd Maka-Java
git checkout develop
```
(Você precisa ter **aceitado o convite** de colaborador no GitHub. Ele chega por e-mail e em github.com/notifications.)

### 3.3 Criar o arquivo `.env`
Na pasta principal (a mesma do `docker-compose.yml`), crie um arquivo chamado `.env` com **uma única linha**:
```
DB_PASSWORD=maka123
```
Esse arquivo **não vai pro Git de propósito** (está no `.gitignore`). Cada um cria o seu.

### 3.4 Subir o banco MySQL
Com o Docker Desktop aberto, na pasta onde está o `docker-compose.yml`:
```
docker compose up -d
```
> Se aparecer `no configuration file provided: not found`, você está na pasta errada. Entre na pasta do repositório (`cd Maka-Java`).

Na primeira vez, o MySQL executa sozinho o `schema.sql` (cria as tabelas) e o `data.sql` (cria os 3 logins). **Isso só acontece quando o volume é criado.** Para zerar tudo:
```
docker compose down -v
docker compose up -d
```
Ou, sem apagar o volume, rode o `backend/src/main/resources/reset.sql` (limpa as tabelas) e depois reinsira o `data.sql`.

### 3.5 Rodar o backend
1. Abra a pasta do repositório no VS Code (espere o "Java: Ready" na barra de baixo).
2. Abra `backend/src/main/java/com/maka/api/MakaApiApplication.java` e clique em **Run**.
3. Deu certo quando aparecer `Started MakaApiApplication`.
4. Teste no navegador: **http://localhost:8080/swagger-ui.html**

### 3.6 Rodar o front
Em **outro** terminal:
```
cd frontend
npm install
npm start
```
Abra **http://localhost:4200**. (O `npm install` demora uns minutos e mostra avisos amarelos: é normal.)

### 3.7 Logins de teste (senha `123456` para os três)
| E-mail | Perfil (banco) | role no front |
|---|---|---|
| gerente@maka.com | MANAGER | manager |
| cozinha@maka.com | KITCHEN | kitchen |
| entregador@maka.com | DELIVERY | delivery |

### 3.8 Todo dia, antes de começar
```
git checkout develop
git pull origin develop
git checkout -b feat/nome-da-tarefa
```

---

## 4. Descobertas e armadilhas (erros que JÁ aconteceram, não repitam)

**Lombok**
- `@AllArgsConstructor`, `@NoArgsConstructor` e `@Builder` **quebram no VS Code**: dão erro "Flexible Constructor Bodies is only available with Java 22" e podem gerar `.class` corrompido (`ClassFormatError: Invalid method Code length 0`). **Usem só `@Getter` e `@Setter`** e o construtor padrão implícito.
- Se o projeto ficar com erros estranhos depois de mexer no Lombok: apague a pasta `backend/target`, e no VS Code use `Ctrl+Shift+P` → **Java: Clean Java Language Server Workspace** → **Restart and delete**.

**JPA / banco**
- `spring.jpa.hibernate.ddl-auto=validate`: **quem cria o banco é o `schema.sql`, não o Hibernate.** Se mudar uma entidade e não mudar o `schema.sql`, o backend não sobe.
- Mudança no banco **só por PR avisado no grupo** (Base é dona do `schema.sql`). Ninguém altera tabela direto no MySQL local.
- `open-in-view=false` e relacionamentos `LAZY`: para trazer relacionamentos, use `@EntityGraph` no repository (o `PedidoRepository` já faz isso para `itens` e `itens.produto`). Sem isso dá `LazyInitializationException`.
- **Controller nunca devolve entidade, sempre DTO.** Entidade com `@OneToMany` indo direto para o JSON gera loop infinito.
- Dinheiro: `BigDecimal` no Java e `DECIMAL(10,2)` no banco. Nunca `double`.
- O `data.sql` usa `INSERT IGNORE` e começa com `USE maka;`.

**Enums**
- Os enums de `Cargo`, `StatusFuncionario`, `StatusPedido`, `TipoPedido` e `FormaPagamento` têm constantes **em minúsculo de propósito** (`pending`, `credit`, `delivery`...), iguais ao banco e ao front (`models/index.ts`). Só `Perfil` é maiúsculo (`MANAGER`, `KITCHEN`, `DELIVERY`) e é convertido para minúsculo no DTO do login.

**Segurança**
- Hoje o `SecurityConfig` é stateless, sem CSRF/formLogin/httpBasic, com CORS lendo `maka.cors.allowed-origins` (`http://localhost:4200`) e um bean `BCryptPasswordEncoder`.
- Sem token, rotas protegidas devolvem **403** (não 401), porque ainda não há *entry point*. A Auth acerta isso (401 = não logado, 403 = sem permissão).
- Senha do banco só em variável de ambiente (`${DB_PASSWORD}`). Nunca commitar `.env`.

**Windows / ferramentas**
- No `cmd`, **um comando por linha**. `docker compose` precisa ser rodado na pasta que tem o `docker-compose.yml`.
- O front mostra 2 erros vermelhos no `tsconfig.json` ("`baseUrl` foi preterida... TypeScript 7.0"). **Pode ignorar**, não quebra nada (vem do projeto antigo).
- Avisos de `LF will be replaced by CRLF` no Git: **normais**, ignorem.
- Se o front mostrar "Não é possível localizar o módulo '@angular/...'": falta rodar `npm install` dentro de `frontend/`.
- Só o MySQL do Docker é usado; não instalem outro MySQL.

**Dependências já no `pom.xml` (Base)**
Spring Web, Data JPA, Security, Validation, MySQL Driver, Lombok, **JJWT 0.12.6**, **springdoc-openapi 2.8.9** (Swagger). Dependência nova só com aprovação da Base.

---

## 5. Estado atual do repositório (o que já existe)

### 5.1 Pastas do backend (`backend/src/main/java/com/maka/api/`)
```
MakaApiApplication.java
config/        SecurityConfig.java            (Base; JwtFilter será da Auth)
common/        GlobalExceptionHandler.java    (Base)
model/         entidades e enums              (Base)
repository/    repositories JPA               (Base)
```
Cada time cria o **próprio pacote** para controller/service/dto, usando as entidades e repositories que já existem:
`auth/`, `produto/` (ou `cardapio/`), `funcionario/`, `pedido/`, `insights/` — cada um com `XController`, `XService` e `dto/`.
> A Auth **não recria** `Usuario`/`Perfil`: usa `model.Usuario`, `model.Perfil` e `UsuarioRepository`.

### 5.2 Entidades (`com.maka.api.model`)
`Usuario` (id, email, senhaHash, nome, perfil, criadoEm) · `Funcionario` (nome, cargo, telefone, email, status, escala, dataAdmissao, observacoes, usuario, criadoEm) · `Produto` (nome, descricao, preco, categoria, imagem, destaque, ativo, tempoPreparo, calorias, tags, criadoEm) · `Pedido` (dados do cliente, formaPagamento, tipo, status, taxaEntrega, total, observacoes, numeroMesa, tempoEstimado, motivoCancelamento, entregador, itens, criadoEm, atualizadoEm) · `ItemPedido` (pedido, produto, quantidade, precoUnitario, observacoes) · enums `Perfil`, `Cargo`, `StatusFuncionario`, `StatusPedido`, `TipoPedido`, `FormaPagamento`.

### 5.3 Repositories (`com.maka.api.repository`)
- `UsuarioRepository`: `findByEmail`, `existsByEmail`
- `FuncionarioRepository`: `findByStatus`
- `ProdutoRepository`: `findByAtivoTrue`, `findByCategoria`
- `PedidoRepository`: `findAllByOrderByCriadoEmDesc` (com `@EntityGraph`), `findByStatusOrderByCriadoEmDesc`
- `ItemPedidoRepository`

### 5.4 `GlobalExceptionHandler` (já trata)
Validação → 400 (com mapa de campos), JSON ilegível → 400, `IllegalArgumentException` → 400, `BadCredentialsException` → 401, `AccessDeniedException` → 403, `NoSuchElementException` → 404, violação de integridade → 409, qualquer outro → 500.

### 5.5 Arquivos importantes
`backend/pom.xml` · `backend/src/main/resources/application.properties` · `schema.sql` (6 tabelas + `produto_tag`) · `data.sql` (3 logins) · `reset.sql` · `docker-compose.yml` (MySQL 8) · `.env` (local, fora do Git) · front: `frontend/src/environments/environment.ts` (já com `apiUrl: 'http://localhost:8080/api'`), `environment.prod.ts` (`apiUrl: '/api'`) e `frontend/src/main.ts` (já com `provideHttpClient(withInterceptors([]))`; a Auth coloca o `authInterceptor` dentro desse `[]`).

### 5.6 Telas do front (não mudam) e rotas
`/` Home · `/cardapio` · `/carrinho` · `/checkout` · `/meus-pedidos` · `/portais` · `/login/manager` · `/login/kitchen` · `/login/delivery` · `/gerente/dashboard` · `/gerente/cozinha` · `/gerente/insights` · `/cozinha/painel` · `/entregador/painel`

### 5.7 Onde estava o Firebase (só 5 arquivos)
| Arquivo | Usava | Vira |
|---|---|---|
| `services/auth.service.ts` | Firebase Auth + `ROLE_MAP` fixo | `POST /api/auth/login` com JWT e tabela `usuario` |
| `services/orders.service.ts` | coleção `orders` com `onSnapshot` | `/api/pedidos` + polling de 5 s |
| `services/menu.service.ts` | coleção `menu` + seed de 27 pratos | `/api/cardapio`; seed vira SQL |
| `pages/employee/staff/staff-management.component.ts` | coleção `staff` direto no componente | `/api/funcionarios` + novo `StaffService` |
| `core/firebase/firebase.config.ts` + `environments/*` | chaves do Firebase | apagar; `environment.apiUrl` |

Pontos de atenção do projeto antigo: o `orderNumber` era calculado no navegador (`orders.length + 1`), o que repete número — no banco vira o `id` do pedido (AUTO_INCREMENT); o pedido guardava o item inteiro (nome, preço, imagem), mas no banco o `item_pedido` guarda só `produto_id`, quantidade e o **preço no momento da compra**.

---

## 6. Banco e contrato da API

### 6.1 Tabelas (já criadas no `schema.sql`)
`usuario` · `funcionario` · `produto` · `produto_tag` · `pedido` · `item_pedido`
Índices em `pedido`: `idx_status (status)` e `idx_criado (criado_em)`. `ddl-auto=validate`: o schema é a verdade.

### 6.2 Os 18 endpoints
| Método | Rota | Acesso | Substitui no Angular |
|---|---|---|---|
| POST | `/api/auth/login` | público | `auth.loginWithEmail()` |
| GET | `/api/auth/me` | logado | `onAuthStateChanged` / `auth.ready` |
| GET | `/api/cardapio` | público | listener do menu |
| POST | `/api/cardapio` | MANAGER | `menuService.addItem()` |
| PUT | `/api/cardapio/{id}` | MANAGER | `menuService.updateItem()` |
| PATCH | `/api/cardapio/{id}/ativo` | MANAGER | `menuService.toggleItem()` |
| DELETE | `/api/cardapio/{id}` | MANAGER | `menuService.deleteItem()` |
| GET | `/api/pedidos` | funcionários | listener de orders |
| GET | `/api/pedidos?ids=1,2,3` | público | `getMyOrders()` (ids no localStorage) |
| POST | `/api/pedidos` | público | `orders.addOrder()` |
| PATCH | `/api/pedidos/{id}/status` | MANAGER, KITCHEN, DELIVERY | `ordersService.updateStatus()` |
| PATCH | `/api/pedidos/{id}/entregador` | DELIVERY | `ordersService.assignDelivery()` |
| GET | `/api/funcionarios` | MANAGER | listener de staff |
| POST | `/api/funcionarios` | MANAGER | `addDoc(staff)` |
| PUT | `/api/funcionarios/{id}` | MANAGER | `updateDoc(staff)` |
| PATCH | `/api/funcionarios/{id}/status` | MANAGER | `toggleStatus()` |
| DELETE | `/api/funcionarios/{id}` | MANAGER | `remove()` |
| GET | `/api/insights/resumo` | MANAGER | opcional (hoje o painel calcula no front) |

### 6.3 Exemplos de corpo
```jsonc
// POST /api/auth/login
{ "email": "gerente@maka.com", "password": "123456" }
// → 200
{ "token": "eyJ...", "user": { "uid": "1", "email": "gerente@maka.com", "role": "manager", "username": "Gerente" } }

// POST /api/pedidos
{
  "items": [ { "id": "12", "quantity": 2, "observations": "ao ponto" } ],
  "customer": { "name": "Ana", "phone": "11999999999", "address": "Rua X, 10" },
  "paymentMethod": "pix",
  "deliveryFee": 8.00
}
// → 201 com o Order completo; total e preços calculados NO SERVIDOR
```

### 6.4 Regras do backend
- **JSON em inglês igual ao front** (`orderNumber`, `createdAt`, `deliveryPersonId`, `isSpecial`, `active`, `prepTime`, `image`, `category`); a tradução português → inglês acontece **só nos DTOs**. Antes de criar um DTO, **confira o model correspondente em `frontend/src/app/models/index.ts`** e use exatamente os mesmos nomes.
- `id` volta como **string** no JSON (`"12"`) — `String.valueOf(id)` no DTO.
- O servidor **recalcula o total** pelo preço do banco; o total que vem do front é ignorado.
- Status: `pending → confirmed → preparing → ready → delivering → delivered`; `cancelled` só antes de `delivering`. Transição inválida → **409**.
- **Formato de erro padrão:** `{ "status": 400, "message": "..." }`. O `ToastService` do front mostra o campo `message`.
- CORS liberado só para `http://localhost:4200`.
- Nenhuma resposta devolve `senha_hash`.

---

## 7. Como trabalhar junto

### 7.1 Ciclo de cada tarefa
1. **Contrato primeiro:** DTOs de Request/Response vão em PR cedo; front e back do time trabalham em cima deles.
2. **Front não espera back:** enquanto o endpoint não existe, o service Angular devolve dado fixo no formato do DTO (`of([...])`); quando o endpoint fica pronto, troca só aquela linha por `http.get`.
3. Branch por tarefa a partir da `develop`, com `git pull origin develop` todo dia antes de começar.
4. **PR pequeno:** um endpoint ou uma tela por PR.
5. **Revisão por alguém de outro time** (não pela própria dupla) e merge só até **quinta**.
6. Quinta à noite: a Base baixa a `develop` limpa, sobe tudo e avisa no grupo se algo quebrou e de quem é.
7. Sexta: a demo roda da `develop`, nunca da máquina de alguém. Só conta como entregue o que funcionar lá.

### 7.2 Regras de Git
- **Nunca push direto no `main`** (protegido por regra do GitHub; só via PR).
- `develop` é a branch de trabalho. Commits no padrão: `feat(pedidos): valida transição de status`, `fix(auth): token expirado`.
- Nada de `node_modules/`, `dist/`, `.angular/`, `.env` ou senha no repositório.
- PR que mexer em `config/`, `pom.xml`, `package.json`, `models/index.ts` ou `app.routes.ts` **também precisa da revisão da Dupla Base**.

### 7.3 Arquivos com dono (quem não é dono pede por issue ou PR marcando o dono)
| Arquivo | Dono | Regra |
|---|---|---|
| `models/index.ts` | Base | pode adicionar campo; renomear/remover só com aviso no grupo |
| `schema.sql`, `data.sql` | Base | outros times pedem por issue (a Gestão adiciona os INSERTs do seed por PR) |
| `pom.xml`, `package.json` | Base | dependência nova só com aprovação |
| `SecurityConfig` | Auth (a Base montou a primeira versão) | rota nova pública só com a Auth sabendo |
| `app.routes.ts` | Base | as rotas atuais não mudam |
| `README.md` | Insights | todo mundo atualiza, a Insights revisa |

### 7.4 Padrões
- Classes Java em português do domínio (`Pedido`, `Produto`); JSON em inglês igual ao front.
- Erros no formato da seção 6.4.
- Dinheiro com `BigDecimal`/`DECIMAL(10,2)`.
- Sem `System.out.println` / `console.log` no PR.

### 7.5 Quando uma tarefa está pronta
Compila, roda na `develop`, o JSON bate com o contrato, a tela usa a API (não dado fixo) e o PR foi revisado por outro time.

### 7.6 Comunicação
- **Daily no grupo até as 10h:** o que fiz, o que vou fazer, onde travei.
- Travou por mais de um dia: avisar a Dupla Base, que redistribui ou puxa ajuda.
- Mudança de contrato: mensagem no grupo marcando o time afetado **antes** do PR.

---

## 8. Cronograma atualizado (6 sprints)

> **O que mudou:** as sprints 1, 2 e 3 do plano original viraram **uma só: a nova Sprint 1**, com prazo até **domingo 25/10** (na segunda 26/10 começa a Sprint 2). As antigas 4 a 8 viraram as Sprints 2 a 6, **com as mesmas datas de antes**.
> Ritual: planejamento na segunda, demo de 15 min na sexta, merge só até quinta (exceto a Sprint 1, que fecha no domingo).

| Sprint nova | Era (plano original) | Datas | Meta |
|---|---|---|---|
| **1 — Fundação + Base + Front lendo a API** | 1 + 2 + 3 | 19/10 a **25/10** | Todas as telas mostram dados do MySQL e os 3 perfis fazem login pelo backend |
| **2 — Escrita** | 4 | 26/10 a 30/10 | Gerente edita cardápio e funcionários; cliente finaliza pedido pelo checkout |
| **3 — Fluxo completo do pedido** | 5 | 03/11 a 06/11 (Finados 02/11) | Pedido vai do cliente à cozinha, entregador e "entregue" **sem Firebase** |
| **4 — Insights e segurança** | 6 | 09/11 a 13/11 | Painel do gerente com números do backend; erros aparecem no toast |
| **5 — Testes em conjunto** | 7 | 16/11 a 19/11 (feriado 20/11) | Os 11 testam juntos; bugs críticos corrigidos; tag `v1.0-rc` |
| **6 — Entrega final** | 8 | 23/11 a 27/11 | Tag `v1.0` na `main` e apresentação com demo ao vivo |

---

### SPRINT 1 — até domingo 25/10 (soma das antigas 1, 2 e 3)

**Meta:** todas as telas mostram dados do MySQL e os 3 perfis fazem login pelo backend.

#### Dupla Base — Emilly e Letícia (legenda: [x] feito · [ ] falta)

Já entregue (pushed na `develop`):
- [x] Pastas `frontend/` e `backend/`; `node_modules/` fora do Git (conferido no `git status`)
- [x] Branch `develop` criada; regra do GitHub protegendo o `main` (PR obrigatório, criada em 19/10)
- [x] `schema.sql` com as 6 tabelas + `produto_tag` e `docker-compose.yml` com MySQL 8
- [x] Projeto Spring Boot 3.5.6 / Java 17 / Maven (`pom.xml` com Web, JPA, Security, Validation, MySQL, Lombok, JJWT, springdoc)
- [x] `application.properties` com variáveis de ambiente (`${DB_PASSWORD}`), `ddl-auto=validate`, `open-in-view=false`, CORS e JWT
- [x] `SecurityConfig` (stateless, CORS, BCrypt) e `GlobalExceptionHandler`
- [x] Entidades JPA e enums das 6 tabelas; repositories
- [x] `data.sql` com os 3 logins em BCrypt (senha 123456) e `reset.sql`
- [x] Swagger abrindo em `/swagger-ui.html`; backend sobe conectado ao MySQL
- [x] Front: `environment.apiUrl` (dev e prod) e `provideHttpClient(withInterceptors([]))` no `main.ts`
- [x] Todos os integrantes adicionados como colaboradores; Letícia já aceitou o convite

**Falta fazer (em ordem):**
1. [x] **Trocar o campo de erro `mensagem` por `message`** no `GlobalExceptionHandler` (o contrato e o `ToastService` esperam `message`).
2. [x] **`SecurityConfig`: liberar tudo temporariamente** (`anyRequest().permitAll()`), como manda o plano: os outros times precisam testar os endpoints sem token até a Auth proteger (Sprint 2). Hoje tudo devolve 403 para quem não tem token.
3. [x] **`SecurityConfig`: trocar a rota pública `/api/produtos/**` por `/api/cardapio/**`** (o contrato usa `/api/cardapio`).
4. [x] Conferir no `schema.sql` os índices `idx_status` e `idx_criado` em `pedido`.
5. [x] Conferir se `dist/` e `.angular/` estão no `frontend/.gitignore`.
6. [x] Revisar `models/index.ts` (status e valores em minúsculo, iguais aos enums) e `app.routes.ts` (rotas atuais não mudam).
7. [ ] **Letícia: teste do zero** — clonar o repositório numa pasta nova e seguir só o Passo 0 deste arquivo. Se subir sem ajuda, o critério "pronto" da Base está cumprido.
8. [ ] Opcional: aprovações obrigatórias = 1 na regra do `main`; deixar a `develop` como branch padrão do repositório.
9. [ ] Subir este arquivo em `docs/PLANO-MAKA.md` e criar um `CLAUDE.md` na raiz (resumo das regras da seção 4).
10. [ ] Opcional: `CODEOWNERS` (pede revisão da Base só nos arquivos de Base) e modelo de PR com checklist (compila, sobe, sem senha, DTO e não entidade).
11. [ ] Revisar e dar merge nos PRs dos outros times (ver seção 7.2).

#### Dupla Auth — Gabriel e Fernando
Entregas: login com JWT funcionando e `/auth/me`; front com `auth.service`, interceptor e guards.
1. [ ] Fazer o Passo 0 e criar a branch `feat/auth-jwt`.
2. [ ] `JwtService` (pacote `auth/`): gerar e validar token com JJWT 0.12.6, usando `maka.jwt.secret` e `maka.jwt.expiration-ms` que já estão no `application.properties`.
3. [ ] DTOs: `LoginRequest { email, password }` e `LoginResponse { token, user { uid, email, role, username } }`. **`uid` = id do usuário como string; `role` em minúsculo** (`manager`, `kitchen`, `delivery`) — converte do enum `Perfil` no DTO.
4. [ ] `AuthService`: `UsuarioRepository.findByEmail` + `PasswordEncoder.matches`. Senha errada → `BadCredentialsException` (o handler já devolve 401).
5. [ ] `AuthController`: `POST /api/auth/login` e `GET /api/auth/me`.
6. [ ] `config/JwtFilter` (lê o `Authorization: Bearer`, valida e preenche o usuário) e registro no `SecurityConfig`. **Até a Sprint 2 as rotas continuam liberadas**; a proteção só entra com aviso no grupo com 2 dias de antecedência.
7. [ ] Front: `core/auth.interceptor.ts` (anexa `Authorization: Bearer <token>` do `localStorage`) e colocar dentro do `withInterceptors([...])` do `main.ts` (PR marcando a Base).
8. [ ] Front: `auth.service.ts` — `loginWithEmail` chama `POST /auth/login` e guarda o token; o construtor chama `GET /auth/me` para resolver `ready`; remover o `ROLE_MAP`.
9. [ ] `auth.ready` continua sendo uma **Promise que resolve uma vez só**; sem token salvo, resolve `null` na hora, sem chamar a API. Guards e `ROLE_ROUTES` esperam por ela.
10. [ ] Guards e telas de login dos 3 perfis funcionando: cada perfil entra no seu painel.
**Pronto quando:** os 3 logins entram cada um no seu painel e `GET /api/auth/me` devolve o usuário com o token.

#### Dupla Gestão — Mauricio e Kauan
Entregas: GET do cardápio e dos funcionários no formato do front; seed no banco; front lendo a API.
1. [ ] Fazer o Passo 0 e criar a branch `feat/gestao-leitura`.
2. [ ] DTOs de produto e de funcionário **com os campos do `models/index.ts`** (`MenuItem` e o modelo de staff): JSON com `isSpecial`, `active`, `prepTime`, `image`, `category`; `id` como string.
3. [ ] **Seed:** converter `INITIAL_MENU` (27 pratos) e `SEED_STAFF` em `INSERT` no `data.sql` (PR marcando a Base). As categorias devem ter o **texto exato** de `CATEGORIES` no front (`Carnes Nobres`, `Peixes & Frutos do Mar`...), senão o filtro do cardápio fica vazio. Lembrar de preencher `produto_tag` para as tags.
4. [ ] `ProdutoService`/`ProdutoController`: `GET /api/cardapio`.
5. [ ] `FuncionarioService`/`FuncionarioController`: `GET /api/funcionarios`.
6. [ ] Front: `menu.service.ts` vira `http.get('/cardapio')` mantendo a assinatura pública (`items$`, `loaded$`); remover `seedMenu`.
7. [ ] Front: criar `services/staff.service.ts` no mesmo formato do `menu.service` (signal com a lista) e tirar o Firestore de `staff-management.component.ts` (o componente só chama o service); remover o `seed()` de funcionários.
8. [ ] Home, cardápio e lista de funcionários sem Firestore.
**Pronto quando:** a home mostra os 27 pratos vindos do MySQL e a lista de funcionários vem da API.

#### Trio Pedidos — Lucas, Enzo e Enzo Cruz
Divisão: **Pessoa 1** backend (`pedido/`) · **Pessoa 2** front do cliente (`orders.service`, checkout, confirmação) · **Pessoa 3** front dos funcionários (`employee/kitchen`, `employee/orders`, `delivery/dashboard`).
1. [ ] Fazer o Passo 0 e criar a branch `feat/pedidos-leitura`.
2. [ ] Desenhar o diagrama da máquina de status (`pending → ... → delivered`, `cancelled`).
3. [ ] **(P1)** DTOs `OrderRequest` e `OrderResponse` **espelhando o `Order` do front**: `items[].name` e `image` (juntando `item_pedido` com `produto`), `customer` como objeto, `createdAt` em ISO, `orderNumber` = `String.format("%04d", id)`, `deliveryPersonId` `null` quando não há entregador.
4. [ ] **(P1)** `GET /api/pedidos` ordenado por data decrescente (usar `findAllByOrderByCriadoEmDesc`, que já tem `@EntityGraph`), `GET /api/pedidos?ids=1,2,3`, e o esqueleto de `POST /api/pedidos`.
5. [ ] **(P2)** `orders.service.ts` com polling de 5 s mantendo `shareReplay` (**um polling só para o app inteiro**):
```ts
readonly orders: Observable<Order[]> = timer(0, 5000).pipe(
  switchMap(() => this.http.get<Order[]>(`${environment.apiUrl}/pedidos`)),
  map(list => list.map(o => ({ ...o, createdAt: new Date(o.createdAt) }))),
  tap(list => this._orders.set(list)),
  shareReplay({ bufferSize: 1, refCount: true })
);
```
6. [ ] **(P3)** Painéis de pedidos e da cozinha lendo a API.
7. [ ] Testar só com os pratos do `data.sql` (nunca criar produto pelo código de pedidos).
**Pronto quando:** os painéis mostram os pedidos do MySQL, atualizando a cada 5 s.

#### Dupla Insights — Murilo e Guilherme
Entregas: board, README inicial, consultas SQL e `GET /api/insights/resumo`.
1. [ ] Fazer o Passo 0.
2. [ ] Criar o **board de tarefas no GitHub** com as issues das 6 sprints (uma por linha da tabela de endpoints) e os 5 times.
3. [ ] **README inicial** ("como rodar"), baseado no Passo 0 deste arquivo.
4. [ ] **Começar pela tela, não pelo SQL:** listar cada número e gráfico da tela de insights (hoje calculados no front) e **desenhar o JSON** do `/insights/resumo`.
5. [ ] Consultas SQL: faturamento por dia (`WHERE status <> 'cancelled'` + `GROUP BY DATE(criado_em)`), ticket médio e pratos mais vendidos. Até existirem pedidos reais, inserir pedidos de exemplo no banco local.
6. [ ] `InsightsService`/`InsightsController`: `GET /api/insights/resumo` (o springdoc/Swagger já está no `pom.xml`; é só documentar).
**Pronto quando:** `GET /api/insights/resumo` devolve números que batem com uma consulta feita à mão no MySQL.

---

### SPRINT 2 — 26/10 a 30/10 (era a Sprint 4): Escrita
**Meta:** o gerente edita cardápio e funcionários, e o cliente finaliza um pedido pelo checkout.

| Time | Entrega |
|---|---|
| **Base** | Primeira integração completa na `develop`; conferir que o `preco_unitario` fica congelado no `item_pedido` |
| **Auth** | `@PreAuthorize` em todos os endpoints de escrita (e tirar o `permitAll` geral, **avisando o grupo 2 dias antes**); logout; token expirado leva ao login; rotas públicas: `GET /cardapio`, `POST /pedidos`, `GET /pedidos?ids=`, `/auth/login` e Swagger; 401 → interceptor limpa o token e vai para `/portais`; 403 → toast "sem permissão" sem deslogar |
| **Gestão** | `POST`, `PUT`, `PATCH`, `DELETE` de cardápio e funcionários; telas de gestão usando a API; depois de salvar, **recarregar a lista** (o `onSnapshot` fazia isso sozinho); prato já usado em pedido → desativar ou devolver 409 ("prato já usado em pedidos, desative em vez de excluir") |
| **Trio Pedidos** | `POST /api/pedidos` com **total calculado no servidor** (preço do banco × quantidade + taxa), `@Transactional`, `preco_unitario` gravado; checkout e confirmação usando a API |
| **Insights** | Tela de insights consumindo a API, com os gráficos Chart.js alimentados pelo backend (mesmos gráficos, só muda a origem dos dados) |

### SPRINT 3 — 03/11 a 06/11 (era a Sprint 5; Finados 02/11): Fluxo completo do pedido
**Meta:** um pedido vai do cliente à cozinha, ao entregador e a "entregue" sem nenhuma chamada ao Firebase.

| Time | Entrega |
|---|---|
| **Base** | **Remover o Firebase**: `firebase` do `package.json`, pasta `core/firebase/`, `firebase.json`, `.firebaserc`, `FIREBASE_SETUP.md`; build limpo (`npm run build`) |
| **Auth** | "Meus pedidos" sem login: ids dos pedidos salvos no `localStorage` + `GET /api/pedidos?ids=` |
| **Gestão** | Bean Validation (`@NotBlank`, `@Positive`) e mensagens de erro no `ToastService` |
| **Trio Pedidos** | `PATCH /status` com validação de transição (409) e `PATCH /entregador`; painéis da cozinha e do entregador completos (o filtro `status === 'ready' && !deliveryPersonId` continua no front) |
| **Insights** | Filtro por período (hoje, 7 dias, 30 dias) nos insights |

### SPRINT 4 — 09/11 a 13/11 (era a Sprint 6): Insights e segurança
**Meta:** painel do gerente com números do backend e todos os erros aparecendo no toast com mensagem clara.

| Time | Entrega |
|---|---|
| **Base** | Revisão de arquitetura: nenhum componente chamando HTTP direto, só services; padronizar nomes |
| **Auth** | Revisão de segurança: senha nunca no JSON, 401 vs 403 corretos, CORS restrito |
| **Gestão** | Criar login para funcionário (vínculo `funcionario → usuario`) |
| **Trio Pedidos** | Cancelamento com motivo; tempo estimado calculado pelo `tempo_preparo` dos itens |
| **Insights** | Documentação da API no Swagger; **roteiro da sessão de testes pronto até a sexta 13/11** (um caso por linha: perfil, passos, resultado esperado, cobrindo os 5 módulos) |

### SPRINT 5 — 16/11 a 19/11 (era a Sprint 7; feriado 20/11): Testes em conjunto
**Meta (quinta 19/11):** os 11 testaram juntos o sistema inteiro, bugs críticos corrigidos, sai a **`v1.0-rc`**. **Feature freeze: nada novo a partir daqui.**
- **Segunda:** todos no mesmo ambiente (`docker-compose` + banco resetado) rodando o roteiro da Insights; cada pessoa testa um módulo que **não** é do próprio time.
- **Terça:** fluxo ponta a ponta em grupo (cliente pede → cozinha prepara → entregador entrega → gerente vê nos insights), com 2 navegadores pedindo ao mesmo tempo e cada perfil tentando entrar na área do outro.
- **Quarta e quinta:** cada time corrige os bugs do próprio módulo, por prioridade no board.
- **Base:** ambiente único de teste (`docker-compose` com MySQL **e a API**), feature freeze e tag `v1.0-rc` na `develop`. **Auth/Gestão/Trio:** bugs dos próprios módulos. **Insights:** conduz a sessão e registra os bugs no board.

### SPRINT 6 — 23/11 a 27/11 (era a Sprint 8): Entrega final
**Meta (sexta 27/11):** tag **`v1.0`** na `main` e apresentação com demo ao vivo, cada squad explicando o próprio módulo.
- **Base:** bugs restantes até terça; merge final em `main` e tag `v1.0`; abertura da apresentação com a arquitetura e o **diagrama ER**.
- **Auth:** slide e demo do login por perfil e do JWT.
- **Gestão:** slide e demo da gestão de cardápio e funcionários.
- **Trio Pedidos:** slide e demo do fluxo do pedido (parte central da apresentação).
- **Insights:** README final ("como rodar em 5 passos"), demo dos insights e do Swagger; **ensaio geral na quarta 25/11**.

---

## 9. Como saber o que EU faço

1. Ache o seu time na seção 2 e a **Sprint** atual na seção 8.
2. Siga os passos numerados do seu time, **na ordem**, marcando `[x]` conforme conclui (cada um pode copiar este arquivo ou usar o board do GitHub).
3. Seu trabalho só vale quando passa pelo "Quando uma tarefa está pronta" (seção 7.5).
4. Dúvida de setup ou erro para subir: pergunte no grupo; quem resolveu ajuda o próximo.

---

## 10. Glossário rápido
- **PR (Pull Request):** pedido para juntar sua branch na `develop`; outra pessoa revisa antes do merge.
- **DTO:** classe só com os campos que a API devolve/recebe (nunca a entidade do banco).
- **Entidade (JPA):** classe Java que representa uma tabela.
- **Repository:** interface que faz consultas ao banco (Spring Data JPA).
- **JWT:** token que o login devolve e o front manda em toda chamada protegida.
- **Merge:** juntar uma branch em outra.
- **Feature freeze:** a partir de uma data, não entra funcionalidade nova, só correção de bug.
