# Aisly Backend — Cadastro de Listas (Tema 3)

Pós-Graduação em Desenvolvimento de Aplicativos Móveis da PUC-PR  
Disciplina: Serviços Mobile em Cloud AWS (Turma U)  
Professor: Vinícius Godoy Mendonça  
Aluno: Levi Lunique Izidio da Silva

Servidor de cadastro de **listas de compras** em Kotlin + Spring Boot, com
login delegado ao [AuthServer](https://github.com/LeviLunique/aisly-authserver) da disciplina. É o backend do
[Aisly](https://github.com/LeviLunique/aisly-ios), um app iOS real de listas de
compras — este servidor sincroniza 100% das funcionalidades do app.

## Vídeo

📹 **[Link do vídeo de demonstração](COLOQUE_O_LINK_AQUI)**

---

## O problema

O Aisly é um app iOS de listas de compras que funciona 100% offline: listas,
itens com preços planejado/real, categorias, um catálogo de produtos
reutilizáveis, templates e histórico de compras — tudo salvo em JSON no
dispositivo. Sem um servidor, o usuário **não tem conta, não tem backup e não
sincroniza entre dispositivos**: trocou de iPhone, perdeu tudo.

O Tema 3 pede exatamente a peça que falta: um segundo servidor que cadastra
listas para usuários autenticados por um AuthServer externo — sem sistema de
login próprio — e que limpa os dados quando a conta é excluída.

## A solução

```
┌─────────────┐   register/login    ┌──────────────┐
│  Aisly iOS  │ ──────────────────► │  AuthServer  │  (emite JWT HS256)
│             │ ◄────── JWT ─────── │   :8080      │
│             │                     └──────┬───────┘
│             │   Bearer JWT               │ DELETE /users/me aciona
│             │ ──────────────────► ┌──────▼───────────────────────┐
│             │ ◄── listas/JSON ─── │  Aisly Backend :8081         │
└─────────────┘                     │  POST /internal/v1/users/    │
                                    │    {sub}/delete-data         │
                                    │  (X-Aisly-Internal-Secret)   │
                                    └──────────┬───────────────────┘
                                               │ JPA/Flyway
                                          ┌────▼─────┐
                                          │ Postgres │
                                          └──────────┘
```

- O app faz login **no AuthServer** e recebe um JWT HS256, validado aqui com o
  mesmo segredo HMAC configurado no AuthServer.
- **Todos** os endpoints `/api/v1/**` exigem o token; o dono dos dados é o
  `sub` do JWT — o cliente nunca envia o próprio id.
- Quando o usuário exclui a conta no AuthServer, ele chama o endpoint interno
  deste servidor (protegido por segredo compartilhado) que apaga todos os
  dados daquele usuário — e a exclusão da conta **só completa se a limpeza
  der certo** (falha do webhook aborta e permite retry).

### Requisitos do Tema 3 — onde cada um está

| Requisito | Onde |
| --- | --- |
| Segundo servidor que cadastra listas | Este repositório (listas, itens, categorias, catálogo, templates, histórico) |
| Login pelo AuthServer, sem login próprio | `security/SecurityConfig.kt` — resource server JWT; nenhum endpoint de senha aqui |
| ≥1 endpoint exigindo o token | Todos os `/api/v1/**` (ex.: `GET /api/v1/lists`) |
| Exclusão de usuário aciona limpeza | AuthServer → `POST /internal/v1/users/{sub}/delete-data` com `X-Aisly-Internal-Secret` (`account/`) |
| Opcional SNS/SQS | Não implementado — optei pelo webhook síncrono: com um único consumidor e a exigência de confirmar a limpeza antes de concluir a exclusão, uma fila só adicionaria consistência eventual sem benefício aqui |

## Arquitetura

Padrão simples e direto — **um pacote por funcionalidade, cada um com
Controller → Service → Repository (Spring Data)**, o mesmo estilo do
backend-dev da disciplina:

```
com.aisly.backend
├── security/    validação do JWT + extração do dono (sub)
├── web/         tratamento de erros + /healthcheck
├── lists/       listas + itens + finalizar compra
├── templates/   listas reutilizáveis (mesma tabela, com recorrência)
├── categories/  categorias por usuário ("Other" é fixa)
├── catalog/     catálogo de produtos reutilizáveis
├── history/     snapshots imutáveis de compras finalizadas
└── account/     webhook interno de exclusão de conta
```

Cada funcionalidade tem subpacotes `requests/` e `responses/` com um DTO por
arquivo (ex.: `lists/requests/CreateListRequest.kt`, `lists/responses/ListResponse.kt`),
seguindo o padrão da disciplina; enums e helpers que não são DTOs ficam na raiz
do pacote da feature (ex.: `lists/ShoppingUnit.kt`, `history/HistorySnapshot.kt`).

Por que assim: as regras de negócio são pequenas (CRUD + algumas invariantes),
então camadas extras (hexagonal, DDD, módulos) só adicionariam indireção sem
proteção — clareza vale mais para revisão e manutenção. As decisões que
importam estão nos services:

- **Ownership em toda query** — nenhum dado de um usuário é visível/alterável
  por outro (id de outro dono responde 404, sem vazar existência).
- **Upsert idempotente por id do cliente** — o app iOS é offline-first e gera
  UUIDs localmente; os POSTs de criação aceitam esse id e um retry vira
  atualização, nunca duplicata.
- **Histórico imutável** — finalizar uma compra cria um snapshot e arquiva a
  lista na mesma transação.
- **Schema versionado com Flyway** — migrações imutáveis em `db/migration`.

## Como rodar

Pré-requisitos: JDK 25 (o Gradle baixa se faltar), Docker.

```bash
# 1. PostgreSQL
docker compose up -d postgres

# 2. AuthServer (repositório backend-dev), em outro terminal
cd ../backend-dev && ./gradlew bootRun          # porta 8080

# 3. Este servidor
./gradlew bootRun                                # porta 8081
```

Fluxo completo via curl:

```bash
# Cadastro no AuthServer → token
TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"demo@aisly.dev","password":"12345678","displayName":"Demo"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["accessToken"])')

# Sem token → 401
curl -i localhost:8081/api/v1/lists

# Com token → 200
curl -s localhost:8081/api/v1/lists -H "Authorization: Bearer $TOKEN"

# Criar uma lista com itens
curl -s -X POST localhost:8081/api/v1/lists \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Compras da semana","budget":300,
       "items":[{"name":"Banana","quantity":1,"unit":"kg","categoryName":"Produce","plannedPrice":9.0}]}'
```

Swagger UI: http://localhost:8081/swagger-ui.html

## Endpoints (resumo)

| Área | Endpoints |
| --- | --- |
| Listas | `GET/POST /api/v1/lists`, `GET/PUT/DELETE /api/v1/lists/{id}`, `PATCH .../archive`, `PATCH .../pin`, `PUT /api/v1/lists/reorder`, `POST .../finish` |
| Itens | `POST/PUT/DELETE /api/v1/lists/{id}/items[/{itemId}]`, `PATCH .../completion`, `PUT .../reorder` |
| Templates | `GET/POST /api/v1/templates`, `POST .../use`, `PATCH .../archive`, `DELETE` |
| Categorias | `GET/POST/PUT/DELETE /api/v1/categories[/{id}]`, `PUT .../reorder` |
| Catálogo | `GET/POST/PUT/DELETE /api/v1/catalog/items[/{id}]` |
| Histórico | `GET/DELETE /api/v1/history[/{id}]`, `POST .../repeat`, `POST .../template` |
| Interno | `POST /internal/v1/users/{sub}/archive-data`, `POST .../delete-data` (header `X-Aisly-Internal-Secret`) |
| Público | `GET /healthcheck` |

## Testes

```bash
./gradlew test
```

Testes de integração (MockMvc + H2 em modo PostgreSQL) cobrindo: upsert por id
do cliente, isolamento entre usuários, finalização de compra idempotente,
categoria fixa, e o webhook interno com segredo certo/errado.

## Deploy na AWS

Infra em `infra/terraform/` (EC2 ARM single-node + Docker Compose com
Postgres, AuthServer e API; bucket S3 versionado para os jars; alarme de
billing). O GitHub Actions valida a versão do Gradle, roda os testes e — em
push para `develop`/`main` — sobe o jar para o S3 e reinicia a API na EC2 via
SSM, com healthcheck e smoke test (`scripts/harness-smoke.sh`).

O segredo HMAC dos JWTs e o segredo interno de webhook são gerados pelo
Terraform e injetados no Docker Compose da EC2. Arquivos `.env` locais,
Terraform state/plan e credenciais ficam ignorados pelo Git; use
`.env.example` e `.env.aws.example` apenas como modelos sem valores reais.

## Integração com o app iOS

O Aisly iOS tem um modo remoto (feature flag `aisly.remote.enabled`) com
telas de login/cadastro contra o AuthServer, token no Keychain e repositórios
remotos que sincronizam todas as entidades com este servidor por diff — com
tratamento global de 401 (sessão expirada → volta ao login).
