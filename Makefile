# keiko-engine — one-command local run on Mac/Linux.
#
# Usage:
#   make run         — clean + bootRun (default subject = us-conlaw)
#   make test        — run JUnit suite
#   make build       — produce build/libs/keiko-engine.jar
#   make deploy      — fly deploy (requires `fly auth login` first)
#
# First-time setup (Mac):
#   1. Install JDK 21:    brew install --cask temurin@21
#   2. Open in IntelliJ IDEA — auto-imports Gradle project
#   3. `make run`

SUBJECT ?= us-conlaw
GRADLE = ./gradlew --no-daemon

.PHONY: run run-auth run-nopull test build deploy clean

run:
	@echo "→ keiko-engine local run (STUDY_SUBJECT=$(SUBJECT))"
	@echo "→ git pull --ff-only (on branch $$(git rev-parse --abbrev-ref HEAD))"
	@git pull --ff-only || echo "  (pull skipped — no upstream or non-ff)"
	@echo "→ booting on http://localhost:8080"
	STUDY_SUBJECT=$(SUBJECT) $(GRADLE) bootRun

run-auth:
	@echo "→ keiko-engine local run with BasicAuth (user=dev / pass=dev)"
	@git pull --ff-only || echo "  (pull skipped — no upstream or non-ff)"
	@echo "→ booting on http://localhost:8080"
	STUDY_SUBJECT=$(SUBJECT) BASIC_AUTH_USER=dev BASIC_AUTH_PASS=dev $(GRADLE) bootRun

run-nopull:
	@echo "→ keiko-engine local run (no pull, STUDY_SUBJECT=$(SUBJECT))"
	STUDY_SUBJECT=$(SUBJECT) $(GRADLE) bootRun

test:
	$(GRADLE) test

build:
	$(GRADLE) bootJar

deploy:
	@echo "→ fly deploy"
	fly deploy

clean:
	$(GRADLE) clean
