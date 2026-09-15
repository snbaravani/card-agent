# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

- Build: `./mvnw clean install` (or `./mvnw clean package`)
- Run the app: `./mvnw spring-boot:run` (requires `OPENAI_API_KEY` env var and the local dependencies below running)
- Run all tests: `./mvnw test`
- Run a single test: `./mvnw test -Dtest=CardAgentApplicationTests#contextLoads`
- Start local dependencies (Mailpit + Qdrant): `docker compose up -d` (uses `compose.yaml`; there is no Spring Boot Docker Compose lifecycle wiring, so these must be started/stopped manually)
  - Mailpit web UI / REST API: http://localhost:8025 (SMTP on 1025)
  - Qdrant: REST on 6333, gRPC on 6334 (the app talks to 6334)
- This project also depends on a separate MCP server running at `http://localhost:8090/card-agent-mcp-server` (not part of this repo) — it must be running for either chat surface to do anything beyond conversation.

## Architecture

This is a Spring AI (2.0.0, currently configured against the OpenAI model provider — the Anthropic starter is present in `pom.xml` but commented out) application with two independent entry points into the same style of `ChatClient` (system prompt loaded from a classpath `.st` resource + MCP tool callbacks). All "real" data access and actions (customer lookup, balances, card programs, closing a card) happen exclusively through MCP tools from the external `card-service` MCP connection — there is no direct database access in this codebase.

### 1. Synchronous chat API
`MCPClientController` (`/api/cards`, `/api/cards/productrec`) answers ad-hoc customer queries (outstanding balance, card recommendations, customer lookup) using `card-agent-system.st` as the system prompt. Its `ChatClient` is built with `defaultTools(toolCallbackProvider)`, a `QuestionAnswerAdvisor` (RAG over the Qdrant `card_products` collection) and `TokenUsageAdvisor`. The prompt explicitly refuses card-closure requests and redirects the customer to email `card-closure-support@mybank.com` instead.

### 2. Autonomous card-closure email workflow
This is the more involved path — understanding it requires reading across several files:

- `CardInboxMonitor` polls Mailpit's REST API (`MailpitClient`) on a fixed delay (`card-agent.inbox.poll-interval`) for unread mail addressed to `card-agent.inbox.address`. Mail not sent *from* `card-agent.inbox.closure-team` is silently ignored — this app only ever acts on mail from the Card Closure Team, never directly from customers.
- Each accepted message is handed to the active `CardEmailHandler` bean. `CardEmailHandlerAgent` is `@Primary`; `LoggingCardEmailHandler` is a no-op baseline that just logs (useful for verifying the polling loop in isolation).
- `CardEmailHandlerAgent` → `CardSupportAgent` (a separate `ChatClient` seeded with `card-closure-agent-system.st`, templated with `closure_team_email`/`support_inbox`) → response is parsed into the structured `CardAgentResponse` record (`subject`, `body`, `operatorSummary`) → `CardClosureMailSender` sends a threaded SMTP reply (sets `In-Reply-To`/`References` from the original `Message-ID`) back through Mailpit.
- `card-closure-agent-system.st` encodes a strict sequential decision workflow (validate closure intent → validate customer email present → verify customer exists → pre-closure checks for balance/fraud/already-closed → execute and notify both parties) and forbids emailing customers directly except at the terminal steps.
- Any failure anywhere in this chain causes the handler to return `false`, and `CardInboxMonitor` resets the message to unread via `mailpit.setRead(id, false)` so it is retried on the next poll — there is no dead-letter queue, just retry-until-success.
- `McpClientLoggingHandler` (`@McpLogging(clients = "card-service")`) forwards log notifications emitted by the MCP server itself into this app's SLF4J logs.

### 3. Manual test endpoint
`CardCloseMailController` (`POST /email`) seeds a fake card-closure email directly into Mailpit via `JavaMailSender`. It exists purely to trigger the workflow above from Postman/curl during manual testing — it is not part of the production flow.

### RAG indexing is currently broken
`CardProgramsLoader` reads the PDFs under `src/main/resources/card_products/*.pdf` with `TikaDocumentReader` and chunks them with `TokenTextSplitter` on startup, but the actual `vectorStore.add(...)` call is commented out. Product PDFs are **not** currently being indexed into Qdrant, so `QuestionAnswerAdvisor` in `MCPClientController` has nothing to retrieve against until this is fixed.

## Configuration notes

- `card-agent.inbox.*` properties bind to `CardInboxProperties` and control the Mailpit base URL, the agent's own inbox address, the trusted closure-team sender address, poll interval, and batch size.
- Qdrant collection name is `card_products`; schema is auto-initialized (`spring.ai.vectorstore.qdrant.initialize-schema=true`).
- Embedding model is `text-embedding-3-small` with dimensions forced to 384.
