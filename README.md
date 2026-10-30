# MAKA

Aplicação de gestão e pedidos para restaurante, com frontend Angular e API REST em Spring Boot.

## Requisitos

- Java 17
- Maven 3.9+
- Node.js 18+ e npm
- Docker com Docker Compose

## Iniciar localmente

1. Na raiz do projeto, configure a senha local do MySQL e inicie o banco:

   ```powershell
   $env:DB_PASSWORD = "senha-local"
   docker compose up -d mysql
   ```

   O schema e os dados iniciais são carregados automaticamente na primeira criação do volume do MySQL.
   Se o banco já existia antes da tabela `ingrediente`, aplique o `CREATE TABLE IF NOT EXISTS ingrediente`
   correspondente em `backend/src/main/resources/schema.sql` no banco existente.

2. Em outro terminal, inicie a API:

   ```powershell
   $env:DB_PASSWORD = "senha-local"
   cd backend
   mvn spring-boot:run
   ```

   A API fica disponível em `http://localhost:8080`. A documentação OpenAPI fica em
   `http://localhost:8080/swagger-ui/index.html`.

3. Em outro terminal, inicie o frontend:

   ```powershell
   cd frontend
   npm ci
   npm start
   ```

   Acesse `http://localhost:4200`. O ambiente local aponta para `http://localhost:8080/api`.

## Funcionalidades deste incremento

- **Insights:** consome `GET /api/insights/resumo` e renderiza os indicadores e gráficos Chart.js a partir do contrato da API. Essa tela não consulta Firebase.
- **Ingredientes:** tela de Gestão em `/gerente/ingredientes`, com cadastro, edição, exclusão e paginação de 10 registros.
- **API de ingredientes:** endpoints sob `/api/admin/ingredientes`, protegidos para o perfil gerente.

### Contrato de ingredientes

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/admin/ingredientes?page=0&size=10` | Lista paginada, ordenada por nome (`size` máximo 100) |
| `POST` | `/api/admin/ingredientes` | Cria ingrediente |
| `PUT` | `/api/admin/ingredientes/{id}` | Atualiza ingrediente |
| `DELETE` | `/api/admin/ingredientes/{id}` | Exclui ingrediente |

O corpo de `POST` e `PUT` usa `nome`, `unidadeMedida` e `custoUnitario`. A listagem retorna uma página Spring Data com os registros em `content`.

Exemplo:

```json
{
  "nome": "Farinha de trigo",
  "unidadeMedida": "kg",
  "custoUnitario": 6.5
}
```

### Testes

```powershell
cd backend
mvn test
```

```powershell
cd frontend
npm run build
```

## Board de trabalho

Itens para acompanhar no GitHub Project do repositório:

| Status | Item |
|---|---|
| Concluído neste incremento | Painel de insights integrado a `/api/insights/resumo` com Chart.js |
| Concluído neste incremento | CRUD paginado de ingredientes — RN06 |
| Próximo | Relacionar ingredientes e quantidades às fichas técnicas dos produtos |
| Próximo | Calcular custo e margem estimada por ficha técnica |
| Próximo | Adicionar testes de integração dos endpoints administrativos e validar acesso por perfil |

## Estrutura

- `backend/` — API Java 17, Spring Boot e MySQL
- `frontend/` — aplicação Angular
- `docker-compose.yml` — banco de dados MySQL para desenvolvimento
