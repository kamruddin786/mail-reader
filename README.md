# Mail Reader MCP Server (STDIO)

MCP server built with **Spring Boot 3.5** and **Spring AI MCP** that reads and replies to email. Uses **STDIO** transport so Cursor (or any MCP client) can run it as a subprocess.

## Features

- **list_recent_mails** – List the most recent emails (with optional limit).
- **list_mails_by_subject** – List emails whose subject contains given text.
- **read_mail** – Read full body of an email by 1-based index.
- **reply_to_mail** – Send a reply to an address with subject and body.

## Prerequisites

- **Java 21**
- **Maven 3.9+**
- IMAP/SMTP account (e.g. Gmail with [App Password](https://support.google.com/accounts/answer/185833))

## MCP tool config (STDIO)

Cursor uses `.cursor/mcp.json` in this repo. The server is configured as a **stdio** server:

- **command**: `java`
- **args**: `-Dspring.ai.mcp.server.stdio=true -jar target/mail-reader-mcp-1.0.0-SNAPSHOT.jar`
- **env**: Set `MAIL_USER` and `MAIL_PASSWORD` (and optionally `MAIL_HOST`, `MAIL_IMAP_PORT`).
- **cwd**: `${workspaceFolder}` so the JAR path is correct when Cursor starts the process.

Ensure the workspace folder is the project root (where `target/` lives).

### Alternative: run with Maven (no JAR)

You can switch to Maven in `.cursor/mcp.json` so the server runs without building a JAR:

```json
"mail-reader": {
  "command": "mvn",
  "args": ["-q", "spring-boot:run", "-Dspring-boot.run.arguments=--spring.ai.mcp.server.stdio=true"],
  "env": { "MAIL_USER": "...", "MAIL_PASSWORD": "..." },
  "cwd": "${workspaceFolder}"
}
```

## Build and run

```bash
# Build JAR
mvn clean package -DskipTests

# Run as STDIO MCP server (for Cursor or CLI)
java -Dspring.ai.mcp.server.stdio=true -jar target/mail-reader-mcp-1.0.0-SNAPSHOT.jar
```

Set mail credentials via env or `application.yml`:

- `MAIL_USER` – IMAP/SMTP username (e.g. Gmail address).
- `MAIL_PASSWORD` – IMAP/SMTP password (e.g. Gmail App Password).
- `MAIL_HOST` – IMAP host (default `imap.gmail.com`).
- `MAIL_IMAP_PORT` – IMAP port (default `993`).

## Configuration

- **STDIO**: `spring.ai.mcp.server.stdio=true` in `application.yml` or `-D`.
- **Logging**: Console pattern is cleared and root level set to WARN so STDIO JSON-RPC is not corrupted.
- **Mail**: Uses `spring.mail.*` for SMTP (sending) and the same host/user/password for IMAP (reading) via Jakarta Mail / Angus Mail.

## Libraries (2025–2026)

- Spring Boot **3.5.0**
- Spring AI **1.1.2** (`spring-ai-starter-mcp-server` for STDIO)
- `spring-boot-starter-mail` (Jakarta Mail + SMTP)
- **Angus Mail 2.0.4** (IMAP implementation)

## Tool annotations

Uses Spring AI `@Tool` / `@ToolParam` from `org.springframework.ai.tool.annotation`. The MCP server auto-configuration discovers `@Tool`-annotated methods on `@Component` beans and exposes them as MCP tools over STDIO.
