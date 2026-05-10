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

.PHONY: run test build deploy clean

run:
	@echo "→ keiko-engine local run (STUDY_SUBJECT=$(SUBJECT))"
	@if [ "$$(git rev-parse --abbrev-ref HEAD)" = "main" ]; then \
		echo "→ git pull --ff-only" && git pull --ff-only; \
	else \
		echo "→ on branch $$(git rev-parse --abbrev-ref HEAD); skipping git pull"; \
	fi
	@echo "→ booting on http://localhost:8080"
	STUDY_SUBJECT=$(SUBJECT) $(GRADLE) bootRun

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
