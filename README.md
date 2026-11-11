# identity-service

Serviço de identidade do **Correcta** (Juiz Online do IFBA). Gerencia pessoas, perfis institucionais, credenciais de acesso, sessões e tokens de autenticação. Outros serviços consultam perfis por uma API interna protegida por token compartilhado.

## Stack

- Kotlin 1.9 + Java 17
- Spring Boot 3.4 (Web, Data JPA, Security, Validation, AMQP, Actuator)
- Spring Cloud OpenFeign (cliente do proxycheck.io)
- PostgreSQL (produção) e H2 (testes)
- RabbitMQ (envio de e-mails transacionais pelo serviço de mailer)
- JJWT 0.11 (tokens de acesso), BCrypt (senhas), Yauaa (parser de `User-Agent`)
- springdoc-openapi (Swagger UI)
- Gradle Kotlin DSL, Spotless/ktlint, JaCoCo, JUnit 5, MockK

## Domínio

| Entidade | Descrição |
| --- | --- |
| `Person` | Pessoa física identificada pelo CPF (armazenado só com dígitos). Possui nome civil, nome social opcional e flag `active`. |
| `InstitutionalProfile` | Vínculo da pessoa com a instituição: `STUDENT`, `FACULTY` ou `STAFF`. Estados: `PENDING`, `ACTIVE`, `INACTIVE`. Uma pessoa só pode ter um perfil não inativo de cada tipo. |
| `AccountCredential` | Login e hash BCrypt da senha do perfil. A senha fica nula até a ativação da conta. |
| `AccountActivation` | Token de ativação de uso único (válido por 7 dias). Só o hash SHA-256 é armazenado. |
| `AccountSession` | Sessão de login (válida por 90 dias) com dispositivo, IP e geolocalização do cliente. |
| `AccountRefreshToken` | Refresh token opaco de uso único, com rotação encadeada (`replacedBy`). Só o hash SHA-256 é armazenado. |

### Fluxo de conta

1. Um perfil `STAFF` cria um perfil institucional. A pessoa é resolvida pelo CPF e criada se não existir.
2. O serviço cria a credencial no estado `PENDING` e publica o e-mail de ativação no RabbitMQ. O link é `<ENV_IDENTITIES_ACCOUNT_ACTIVATION_BASE_URL>/<token>`.
3. O usuário define a senha em `POST /account/create-password` com o token de ativação. O perfil passa para `ACTIVE`.
4. O usuário faz login e recebe um access token (JWT, 15 min por padrão) e um refresh token (7 dias por padrão).
5. `POST /auth/refresh-token` consome o refresh token e emite um novo par na mesma sessão.

Na primeira inicialização, sem nenhum perfil `STAFF`, o `FirstStaffAccountEnsurer` cria a conta administrativa inicial.

## Autenticação

O `AuthenticationFilter` lê as credenciais de cada requisição:

- `/api/v1/profiles/**`: somente o header `X-Internal-Token`.
- `/account/create-password`: token de ativação.
- Demais rotas: JWT de acesso.

O token vem do header `Authorization: Bearer <token>` ou do cookie `correcta-access-token`. Com cookie, métodos não seguros (`POST`, `PATCH`, `DELETE`...) exigem header `Origin` confiável, para bloquear CSRF.

As anotações dos controllers são aplicadas pelo `SecurityAspect`:

| Anotação | Exige | Falha |
| --- | --- | --- |
| `@IsLoggedUser` | requisição autenticada | `401` |
| `@IsAccountActivationRequest` | token de ativação | `401` |
| `@IsInternalRequest` | `X-Internal-Token` válido | `401` |
| `@IsProfileType(STAFF)` | perfil do tipo informado | `403` |

## API

Com `ENV_SPRINGDOC_ENABLED=true`, a documentação fica em `/swagger-ui.html` (perfil `local`) ou `/swagger-ui/index.html`.

| Método | Rota | Acesso | Descrição |
| --- | --- | --- | --- |
| `POST` | `/auth/login` | público | Login com login e senha |
| `POST` | `/auth/logout` | logado | Revoga a sessão atual |
| `POST` | `/auth/refresh-token` | público | Rotaciona os tokens |
| `POST` | `/account/create-password` | ativação | Define a primeira senha |
| `GET` | `/account/me` | logado | Dados da conta autenticada |
| `GET` | `/account/sessions` | logado | Sessões ativas |
| `DELETE` | `/account/sessions` | logado | Revoga todas as sessões |
| `DELETE` | `/account/sessions/{id}` | logado | Revoga uma sessão (dono ou `STAFF`) |
| `GET` | `/persons` | `STAFF` | Lista paginada de pessoas |
| `POST` | `/persons` | `STAFF` | Cria pessoa |
| `GET` | `/persons/{personId}` | `STAFF` | Detalhes da pessoa e seus perfis |
| `PATCH` | `/persons/{personId}` | `STAFF` | Atualiza nomes |
| `POST` | `/persons/{personId}/deactivate` | `STAFF` | Desativa a pessoa e seus perfis |
| `POST` | `/persons/{personId}/reactivate` | `STAFF` | Reativa a pessoa |
| `GET` | `/institutional-profiles` | `STAFF` | Lista paginada de perfis |
| `POST` | `/institutional-profiles` | `STAFF` | Cria perfil e credencial |
| `GET` | `/institutional-profiles/{profileId}` | `STAFF` | Detalhes do perfil |
| `PATCH` | `/institutional-profiles/{profileId}` | `STAFF` | Atualiza o e-mail |
| `POST` | `/institutional-profiles/{profileId}/activation` | `STAFF` | Reenvia o e-mail de ativação |
| `POST` | `/institutional-profiles/{profileId}/deactivate` | `STAFF` | Desativa o perfil |
| `POST` | `/institutional-profiles/{profileId}/reactivate` | `STAFF` | Reativa o perfil |
| `GET` | `/api/v1/profiles/{profileId}` | interno | Consulta um perfil |
| `GET` | `/api/v1/profiles/search?q=&type=&size=` | interno | Busca perfis ativos por nome, login ou e-mail |
| `POST` | `/api/v1/profiles/batch-lookup` | interno | Consulta até 100 perfis |
| `GET` | `/actuator/health` | público | Health check (probes habilitadas) |

Listagens aceitam `page`, `size` (1 a 100), `sortBy` e `sortDirection` (`asc`/`desc`). Erros retornam `{"message": "..."}`; erros de validação retornam também `errors` por campo. As mensagens de erro da API estão em português.

## Configuração

Variáveis de ambiente lidas por `application.properties`:

| Variável | Padrão | Descrição |
| --- | --- | --- |
| `ENV_DATASOURCE_POSTGRES_URL` | — | URL JDBC do PostgreSQL |
| `ENV_DATASOURCE_POSTGRES_USERNAME` | — | Usuário do banco |
| `ENV_DATASOURCE_POSTGRES_PASSWORD` | — | Senha do banco |
| `ENV_JPA_DDL_AUTO` | `update` | Estratégia de DDL do Hibernate |
| `ENV_SQL_INIT_MODE` | `always` | Executa `db/postgresql/schema.sql` (migração idempotente de enums legados) |
| `ENV_RABBITMQ_URL` | — | Endereço AMQP do RabbitMQ |
| `ENV_MAILER_RABBITMQ_EXCHANGE` | `send-transactional-mail-exchange` | Exchange do mailer |
| `ENV_MAILER_RABBITMQ_ROUTING_KEY` | `send-transactional-mail-routing-key` | Routing key do mailer |
| `ENV_PROXY_CHECK_URL` | `https://proxycheck.io` | URL base do proxycheck.io |
| `ENV_IDENTITIES_JWT_SECRET` | — | Chave HMAC dos JWT (mínimo 32 bytes) |
| `ENV_IDENTITIES_JWT_ACCESS_TOKEN_DURATION` | `900` | Validade do access token, em segundos |
| `ENV_IDENTITIES_JWT_REFRESH_TOKEN_DURATION` | `604800` | Validade do refresh token, em segundos |
| `ENV_IDENTITIES_INTERNAL_TOKEN` | vazio | Token da API interna. Vazio bloqueia toda chamada interna |
| `ENV_IDENTITIES_ACCOUNT_ACTIVATION_BASE_URL` | `http://localhost:5173/activate-account` | Página de ativação do front-end. O token é anexado como último segmento do caminho |
| `ENV_IDENTITIES_FIRST_ACCOUNT_SETUP_ENABLED` | `true` | Cria a conta `STAFF` inicial |
| `ENV_IDENTITIES_FIRST_ACCOUNT_EMAIL` | ver `application.properties` | E-mail e login da conta inicial |
| `ENV_IDENTITIES_FIRST_ACCOUNT_PERSON_NAME` | `Administrador` | Nome da pessoa da conta inicial |
| `ENV_IDENTITIES_FIRST_ACCOUNT_PERSON_DOCUMENT_NUMBER` | `000.000.000-00` | CPF da pessoa da conta inicial |
| `ENV_SPRINGDOC_ENABLED` | `false` | Habilita OpenAPI e Swagger UI |

Para desenvolvimento, `application-local.properties` traz valores locais. Ative com `SPRING_PROFILES_ACTIVE=local`.

## Execução

Pré-requisitos: JDK 17, PostgreSQL e RabbitMQ acessíveis.

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

Imagem Docker:

```bash
docker build -t identity-service .
docker run -p 8080:8080 --env-file .env identity-service
```

## Testes e cobertura

```bash
./gradlew test                 # testes + relatório JaCoCo
./gradlew check                # testes + verificação de cobertura mínima
```

- Relatório HTML: `build/reports/jacoco/test/html/index.html`
- Relatório XML: `build/reports/jacoco/test/jacocoTestReport.xml`

Estado atual: **143 testes**, **100% de cobertura de linhas e de branches**. A task `jacocoTestCoverageVerification` (executada por `check`) falha abaixo de 100%.

Os testes ficam em `src/test/kotlin`:

- Testes unitários com MockK para serviços, controllers, segurança, componentes, validadores, modelos e utilitários.
- Testes de integração com `@SpringBootTest` e H2 (perfil `test`) para as specifications JPA e a API interna de perfis.

## Qualidade de código

- Spotless com ktlint 1.7.1: `./gradlew spotlessApply`.
- O hook `.githooks/pre-commit` formata os arquivos Kotlin staged. O Gradle configura `core.hooksPath` na primeira compilação.

## Deploy

O workflow `.github/workflows/release.yml` roda a cada push na `main`:

1. Gera e publica a imagem `docker.correcta.staily.dev/correcta/identity-service:sha-<commit>`.
2. Atualiza a tag da imagem no overlay de produção do repositório `CorrectaIFBA/infra` (GitOps).

O build da imagem pula os testes (`-x test`). Rode `./gradlew check` antes do push.

## Estrutura

```
src/main/kotlin/io/correcta/identities
├── clients/          # Feign (proxycheck.io)
├── components/       # Conta inicial, parser de User-Agent, filtro do header Vary
├── config/           # JPA, agendador, OpenAPI e properties
├── controllers/      # Interfaces da API (OpenAPI) e implementações em impl/
├── entities/         # Entidades JPA
├── exceptions/       # Exceções de negócio mapeadas para status HTTP
├── handlers/         # @RestControllerAdvice
├── mailer/           # Publicação de e-mails no RabbitMQ
├── models/           # DTOs de request e response
├── repositories/     # Spring Data JPA e specifications
├── security/         # Filtro, providers, anotações e aspecto de autorização
├── service/          # Regras de negócio
└── utils/            # Hash, tokens, paginação, CPF e validação
```

## Pontos conhecidos

- As origens CORS são fixas em `SecurityConfig`.
- O `UserAgentParser` repete a marca no nome de alguns dispositivos (ex.: `Google Google Pixel 8`), pois o Yauaa já inclui a marca em `DeviceName`.
