#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Grabio - diagnostyka: co jest na GitHubie i dlaczego Actions nie rusza.
Uruchom (albo dwuklik na sprawdz.bat) i wklej caly wynik.
"""

import subprocess

REPO = "wskakuj/Grabio"


def sh(cmd):
    print("$ " + " ".join(cmd))
    r = subprocess.run(cmd, capture_output=True, text=True,
                       encoding="utf-8", errors="replace")
    out = ((r.stdout or "") + (r.stderr or "")).strip()
    print(out if out else "(brak wyniku)")
    print("-" * 62)


print("=" * 62)
print("  DIAGNOZA GRABIO")
print("=" * 62)

print("\n### LOKALNE REPO (git)")
sh(["git", "remote", "-v"])
sh(["git", "branch", "-a"])
sh(["git", "log", "--oneline", "-8"])
sh(["git", "tag", "-l"])
sh(["git", "status", "--short"])

print("\n### GITHUB")
sh(["gh", "auth", "status"])
sh(["gh", "repo", "view", REPO, "--json", "name,visibility,defaultBranchRef"])
print("Czy plik workflow jest na GitHubie?")
sh(["gh", "api", f"repos/{REPO}/contents/.github/workflows",
    "--jq", ".[].name"])
print("Czy sekrety sa ustawione? (nazwy, bez wartosci)")
sh(["gh", "secret", "list", "--repo", REPO])
print("Ostatnie przebiegi Actions:")
sh(["gh", "run", "list", "--repo", REPO, "--limit", "5"])
print("Tagi na GitHubie:")
sh(["gh", "api", f"repos/{REPO}/tags", "--jq", ".[].name"])

print("\nGotowe. Skopiuj powyzsze i wyslij dalej.")
