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

### 5.2 Content additions (50 validated cards per PR for the active subject)

**Updated PR #3 (v0.3)**: every PR adds **50 validated cards** to the active subject's `cards.json`, spread roughly evenly across major topics. For us-conlaw, the topic mix:
- Federalism (~8): Commerce Clause, Dormant Commerce, 10A, preemption, P&I, spending, sovereign immunity, treaty/foreign affairs
- Federal Courts / Justiciability (~6): Erie, abstention (Younger / Pullman / Burford / Colorado River), Ex parte Young, jurisdiction
- State sovereignty (~4): Garcia, Alden, FMC, Boerne § 5
- Separation of powers (~8): Youngstown, Chadha, Bowsher, Mistretta, Morrison/Seila, Free Enterprise Fund, Zivotofsky, Trump v. Hawaii, Biden v. Nebraska, major-questions cases
- 1A (~7): incitement (Brandenburg), public concern (Snyder), political speech (Citizens United, Janus), compelled speech (303 Creative), Free Exercise (Trinity Lutheran, Carson, Espinoza)
- EP / DP (~6): Glucksberg methodology, Lawrence, Windsor, SFFA, Plyler, Rodriguez
- **Death penalty** (~3): Furman, Gregg, Atkins, Roper, Hall, Bucklew (covered explicitly per 先輩)
- 4A / 5A / 6A criminal procedure (~5): Mapp, Terry, Strickland, Batson, Ramos
- 8A (~3): Roper, Hall, Bucklew (covered explicitly per 先輩)

Mark `provenance: human_curated` when citing well-settled black-letter doctrine with the leading SCOTUS opinion. Mark `experimental` only if uncertain about a holding's specifics. Tutor (or 先輩 himself, with bar background) validates and promotes if needed.

### 5.2a Citation policy (NEW PR #3)

Every card MUST include at least one citation in the `citations` array, with `label` and `url`. Citation tier order:
1. **SCOTUS first** — primary authority, always cited if a SCOTUS opinion is on point
2. **Federal circuit courts** — supplement when the area is still developing or when a circuit split is heading to cert
3. **Top law reviews** (secondary authority for scholarly treatment)
   - **FAVORED**: University of Texas School of Law (Austin) — Texas Law Review
   - **ESPECIALLY FAVORED**: UT Austin's *Review of Litigation*
   - Other top tier (in rough rank order): Harvard, Yale, Stanford, Columbia, Chicago, NYU, Penn, Virginia, Michigan
4. **Type field** classifies the citation: `"scotus"` | `"circuit"` | `"law-review"` | `"other"` — drives the back-face marker (◆ SCt, ▲ CCA, ◇ LR, • other)

**URL convention (PR #8 update — Google Search, with supremecourt.gov keyword bias for SCOTUS cites)**:

```
SCOTUS:        https://www.google.com/search?q=<urlencoded label>+supremecourt.gov
Circuit / LR:  https://www.google.com/search?q=<urlencoded label>
```

Why: Justia's per-case URL pattern (`supreme.justia.com/cases/federal/us/<vol>/<page>/`) doesn't resolve for every case — particularly recent slip-opinions where the U.S. Reports volume hasn't been published yet (先輩 saw "Page not found" on a Justia link in v0.3). Google Search reliably surfaces a working hit. **PR #8 added a `supremecourt.gov` keyword for SCOTUS cites** so Google ranks the official site (where the case PDFs live at `https://www.supremecourt.gov/opinions/...`) higher — 先輩 prefers the official PDF format. Soft bias, not a `site:` restriction, so older cases not on supremecourt.gov still return useful hits.

Script-helpers (`scripts/add_pr*_*.py`) provide `cite_scotus()` / `cite_circuit()` / `cite_lr()` factories. The `cite_scotus` factory automatically appends the `supremecourt.gov` keyword. **Always use these factories** for new cards — never construct citation URLs by hand.

The frontend renders citations as clickable links. Multiple citations are listed lead-authority first.

### 5.3 Version bump

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
- v0.8 (PR #8 — even → JP): SCOTUS citation URLs now favor supremecourt.gov in Google ranking (append `supremecourt.gov` keyword to the search query, soft bias not `site:` restriction so older cases not on supremecourt.gov still surface useful hits) — owner prefers the official PDF format. Backfilled all 576 SCOTUS citations across 300 existing cards via `scripts/backfill_pr8_supremecourt_urls.py`; the `cite_scotus()` factory in new card-add scripts encodes the new convention. CLAUDE.md §5.2a updated. + 50 NEW validated us-conlaw cards (cards 301-350: Comstock N&P · Sveen Contract Clause · Saenz right-to-travel · Garcia structural · Zschernig/Garamendi foreign-affairs preemption · Bond/Holland Treaty Power · Dole spending · Allapattah supplemental jurisdiction · Mottley well-pleaded complaint · Harlow/Pearson qualified immunity · Egbert Bivens narrowing · Anti-Injunction Act · Dukes/Comcast class action · College Savings/Allen v. Cooper IP abrogation · Florida Prepaid · PennEast eminent domain · Bowsher · Morrison v. Olson · Klein/McCardle jurisdiction-stripping · Treaty Clause/Medellin · Dames & Moore executive agreements · Hamdi/Boumediene/Hamdan war on terror · Snyder v. Phelps · US v. Alvarez Stolen Valor · US v. Stevens · Pleasant Grove/Walker government speech · Garcetti/Pickering · Forsyth/Lakewood prior restraint · Counterman v. Colorado true threats · Yick Wo · Loving v. Virginia · Romer v. Evans · Cleburne rational-basis-with-bite · Trinity Lutheran/Carson · Dobbs life-of-mother · Coker/Kennedy v. Louisiana · Enmund/Tison · Atkins/Hall/Moore · Florida v. Jardines · US v. Jones GPS · Kyllo · Hudson v. Michigan · Strieff · Brown v. Plata · Wilkinson v. Austin · Kelo v. New London) + 13 JP motivation entries (因果応報 · 一意専心 · 一所懸命 · 義を見てせざるは勇なきなり · 雲外蒼天 · 一陽来復 · 大器晩成 · 諸行無常 · 言うは易く行うは難し · 雪に耐えて梅花麗し · 桃栗三年柿八年 · 急いては事を仕損じる · 一念岩をも通す)
- v0.7 (PR #7 — odd → ZH): run.ps1 / Makefile run-from-main policy (always `git checkout main` then `git pull --ff-only` before booting; refuses to switch if working tree is dirty — owner reviews/merges PRs on GitHub, so main is the source of truth for local runs) + run.ps1 ASCII-only output (was breaking PowerShell 5.1's parser when saved as UTF-8-no-BOM — `→`/emoji bytes get interpreted as Windows-1252) + 50 NEW validated us-conlaw cards (cards 251-300: federalism / fed-courts / state sovereignty / sep-of-powers / 1A / EP-DP / death penalty / 4A-5A-6A / 8A non-capital — Reeves/Pike/Wayfair/Raich/Hibbs/Tennessee-Lane/Garcia/Rucho/Hunt/Laidlaw/Abbott-Labs/TransUnion/Stern/Seminole-Tribe/Katz/US-Georgia/Mistretta/Whitman/Zivotofsky/Dept-Commerce-NY/Trump-Hawaii/Free-Enterprise-Fund/Reed-Gilbert/303-Creative/Kennedy-Bremerton/Marsh-Town-Greece/Hosanna-Tabor/American-Legion/Smith-Cal/Morales-Santana/VMI/Wiesenfeld/Orr/Salyer/Mills-Jeter/Hurst/Madison-Ford/Kahler/Riley/Carpenter/Birchfield/Maryland-King/Padilla/Hudson/Solem-Ewing/Timbs-as-applied) + 13 ZH motivation entries (大智若愚 · 厚積薄發 · 知行合一 · 寧靜致遠 · 任重道遠 · 一鼓作氣 · 守株待兔 · 居安思危 · 名正言順 · 朝聞道夕死可矣 · 過猶不及 · 木秀於林風必摧之 · 三思而後行)
- v0.6 (PR #6 — even → JP): SessionTimeoutFilter (180-min idle window, only active when BasicAuth is enabled, rotates `WWW-Authenticate` realm to force browser re-prompt) + WebFilterConfig (explicit `FilterRegistrationBean` ordering: BasicAuth → SessionTimeout) + run.ps1 / Makefile do `git pull --ff-only` ALWAYS (not just on main) + `-WithAuth` switch (run.ps1) and `make run-auth` for local BasicAuth testing (creds dev/dev) + 50 NEW validated us-conlaw cards (cards 201-250: federalism / fed-courts-justiciability / state sovereignty / sep-of-powers / 1A / EP-DP / death penalty / 4A-5A-6A / 8A non-capital — Lopez/Murphy/Sebelius/Pullman/Erie/Rooker-Feldman/Lujan/FMC/Alden/Boerne/Youngstown/Chadha/Clinton-NYC/Noel-Canning/Seila-Law/major-questions/Trump-v-US/Pentagon-Papers/Tam/McCutcheon/Rosenberger/Janus/Mahanoy/Mathews/Lawrence/Obergefell/Dobbs/Roper/Glossip/McCleskey/Leon/Quarles/Edwards-Shatzer/Bullcoming/Batson/Graham-Miller/Estelle/Timbs) + 13 JP motivation entries (心頭滅却 · 一寸の光陰軽んずべからず · 蒔かぬ種は生えぬ · 出る杭は打たれる · 鬼に金棒 · 一を聞いて十を知る · 弘法も筆の誤り · 笑う門には福来たる · 早起きは三文の徳 · 二兎を追う者は一兎をも得ず · 鶏口となるも牛後となるなかれ · 君子豹変す · 忠言耳に逆らう)
- v0.5 (PR #5 — odd → ZH): BasicAuth filter (env-driven, opt-in via BASIC_AUTH_USER + BASIC_AUTH_PASS; mirrors Python sibling) + 50 NEW validated us-conlaw cards (cards 151-200: 4A continuations, 5A invocation, 6A speedy/confrontation, 8A status/juveniles, habeas/Guantanamo, takings full set, recent standing, press/access, affirmative-action history, race-conscious districting, religion - more displays/RFRA/RLUIPA, structural federalism, 1A speech history) + 13 ZH motivation entries (千軍易得 · 前事不忘 · 燕雀安知鴻鵠 · 塞翁失馬 · 他山之石 · 君子之交淡如水 · 讀萬卷書行萬里路 · 繩鋸木斷水滴石穿 · 海內存知己天涯若比鄰 · 不入虎穴焉得虎子 · 言必信行必果 · 百川歸海有容乃大)
- v0.4 (PR #4 — even → JP): Google Search URL fix (Justia URLs were 404ing on recent slip-ops; switched ALL 196 citation URLs to Google Search format) + run.ps1 / Makefile do `git pull --ff-only` first when on main + 50 NEW validated us-conlaw cards (cards 101-150: voting rights / 14A incorporation / 1A speech & religion / sex+LGBT+race EP / 4A criminal procedure / punitive damages DP / personal jurisdiction / recent SCOTUS — Sackett, Moore v. US, FDA v. Alliance) + 12 JP motivation entries (石の上にも三年 · 塵も積もれば · 良薬は口に苦し · 光陰矢の如し · 百聞は一見に如かず · 井の中の蛙 · 三人寄れば文殊 · 覆水盆に返らず · 情けは人の為ならず · 塞翁が馬 · 和を以て貴しと為す · 隗より始めよ)
- v0.3 (PR #3 — odd → ZH): citations-with-links feature (Citation record + CardDto exposes them + JS renders as `<a target="_blank">` + CSS) + backfill of all 50 existing cards with citations + **50 NEW validated us-conlaw cards (cards 051-100)** spread across federalism / fed-courts / state sovereignty / sep-of-powers / 1A / EP-DP / **death penalty** / 4A-5A-6A / **8A** + 12 ZH motivation entries (業精於勤 · 天下大事必作於細 · 知者不惑 · 工欲善其事 · 玉不琢不成器 · 三人行必有我師焉 · 學而不思則罔 · 君子求諸己 · 勿以惡小而為之 · 自強不息 · 靜以修身 · 失之東隅) + CLAUDE.md §5.2 standing-rule update (50-cards/PR) + §5.2a citation policy (UT Austin / Review of Litigation favored)

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
- ~~**BasicAuth gate**~~ — done in v0.5; idle-session timeout (180 min, realm-rotation re-prompt) done in v0.6
- **Tutor invite flow** (24h time-boxed login) — once the user system grows
- **GitHub Actions CI** — `./gradlew test` on push, auto-deploy to Fly on merge to main
- **Front-end polish** — v0.1 template is utilitarian; full styling parity with the JLPT sibling's paper/ink aesthetic is a fast follow

頑張って、Natsumi-san. The bar-prep deck is in your hands now.

— Initial scaffold, May 2026
