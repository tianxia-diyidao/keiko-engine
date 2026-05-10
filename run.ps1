# keiko-engine — one-command local run on Windows.
#
# Usage:
#   .\run.ps1                  # default: pull + bootRun
#   .\run.ps1 -Test            # also run tests before serving
#   .\run.ps1 -SubjectId calc  # override active subject
#   .\run.ps1 -WithAuth        # enable BasicAuth locally (test creds: dev / dev)
#   .\run.ps1 -NoPull          # skip the auto-pull (useful offline)
#
# First-time setup:
#   1. Install JDK 21:    winget install --id EclipseAdoptium.Temurin.21.JDK
#      (or download from https://adoptium.net)
#   2. Open this folder in IntelliJ IDEA — it auto-imports the Gradle project
#      and downloads the wrapper JAR on first build.
#   3. Run this script, OR use IntelliJ's run config "KeikoEngineApplication".

param(
    [switch]$Test,
    [switch]$NoPull,
    [switch]$WithAuth,
    [string]$SubjectId = "us-conlaw"
)

$ErrorActionPreference = "Stop"

Write-Host "→ keiko-engine local run" -ForegroundColor Cyan
Write-Host "  STUDY_SUBJECT = $SubjectId"

# Step 0: ALWAYS try git pull --ff-only, regardless of branch. If on a feature
# branch with no upstream, git will say so and we move on. If pull fails for
# a real reason (conflict, non-ff), surface and stop. -NoPull skips for offline.
if (-not $NoPull) {
    $branch = (git rev-parse --abbrev-ref HEAD).Trim()
    Write-Host "→ git pull --ff-only  (on branch '$branch')" -ForegroundColor Cyan
    git pull --ff-only
    if ($LASTEXITCODE -ne 0) {
        # No upstream is benign on a fresh feature branch — warn but continue.
        $hasUpstream = (git rev-parse --abbrev-ref --symbolic-full-name "@{u}" 2>$null)
        if (-not $hasUpstream) {
            Write-Host "→ '$branch' has no upstream — nothing to pull, continuing." -ForegroundColor Yellow
        } else {
            Write-Host "✗ git pull failed. Resolve and retry, or run with -NoPull." -ForegroundColor Red
            exit $LASTEXITCODE
        }
    }
}

# Surface env to the bootRun task.
$env:STUDY_SUBJECT = $SubjectId

if ($WithAuth) {
    # Local dev convenience: enable BasicAuth with throwaway creds so you can
    # see the browser auth prompt without hitting Fly.io. Override by setting
    # BASIC_AUTH_USER / BASIC_AUTH_PASS in your shell before invoking.
    if (-not $env:BASIC_AUTH_USER) { $env:BASIC_AUTH_USER = "dev" }
    if (-not $env:BASIC_AUTH_PASS) { $env:BASIC_AUTH_PASS = "dev" }
    Write-Host "→ BasicAuth ENABLED locally (user=$env:BASIC_AUTH_USER)" -ForegroundColor Yellow
}

if ($Test) {
    Write-Host "→ running tests first..." -ForegroundColor Cyan
    .\gradlew.bat --no-daemon test
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

Write-Host "→ booting on http://localhost:8080" -ForegroundColor Cyan
.\gradlew.bat --no-daemon bootRun
