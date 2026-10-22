<strong>**DO NOT DISTRIBUTE OR PUBLICLY POST SOLUTIONS TO THESE LABS. MAKE ALL FORKS OF THIS REPOSITORY WITH SOLUTION CODE PRIVATE. PLEASE REFER TO THE STUDENT CODE OF CONDUCT AND ETHICAL EXPECTATIONS FOR COLLEGE OF INFORMATION TECHNOLOGY STUDENTS FOR SPECIFICS. **</strong>

# D424 Software Engineer Capstone: OpsLog

An autoregulated strength-training web app for Kaimana Strength Collective. A morning check-in scales each day's session to the member's recovery, schedule, and available equipment. An MCP interface lets an authorized AI assistant read training data and write sessions through the same validated service layer.

**Live:** https://opslog.onrender.com

## Tech stack

| Layer | Technology |
|---|---|
| Back end | Java 21, Spring Boot 4.1, Spring Security, Spring Data JPA, Flyway |
| AI interface | Spring AI 2.0 MCP server (Streamable HTTP at `/mcp`) |
| Front end | React 19, TypeScript, Vite, MUI, TanStack Query, React Router |
| Database | PostgreSQL (Docker locally, Neon in production) |
| Hosting | Docker image on Docker Hub, deployed to Render |

## Prerequisites

- JDK 21 (Eclipse Temurin)
- Node.js 22 LTS
- Docker Desktop (on Windows, enable **Settings → Resources → WSL Integration** for your distro)
- IntelliJ IDEA (recommended)

## Run locally

```bash
# Back end on http://localhost:8080. Starts Postgres from compose.yaml automatically.
./mvnw spring-boot:run

# Front end on http://localhost:5173. Proxies /api and /mcp to :8080.
cd frontend
npm install
npm run dev
```

Health check: `curl localhost:8080/actuator/health`

## Run tests

```bash
./mvnw test
```

Integration tests use Testcontainers, so Docker must be running.

## Database migrations

Schema changes are versioned Flyway scripts in `src/main/resources/db/migration`.

- Name new files `V{n}__description.sql` (two underscores), for example `V2__add_exercises.sql`.
- Never edit a migration that has already been applied. Flyway checksums each file and refuses to start if one changes. Write a new migration instead.
- Hibernate runs with `ddl-auto: validate`, so entities must match the migrated schema.

## Test the MCP server

```bash
npx @modelcontextprotocol/inspector
```

Add a server with transport **Streamable HTTP** and URL `http://localhost:8080/mcp` (or `https://opslog.onrender.com/mcp`), connect, and run the `ping` tool.

## Deploy

1. Copy `.env.example` to `.env` and fill in `DOCKER_IMAGE` and `RENDER_DEPLOY_HOOK_URL`.
2. Log in to Docker Hub: `docker login -u <username>`
3. Commit your changes, then run:

```bash
./scripts/deploy.sh
```

The script refuses to run with uncommitted changes. It runs the tests, builds the image, tags it with the Git commit SHA and `latest`, pushes both to Docker Hub, and triggers Render's deploy hook pinned to that SHA. Render waits for `/actuator/health` to pass before switching traffic.

To roll back, use **Rollback** on a previous deploy in the Render dashboard.

## Environment variables

See `.env.example`. Production values are set in the Render dashboard and are never committed.

## Windows / WSL notes

- The IntelliJ terminal injects a Windows `JAVA_HOME`. In WSL, override it in `~/.bashrc` to point at a Linux JDK 21.
- Vite uses `watch.usePolling` because file-change events don't cross from the Windows drive into WSL.
- Line endings are normalized to LF by `.gitattributes`. Set IntelliJ to LF for new files.
