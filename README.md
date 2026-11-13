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
- Git

## Get the code and set up IntelliJ

1. In IntelliJ, open **File → New → Project from Version Control…** (or **Clone Repository** on the Welcome screen).
2. Paste the GitLab repository URL, choose a folder, and click **Clone**. Work on `working_branch`; `main` is protected.
3. When IntelliJ asks, load the Maven project. It downloads the back-end dependencies from `pom.xml`.
4. Open **File → Project Structure → Project** and set the SDK and language level to **21**.
5. Open **Settings → Editor → Code Style** and set the line separator to **LF**.
6. Optional: set the SQL dialect to **PostgreSQL** and add a Database connection to `localhost:5432`, database `opslog`, user `opslog`, password `secret` (the values in `compose.yaml`) to browse the tables.
7. In a terminal, run `cd frontend && npm install` once.

## Project layout

```
src/main/java/com/opslog/
  auth/       sign-up, sign-in, JWT, SecurityConfig
  catalog/    Exercise and Equipment hierarchies, exercise search
  checkin/    morning check-in, TrainingTier hierarchy, TierSelector
  location/   gyms and their equipment
  plan/       program templates, week planner, locked days
  session/    SessionGenerator, RuleBasedGenerator, AiSessionWriter, sessions
  logging/    logged sets, ProgressionRule, history search
  report/     Training Report, Training Balance Report
  notify/     announcements and the avatar notification menu
  mcp/        MCP tools, scoped tokens, audit log, coach summary
  demo/       optional demo account seeder
  common/     shared validation and error handling
src/main/resources/
  application.yaml, application-prod.yaml
  db/migration/   Flyway V1__init.sql … V20__exercise_timings.sql
src/test/java/    JUnit 5 unit tests and Testcontainers integration tests
frontend/src/     React pages, one folder per area (today, stats, planner, catalog, settings, …)
scripts/deploy.sh test → build → push → Render deploy
Dockerfile        multi-stage image (Node build → Maven build → JRE runtime)
render.yaml       Render service definition
compose.yaml      local PostgreSQL 18
```

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

`/mcp` needs a token. Sign in to the app, open **Settings → AI access → New**, name the token, tick **Allow changes to my plan and sessions** if the assistant should write, and click **Create token**. The token is shown once; only its SHA-256 hash is stored.

```bash
npx @modelcontextprotocol/inspector
```

Add a server with transport **Streamable HTTP**, URL `http://localhost:8080/mcp` (or `https://opslog.onrender.com/mcp`), and the header `Authorization: Bearer <token>`. Connect and run a tool.

| Tool | Scope | What it does |
|---|---|---|
| `ping` | read | Connection check |
| `get_checkin` | read | The member's check-in and tier for a date |
| `list_logs` | read | Logged sets in a date range |
| `get_plan` | read | The planned days, including which are locked |
| `search_exercises` | read | Catalog search |
| `get_training_report` | read | The Training Report |
| `set_plan_day` | read and write | Plans a day; locked days are refused by the service layer |
| `write_session` | read and write | Writes the day's session through `AiSessionWriter` |
| `set_coach_summary` | read and write | Posts the assistant's summary to the coach card |

Every tool call except `ping` is recorded in the MCP audit log (tool, outcome, time), and a token can be revoked from **Settings → AI access**.

## Deploy

1. Copy `.env.example` to `.env` and fill in `DOCKER_IMAGE` and `RENDER_DEPLOY_HOOK_URL`.
2. Log in to Docker Hub: `docker login -u <username>`
3. Commit your changes, then run:

```bash
./scripts/deploy.sh
```

The script refuses to run with uncommitted changes. It runs the tests, builds the image, tags it with the Git commit SHA and `latest`, pushes both to Docker Hub, and triggers Render's deploy hook pinned to that SHA. Render waits for `/actuator/health` to pass before switching traffic.

To roll back, use **Rollback** on a previous deploy in the Render dashboard.

### How production is put together

- **Image:** the `Dockerfile` has three stages. Node 22 builds the React app, a JDK 21 stage builds the Spring Boot jar with the React files inside it, and the final stage copies only the jar onto a JRE 21 image that runs as a non-root `opslog` user. JVM flags are sized for Render's 512 MB free instance.
- **Docker Hub:** `queperalta/opslog` (private). Each deploy pushes a tag named after the commit SHA, so any earlier version can be redeployed.
- **Render:** web service `opslog` (Oregon, Free) runs the image. Health check path `/actuator/health`. The service settings are also recorded in `render.yaml`.
- **Neon:** PostgreSQL 18 project `opslog`; the live branch is `production`. Flyway applies any new migrations when the app starts, so a deploy with a new `V{n}__*.sql` updates the schema on its own. Create a Neon branch before a risky data change so it can be restored.

## Environment variables

Names are listed in `.env.example`. Production values are set in the Render dashboard (**opslog → Environment**) and are never committed.

| Variable | Where | Purpose |
|---|---|---|
| `DOCKER_IMAGE` | local `.env` | Docker Hub repository for `deploy.sh` |
| `RENDER_DEPLOY_HOOK_URL` | local `.env` | Render deploy hook for `deploy.sh` |
| `SPRING_PROFILES_ACTIVE` | Render | `prod` |
| `SPRING_DATASOURCE_URL` | Render | Neon JDBC URL with `sslmode=require` |
| `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Render | Neon credentials |
| `JWT_SECRET` | Render | Key that signs sign-in tokens |
| `OPSLOG_DEMO_ENABLED`, `OPSLOG_DEMO_EMAIL`, `OPSLOG_DEMO_PASSWORD` | Render (optional) | Seed the demo account with two weeks of history at startup |
| `OPSLOG_DEMO_RESET` | Render (optional) | `true` rebuilds the demo account with fresh dates |

## Maintenance

- **Add a feature:** create the migration first if the schema changes, then the entity, service, controller and React page, and add tests. Run `./mvnw test` and `cd frontend && npm run lint && npm run build` before committing.
- **Add catalog data** (an exercise, equipment, a location, a tier): add rows in a new migration. Every dropdown reads from the database, so no front-end change is needed.
- **Update dependencies:** change versions in `pom.xml` or `frontend/package.json`, run the full test suite, then deploy.
- **Check production:** Render **Logs** for errors, `https://opslog.onrender.com/actuator/health` for status, and the Neon SQL Editor (branch `production`) for data.

## Windows / WSL notes

- The IntelliJ terminal injects a Windows `JAVA_HOME`. In WSL, override it in `~/.bashrc` to point at a Linux JDK 21.
- Vite uses `watch.usePolling` because file-change events don't cross from the Windows drive into WSL.
- Line endings are normalized to LF by `.gitattributes`. Set IntelliJ to LF for new files.
