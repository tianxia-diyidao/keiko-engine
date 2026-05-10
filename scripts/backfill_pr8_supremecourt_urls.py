"""PR #8 (v0.8) -- backfill SCOTUS citation URLs to favor supremecourt.gov
in the Google Search results, since the owner prefers the official PDFs.

Strategy: append " supremecourt.gov" as a keyword to the existing label in
the search query for any citation with type == "scotus". This BIASES Google's
ranking toward the official site (where the case PDFs live at
https://www.supremecourt.gov/opinions/...) without hard-restricting the
search via `site:` -- older cases not on supremecourt.gov still return
useful hits.

Circuit / law-review / other citations are unchanged (those don't live on
supremecourt.gov, so the keyword wouldn't help and might hurt).

Run from repo root:
    python scripts\\backfill_pr8_supremecourt_urls.py
"""
from __future__ import annotations

import json
from pathlib import Path
from urllib.parse import quote_plus

CARDS_PATH = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "subjects" / "us-conlaw" / "cards.json"

SUPREMECOURT_HINT = "supremecourt.gov"


def scotus_google_url(label: str) -> str:
    return f"https://www.google.com/search?q={quote_plus(label + ' ' + SUPREMECOURT_HINT)}"


def main():
    data = json.loads(CARDS_PATH.read_text(encoding="utf-8"))
    cards = data["cards"]
    changed_citations = 0
    changed_cards = 0
    for c in cards:
        card_changed = False
        for cite in c.get("citations", []):
            if cite.get("type") != "scotus":
                continue
            new_url = scotus_google_url(cite["label"])
            if cite.get("url") != new_url:
                cite["url"] = new_url
                changed_citations += 1
                card_changed = True
        if card_changed:
            changed_cards += 1
    tmp = CARDS_PATH.with_suffix(".json.tmp")
    tmp.write_text(json.dumps({"cards": cards}, ensure_ascii=False, indent=2) + "\n",
                   encoding="utf-8")
    tmp.replace(CARDS_PATH)
    print(f"Updated {changed_citations} SCOTUS citations across {changed_cards} cards. "
          f"(Total cards: {len(cards)}.)")


if __name__ == "__main__":
    main()
