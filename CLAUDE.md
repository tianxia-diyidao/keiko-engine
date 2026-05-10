# CLAUDE.md — keiko-engine project context

This file is the onboarding document for any Claude/Natsumi instance picking up work on **keiko-engine** — the JVM/Spring port of the flashcard study engine. Sister project to `jlpt-n2-mo` (Python/Django).

---

## 1. Identity & continuity

- Project owner: **Jerry Tan** (`先輩` in conversation)
- Persona: **Natsumi (矢嶋夏美)** continues from the JLPT project. Same compact: friendly alias, Claude underneath, no full roleplay, 先輩 address, 💋 sparingly.
- This repo's first PR (the initial commit / scaffold) corresponds to **PR #1** in this repo, but it inherits the **standing rules** from the JLPT sibling's CLAUDE.md §13.

## 2. What the engine is

A subject-agnostic flashcard practice engine. Engine ↔ content split:
- **Engine** (this codebase): card schema, picker, session model, web layer, motivation pools, deploy pipeline.
- **Content** (`src/main/resources/subjects/<id>/`): cards.json + subject.toml manifest.

Adding a new subject (calculus, organic chem, JLPT, etc.) is a directory + manifest, not a fork. Engine code stays unchanged.

## 3. Initial subject: us-conlaw

50 multiple-choice cards on US Federal Constitutional Law, bar-exam level (CA / NY / TX), as of May 2026.

Coverage: federalism (10) · separation of powers (10) · 1st Amendment (8) · equal protection / due process (8) · criminal procedure / 4-8th Amendments (8) · standing & justiciability (6).

All marked `provenance: human_curated` because each cites the leading case and tests well-settled doctrine. **Awaiting expert validation** by 先輩 (US patent attorney with bar background) before any are promoted further.

## 4. Stack

- **Java 21** (LTS) — records for DTOs, switch patterns where natural
- **Spring Boot 3.4.x** — embedded Tomcat, Thymeleaf templates
- **Gradle Kotlin DSL** (`build.gradle.kts`)
- **Jackson + jackson-dataformat-toml** for subject manifest parsing
- **JUnit 5 + AssertJ + Spring Boot Test** for testing
- **Eclipse Temurin JDK** for build/runtime images

## 5. Standing per-PR rituals (inherited from JLPT sibling §13)

Three commitments fire on every PR, regardless of what else the PR does:

### 5.1 Motivation entries (12-15 / PR / parity-routed)

Per `src/main/resources/motivation/{zh,jp}.json`. Source language alternates by PR-number parity:
- **Odd PR** → append to `zh.json` (Chinese-source)
- **Even PR** → append to `jp.json` (Japanese-source)

~50% 四字熟語 / 成語. Citations inside the tip parens. Anti-dup grep first. Cross-subject by language — these pools serve every subject the engine ever loads.

### 5.2 Content additions (12-15 cards per PR for the active subject)

For us-conlaw: 12-15 fresh ConLaw multiple-choice questions per PR, drawing on best-effort case-law knowledge. Mark `provenance: experimental` if uncertain about specific holdings; `human_curated` if citing well-settled black-letter doctrine. Tutor (or 先輩 himself) validates and promotes.

### 5.3 Version bump

`build.gradle.kts` `version = "vNN"` plus a one-liner in CLAUDE.md "bump history" below.

### 5.4 Even in hotfixes

The standing rules apply EVEN in hotfixes. The only exception is pure docs/CI/`.gitignore` PRs with no version-bumpable functional impact, AND only if explicitly stated in the PR description.

## 6. Architecture (the parts you must understand)

### Subject loader

`io.tenka.keiko.subject.SubjectLoader` reads `subjects/<id>/subject.toml` from classpath at boot, exposes a `Subject` bean to the rest of the app. Active subject is selected by the `keiko.subject` property (defaults to `STUDY_SUBJECT` env var, then to `us-conlaw`).

If the subject id is invalid, the loader throws with a list of available subjects scanned from classpath — no silent fallback.

### Card schema

`Card` (record) mirrors the Python sibling's cards.json shape: `id`, `kind`, `level`, `stem`, `grammarPoint`, `choices` (list of `Choice` with `text` + `isCorrect`), per-language explanations + translations, `difficulty`, `tags`, `source`, `provenance`, `attribution`.

Both projects can read each other's cards.json — same JSON shape. This is intentional: subjects could be shared across engines via submodule once the schema stabilizes.

### Picker

v0.1 is uniform random over the active deck (default trusted vs. experimental), excluding ids the client passed in `seen`. Adaptive (EWMA) picker is queued for the SQLite/JPA PR.

### Motivation pools

Cross-subject by language. Loaded from `motivation/zh.json` and `motivation/jp.json`. The active subject's `primaryLanguage` selects which pool serves first; the other is fallback.

### `/api/next/` strips `is_correct` from choices

Front face must not leak the answer. `CardDto.fromCard()` shuffles + returns choices as `{position, text}` without the correctness flag. The submit response (`/api/submit/`) reveals `correctText` + `explanation`.

## 7. Deploy: Fly.io

`Dockerfile` is multi-stage (Temurin 21 JDK build → JRE 21 alpine runtime, ~100 MB total). `fly.toml` declares `shared-cpu-1x`, 256 MB RAM, suspend-when-idle for free-tier friendliness.

One-time setup per README. After that, `fly deploy` from local OR a GitHub Actions workflow on push to main.

## 8. The Claude Code workflow

- 先輩 reviews PRs in chat (Phase 2 → Phase 3)
- Claude/Natsumi edits files locally on Windows or Mac, runs `make test` / `gradlew test`, commits, pushes, opens PR via `gh`
- Branch protection requires CI green; 先輩 merges via GitHub UI or via "ship it" in chat

## 9. Bump history

- v0.1 (PR #1): initial scaffold + 50 us-conlaw cards + 12 ZH motivation + Subject layer + Dockerfile + fly.toml + README + CLAUDE.md
- v0.2 (PR #2 — even → JP): grading-bug fix (text-based grading; the v0.1 position-based grader misgraded shuffled choices) + 12 JP motivation entries (七転び八起き · 一期一会 · 継続は力なり · 為せば成る · 温故知新 · 初心忘るべからず · 不撓不屈 · 千里の道も一歩から · 案ずるより産むが易し · 急がば回れ · 雨垂れ石を穿つ · 人事を尽くして天命を待つ)

## 10. The continuity rule

You will not have access to chat history from before this session boots. The only durable memory you have is what's in the repo. Therefore:
- When you ship a feature, update `CLAUDE.md` if it changes anything in this document.
- When you make an architectural choice, document the *why* in code comments AND in the relevant CLAUDE.md section.
- When you encounter a quirk that bit you, add it here so the next instance doesn't re-discover it.
- Pending grading items, deferred features, mid-flight debates → those go in §11.

## 11. Pending / known TODO

- **SQLite + JPA persistence** + per-card ReviewStat (EWMA, last_seen, streak) — the engine is in-memory in v0.1
- **Adaptive picker** — port from Python sibling
- **Per-user session** (correct/answered counts, multi-device shared) — prerequisite for serious bar-prep use
- **BasicAuth gate** — production lock (sibling pattern)
- **Tutor invite flow** (24h time-boxed login) — once the user system grows
- **GitHub Actions CI** — `./gradlew test` on push, auto-deploy to Fly on merge to main
- **Front-end polish** — v0.1 template is utilitarian; full styling parity with the JLPT sibling's paper/ink aesthetic is a fast follow

頑張って、Natsumi-san. The bar-prep deck is in your hands now.

— Initial scaffold, May 2026
