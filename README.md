# Card Agent

An AI support agent for a credit card issuer, built with Spring AI. It exposes a synchronous chat API for customer queries and runs an autonomous, email-driven workflow that processes card-closure requests end to end — reading mail, deciding what to do via an LLM, and replying — without a human in the loop for the happy path.

All customer/card data access and actions (balance lookup, card program details, closing a card) are performed exclusively through tools exposed by an external MCP server; this app never talks to a card database directly.

## Capabilities

**Cardigan chat agent** (`GET /api/cards`)
- Look up a customer's outstanding balance by email or mobile number.
- Recommend the best card product for a customer's needs (e.g. lowest interest rate, Qantas vs. Velocity points, longest interest-free period, no international fee), citing real card program details retrieved via RAG.
- Look up basic customer details by email.
- Refuses card-closure requests directly, redirecting the customer to email the closure team instead.

**Autonomous card-closure workflow**
- Polls a mailbox on a schedule for closure requests coming from the Card Closure Team (not directly from customers).
- Validates the request, verifies the customer exists, and checks for blockers (outstanding balance, fraud flag, already closed).
- Closes the card via the MCP tool when all checks pass, then sends a threaded confirmation reply to both the closure team and the customer.
- On any failure, leaves the request for retry on the next poll rather than dropping it.

## Tech stack

- **Java 25**, **Spring Boot 4.1.1**, **Maven**
- **Spring AI 2.0.0** — `ChatClient`, MCP client (streamable HTTP), RAG advisors
- **OpenAI** — chat model + `text-embedding-3-small` embeddings
- **Qdrant** — vector store for card product documents (RAG)
- **Apache Tika** — PDF parsing of card program documents
- **Spring Mail (JavaMail)** — sending/receiving agent email
- **Mailpit** — local fake SMTP server + inbox used as the mailbox the agent monitors
- **Model Context Protocol (MCP)** — tool access to an external card-service backend
- **Docker Compose** — local Mailpit and Qdrant instances

## Running locally

1. Start the local dependencies:
   ```
   docker compose up -d
   ```
   This brings up Mailpit (SMTP on `1025`, web UI/API on `8025`) and Qdrant (`6333`/`6334`).

2. Have the external card-service MCP server running at `http://localhost:8090/card-agent-mcp-server`.

3. Set your OpenAI API key and run the app:
   ```
   export OPENAI_API_KEY=sk-...
   ./mvnw spring-boot:run
   ```

4. Try the chat API:
   ```
   curl -H "email: jane@example.com" "http://localhost:8080/api/cards?message=What+is+my+outstanding+balance?"
   ```

5. Seed a test card-closure email (simulating the Card Closure Team) and watch it get processed:
   ```
   curl -X POST "http://localhost:8080/email?subject=Card+Closure&body=Please+close+the+card+belonging+to+the+customer+jane@example.com"
   ```
   Check the Mailpit UI at http://localhost:8025 to see the agent's reply.
