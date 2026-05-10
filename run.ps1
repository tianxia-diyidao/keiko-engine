# keiko-engine -- one-command local run on Windows.
#
# Usage:
#   .\run.ps1                  # default: switch to main + pull + bootRun
#   .\run.ps1 -Test            # also run tests before serving
#   .\run.ps1 -SubjectId calc  # override active subject
#   .\run.ps1 -WithAuth        # enable BasicAuth locally (test creds: dev / dev)
#   .\run.ps1 -NoPull          # skip the auto-pull (and don't switch branches)
#
# Workflow assumption (per repo owner):
#   You review/approve/merge PRs on GitHub. So whenever you run locally, you
#   want to run MAIN's latest -- not whichever feature branch the working tree
#   happens to be on. This script enforces that: it switches to main and
#   pulls before booting. If the working tree is dirty (uncommitted changes),
#   it bails out instead of clobbering your work -- commit/stash first.
#
# IMPORTANT: ASCII-only output. PowerShell 5.1 reads .ps1 files as the system
# code page (Windows-1252 in en-US) unless they have a UTF-8 BOM. Emoji /
# arrows / smart quotes break the parser when this script is saved as
# UTF-8-no-BOM by editors that don't add the BOM (most cross-platform
# editors). Use ">>", "-->" or "==>" instead.
#
# First-time setup:
#   1. Install JDK 21:    winget install --id EclipseAdoptium.Temurin.21.JDK
#   2. Open this folder in IntelliJ IDEA -- it auto-imports the Gradle project
#   3. Run this script, OR use IntelliJ's run config "KeikoEngineApplication".

param(
    [switch]$Test,
    [switch]$NoPull,
    [switch]$WithAuth,
    [string]$SubjectId = "us-conlaw"
)

$ErrorActionPreference = "Stop"

Write-Host "==> keiko-engine local run" -ForegroundColor Cyan
Write-Host "    STUDY_SUBJECT = $SubjectId"

# Step 0: switch to main and pull latest. The whole point of "run" is to test
# the merged production code; the user merges via GitHub, so main is truth.
if (-not $NoPull) {
    $branch = (git rev-parse --abbrev-ref HEAD).Trim()

    if ($branch -ne "main") {
        # Refuse to switch if there are uncommitted changes -- losing work via
        # an automatic checkout is exactly the kind of thing the agent rules
        # call out as a destructive shortcut.
        $dirty = (git status --porcelain) | Out-String
        if ($dirty.Trim().Length -gt 0) {
            Write-Host "XX working tree on '$branch' has uncommitted changes:" -ForegroundColor Red
            Write-Host $dirty -ForegroundColor Red
            Write-Host "   commit/stash before running, or use -NoPull to boot from this branch as-is." -ForegroundColor Red
            exit 1
        }
        Write-Host "==> on '$branch'; switching to main per run-from-main policy" -ForegroundColor Yellow
        git checkout main
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    }

    Write-Host "==> git pull --ff-only  (on main)" -ForegroundColor Cyan
    git pull --ff-only
    if ($LASTEXITCODE -ne 0) {
        Write-Host "XX git pull failed. Resolve and retry, or run with -NoPull." -ForegroundColor Red
        exit $LASTEXITCODE
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
    Write-Host "==> BasicAuth ENABLED locally (user=$env:BASIC_AUTH_USER)" -ForegroundColor Yellow
}

if ($Test) {
    Write-Host "==> running tests first..." -ForegroundColor Cyan
    .\gradlew.bat --no-daemon test
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

Write-Host "==> booting on http://localhost:8080" -ForegroundColor Cyan
.\gradlew.bat --no-daemon bootRun
