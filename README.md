# keiko-engine

A **content-agnostic flashcard study engine**, Spring Boot edition. Sister project to the Python/Django [`jlpt-n2-mo`](https://github.com/tianxia-diyidao/jlpt-n2-mo) — same engine philosophy, JVM stack, generalized to study **any subject**.

The initial subject ships with **50 reliable US Federal Constitutional Law multiple-choice questions** (state-bar level, CA / NY / TX, as of May 2026), but the engine itself is subject-blind: drop a new directory under `src/main/resources/subjects/<id>/` (with `cards.json` + `subject.toml`), point `STUDY_SUBJECT` at it, restart.

---

## ONE COMMAND to run locally

### Windows (PowerShell)

```powershell
.\run.ps1
```

→ opens http://localhost:8080

### Mac / Linux

```bash
make run
```

→ opens http://localhost:8080

Both wrappers shell out to `./gradlew bootRun` with `STUDY_SUBJECT=us-conlaw` set. Override with `.\run.ps1 -SubjectId calc` (Windows) or `make run SUBJECT=calc` (Mac) once a `calc` subject directory exists.

---

## ONE COMMAND to deploy to Fly.io

After PR is merged to `main`, from the repo root:

```bash
fly deploy
```

Setup steps for first deploy are below.

---

## First-time local setup (Windows)

1. **Install JDK 21** (Eclipse Temurin):
   ```powershell
   winget install --id EclipseAdoptium.Temurin.21.JDK
   ```
   Or download from https://adoptium.net.

2. **Verify Java**:
   ```powershell
   java -version
   ```
   Should report `openjdk version "21.x.x"`.

3. **Clone the repo**:
   ```powershell
   cd C:\Users\jerry\code
   git clone https://github.com/tianxia-diyidao/keiko-engine.git
   cd keiko-engine
   ```

4. **Open in IntelliJ IDEA** (Community is free):
   - File → Open → select the `keiko-engine` folder
   - IntelliJ detects the Gradle project, downloads the wrapper JAR + Gradle 8.x + all dependencies on first import (~3-5 min one time)
   - Set Project SDK to 21 if prompted

5. **Run the app**:
   - **From IntelliJ**: green ▶ next to `KeikoEngineApplication.main()`
   - **From terminal**: `.\run.ps1`

6. **Open http://localhost:8080** — you should see the masthead `Lex / US Federal Constitutional Law`, a card from the 50-card bank, and a Mandarin pithy saying scrolling at the bottom (the cross-subject motivation pool).

---

## First-time local setup (Mac)

1. **Install JDK 21**:
   ```bash
   brew install --cask temurin@21
   ```

2. **Clone + open**:
   ```bash
   git clone https://github.com/tianxia-diyidao/keiko-engine.git
   cd keiko-engine
   open -a "IntelliJ IDEA" .
   ```

3. **Run**: `make run` or IntelliJ's ▶ button.

---

## First-time Fly.io deploy

You only do this **once**; subsequent deploys are just `fly deploy`.

1. **Install flyctl** (Windows PowerShell):
   ```powershell
   iwr https://fly.io/install.ps1 -useb | iex
   ```
   Or on Mac: `brew install flyctl`.

2. **Sign up** (free tier — 3 shared VMs, no credit card required for the basic plan):
   ```bash
   fly auth signup
   ```
   Or if you already have an account: `fly auth login`.

3. **Pick a unique app name** — Fly app names are globally unique. Edit `fly.toml`:
   ```toml
   app = "keiko-engine-jerry"   # change to YOUR handle
   ```

4. **Launch the app on Fly** (one-time provisioning):
   ```bash
   fly launch --no-deploy --copy-config
   ```
   Accept the suggested settings (or pick a different region than `sjc` — see `fly platform regions`). The `--no-deploy` flag means it just creates the app + reads `fly.toml`; we'll deploy explicitly next.

5. **Deploy**:
   ```bash
   fly deploy
   ```
   This builds the multi-stage Dockerfile (Stage 1: Gradle build → fat JAR; Stage 2: slim JRE + JAR) and pushes to Fly. First build takes ~3-5 min (Gradle dependency download); subsequent builds are ~1-2 min.

6. **Open the live app**:
   ```bash
   fly open
   ```
   → opens https://keiko-engine-jerry.fly.dev (or whatever you named it).

---

## Project layout

```
keiko-engine/
├── README.md, CLAUDE.md, .gitignore
├── settings.gradle.kts, build.gradle.kts
├── Dockerfile, fly.toml                  ← deploy
├── run.ps1, Makefile                     ← one-command local
├── src/main/
│   ├── java/io/tenka/keiko/
│   │   ├── app/    KeikoEngineApplication
│   │   ├── domain/ Card, Choice
│   │   ├── subject/ Subject + SubjectLoader
│   │   ├── service/ CardStore, PickerService, MotivationService
│   │   └── web/    IndexController, ApiController, dto/...
│   └── resources/
│       ├── application.yml
│       ├── subjects/us-conlaw/   ← cards.json (50) + subject.toml
│       ├── motivation/zh.json    ← 12 ZH pithy quotes (cross-subject)
│       ├── templates/index.html  ← Thymeleaf SPA shell
│       └── static/css,js/
└── src/test/java/io/tenka/keiko/...      ← JUnit smoke + integration tests
```

## Adding a new subject

```bash
mkdir -p src/main/resources/subjects/calculus-i
# author calculus-i/subject.toml + calculus-i/cards.json
# (use src/main/resources/subjects/us-conlaw/ as a template)
.\run.ps1 -SubjectId calculus-i
```

Engine code, templates, motivation pools — none change. Adding subjects is the cheap, intended path.

---

## What's IN this v0.1 / what's queued

**In v0.1**:
- 50 ConLaw multiple-choice cards (federalism, separation of powers, 1A, EP, DP, criminal procedure, standing/justiciability)
- Random picker over default deck
- Subject-agnostic content layer (TOML manifest + classpath cards.json)
- 12 ZH motivation entries (cross-subject)
- Thymeleaf-rendered SPA shell + minimal JS picker
- Dockerfile + fly.toml for one-command deploy

**Queued for follow-up PRs**:
- SQLite + JPA persistence + per-card review state (EWMA)
- Adaptive picker (port from Python sibling)
- Per-user session (correct/answered counts, multi-device)
- Tutor invite flow (24h time-boxed login)
- BasicAuth gate (production lock)
- Standing-rule motivation cycle (12-15 entries per PR, parity-routed)
- Migration to a sister Python `study-engine-py` for the engine, with subjects shared via git submodule

---

🤖 Initial scaffold authored with [Claude Code](https://claude.com/claude-code).
