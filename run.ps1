# keiko-engine — one-command local run on Windows.
#
# Usage:
#   .\run.ps1                  # default: clean + bootRun
#   .\run.ps1 -Test            # also run tests before serving
#   .\run.ps1 -SubjectId calc  # override active subject
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
    [string]$SubjectId = "us-conlaw"
)

$ErrorActionPreference = "Stop"

# Echo what we're about to do — visibility on long-running cmds is nice.
Write-Host "→ keiko-engine local run" -ForegroundColor Cyan
Write-Host "  STUDY_SUBJECT = $SubjectId"

# Step 0: pull latest from main (skip with -NoPull when iterating on a feature branch).
if (-not $NoPull) {
    $branch = (git rev-parse --abbrev-ref HEAD).Trim()
    if ($branch -ne "main") {
        Write-Host "→ on branch '$branch' (not main); skipping git pull. Use -NoPull to silence." -ForegroundColor Yellow
    } else {
        Write-Host "→ git pull --ff-only" -ForegroundColor Cyan
        git pull --ff-only
        if ($LASTEXITCODE -ne 0) {
            Write-Host "✗ git pull failed. Resolve and retry, or run with -NoPull." -ForegroundColor Red
            exit $LASTEXITCODE
        }
    }
}

# Surface env to the bootRun task.
$env:STUDY_SUBJECT = $SubjectId

if ($Test) {
    Write-Host "→ running tests first..." -ForegroundColor Cyan
    .\gradlew.bat --no-daemon test
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

Write-Host "→ booting on http://localhost:8080" -ForegroundColor Cyan
.\gradlew.bat --no-daemon bootRun
