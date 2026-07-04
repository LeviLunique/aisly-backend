# Aisly Backend

Kotlin/Spring Boot modular monolith for the Aisly iOS app. The API stores shopping lists, templates, categories, catalog entries and purchase history for users authenticated by the external AuthServer.

## Local Bootstrap

1. Create `.env` from `.env.example`.
2. Start PostgreSQL and the API:

```bash
docker compose up --build
```

3. Health check:

```bash
curl http://localhost:8081/healthcheck
```

Protected endpoints require a bearer JWT issued by the AuthServer configured through `AUTH_ISSUER_URI`, `AUTH_JWK_SET_URI` and `AUTH_AUDIENCE`.

## Branch And Version Policy

- `develop` deploys development only from versions matching `x.y.z-n-SNAPSHOT`.
- `main` deploys production only from closed semantic versions matching `x.y.z`.
- The pipeline validates the version in `build.gradle.kts`; it never creates versions automatically.

Private agent docs and skills are intentionally ignored in `/docs`, `.codex-skills` and `.claude/skills`.

