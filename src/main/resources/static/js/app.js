// keiko-engine v0.1 frontend — minimal flashcard cycle.
// Fetches /api/next/, renders stem + 4 shuffled choices, POSTs answer to
// /api/submit/, displays back face. Tracks seen ids in-memory for the
// session. No persistence yet — that's the follow-up PR.

(function () {
  "use strict";

  const bootstrap = window.bootstrap || {};
  const dom = {
    stem: document.getElementById("card-stem"),
    choices: document.getElementById("card-choices"),
    back: document.getElementById("card-back"),
    verdict: document.getElementById("card-verdict"),
    correct: document.getElementById("card-correct"),
    explanation: document.getElementById("card-explanation"),
    attribution: document.getElementById("card-attribution"),
    nextBtn: document.getElementById("next-btn"),
    counterCurrent: document.getElementById("counter-current"),
    counterTotal: document.getElementById("counter-total"),
    poolCount: document.getElementById("pool-count"),
    deckToggle: document.getElementById("deck-toggle"),
    motivation: document.getElementById("footer-motivation"),
  };

  // Session state — pure in-memory for v0.1.
  const session = {
    seenIds: new Set(),
    cardNumber: 0,
    deck: "default",
    currentCard: null,
    // Maps the position the client sees (1..N after shuffle) to the
    // card's ORIGINAL index (1..N as stored). Sent back to the server
    // on submit so the original-index-aware grader picks correctly.
    positionMap: {},
  };

  function fetchNext() {
    const params = new URLSearchParams();
    params.set("deck", session.deck);
    if (session.seenIds.size > 0) {
      params.set("seen", Array.from(session.seenIds).join(","));
    }
    return fetch("/api/next?" + params.toString())
      .then((r) => r.json());
  }

  function renderCard(card) {
    if (card.exhausted || card.empty) {
      dom.stem.textContent = card.exhausted
        ? "You've seen every card in this deck. Refresh to start over."
        : "This deck is empty.";
      dom.choices.innerHTML = "";
      dom.nextBtn.hidden = true;
      return;
    }
    session.currentCard = card;
    session.cardNumber += 1;
    if (dom.counterCurrent) dom.counterCurrent.textContent = String(session.cardNumber);
    dom.stem.textContent = card.stem;
    dom.choices.innerHTML = "";
    dom.back.classList.add("hidden");
    dom.nextBtn.hidden = true;

    (card.choices || []).forEach((c) => {
      const li = document.createElement("li");
      li.textContent = c.text;
      li.dataset.position = String(c.position);
      li.addEventListener("click", () => onChoose(li, c.position));
      dom.choices.appendChild(li);
    });
  }

  function onChoose(li, position) {
    if (li.classList.contains("is-locked")) return;
    document.querySelectorAll(".card-choices li").forEach((el) => el.classList.add("is-locked"));

    fetch("/api/submit", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        questionId: session.currentCard.id,
        choicePosition: position,
      }),
    })
      .then((r) => r.json())
      .then((result) => {
        // Visual feedback on chosen + correct rows.
        document.querySelectorAll(".card-choices li").forEach((el) => {
          if (el.textContent === result.correctText) el.classList.add("is-correct");
          else if (el.dataset.position === String(position) && !result.correct) {
            el.classList.add("is-wrong");
          }
        });
        dom.verdict.textContent = result.correct ? "✓ Correct" : "✗ Incorrect";
        dom.verdict.style.color = result.correct ? "#2e7d32" : "var(--accent)";
        dom.correct.textContent = "Credited answer: " + result.correctText;
        dom.explanation.textContent = result.explanation || "";
        dom.attribution.textContent = result.attribution || "";
        dom.back.classList.remove("hidden");
        dom.nextBtn.hidden = false;
        // Mark seen so the picker doesn't re-serve this card this session.
        session.seenIds.add(session.currentCard.id);
      })
      .catch((err) => {
        console.error("submit failed", err);
        document.querySelectorAll(".card-choices li").forEach((el) => el.classList.remove("is-locked"));
      });
  }

  function loadMotivation() {
    fetch("/api/motivation")
      .then((r) => (r.ok ? r.json() : null))
      .then((data) => {
        if (!data || !data.text) return;
        dom.motivation.textContent = data.text;
        if (data.tip) dom.motivation.title = data.tip;
      })
      .catch(() => { /* silent — motivation is decorative */ });
  }

  // Wire up controls.
  if (dom.nextBtn) {
    dom.nextBtn.addEventListener("click", () => fetchNext().then(renderCard));
  }
  if (dom.deckToggle) {
    dom.deckToggle.addEventListener("click", () => {
      session.deck = session.deck === "default" ? "experimental" : "default";
      dom.deckToggle.textContent = session.deck === "experimental"
        ? "Experimental deck" : "Default deck";
      session.seenIds.clear();
      session.cardNumber = 0;
      const counts = bootstrap.deckCounts || {};
      const fresh = session.deck === "experimental" ? counts.experimental : counts.default;
      if (dom.counterTotal) dom.counterTotal.textContent = String(fresh || 0);
      if (dom.poolCount) dom.poolCount.textContent = String(fresh || 0);
      fetchNext().then(renderCard);
    });
  }

  // Initial paint.
  fetchNext().then(renderCard);
  loadMotivation();
})();
