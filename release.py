#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Grabio - pomocnik wydawania wersji (release)
============================================
Jednym poleceniem: synchronizuje repo z GitHubem, podbija numer wersji
w app/build.gradle.kts, commituje, wysyla zmiany i wypuszcza tag vX.Y.Z.
GitHub Actions (.github/workflows/release.yml) na tagu buduje podpisany
APK i dolacza go do Release - a aplikacja sama go potem wykryje
i zaproponuje aktualizacje.

Uzycie (w folderze repo Grabio):
    python release.py        -> kreator krok po kroku
    python release.py -k     -> bez pytania o potwierdzenie

Numer nastepnej wersji podpowiadany jest na podstawie ostatniego tagu
na GitHubie (vX.Y.Z + 1).

Wymagania: git, Python 3. Do jednorazowego utworzenia repo: gh (GitHub CLI)
albo reczne utworzenie na github.com.
"""

import re
import subprocess
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent
GRADLE = REPO / "app" / "build.gradle.kts"
NOTES = REPO / "RELEASE_NOTES.md"
OWNER = "wskakuj"
NAME = "Grabio"
REPO_URL = f"https://github.com/{OWNER}/{NAME}.git"
GITHUB_URL = f"https://github.com/{OWNER}/{NAME}"


def git(*args, check=True):
    """Uruchamia git w katalogu repo, zwraca stdout (lub konczy przy bledzie)."""
    r = subprocess.run(["git", "-C", str(REPO), *args],
                       capture_output=True, text=True,
                       encoding="utf-8", errors="replace")
    if check and r.returncode != 0:
        print("\nX BLAD git " + " ".join(args))
        if r.stdout.strip():
            print(r.stdout.strip())
        if r.stderr.strip():
            print(r.stderr.strip())
        print("\nNic nie wyslano - popraw problem i uruchom release.py ponownie.")
        sys.exit(1)
    return r.stdout.strip()


def read_current_version():
    m = re.search(r'versionName\s*=\s*"([^"]+)"',
                  GRADLE.read_text(encoding="utf-8"))
    if not m:
        print("X Nie znaleziono versionName w app/build.gradle.kts")
        sys.exit(1)
    return m.group(1)


def set_current_version(ver):
    """Ustawia versionName oraz versionCode (x*1000000+y*1000+z)."""
    s = GRADLE.read_text(encoding="utf-8")
    bez = ver.lstrip("v")
    m = re.match(r"^(\d+)\.(\d+)\.(\d+)$", bez)
    if not m:
        print("X Wersja musi byc w formacie X.Y.Z")
        sys.exit(1)
    vc = int(m.group(1)) * 1000000 + int(m.group(2)) * 1000 + int(m.group(3))
    s = re.sub(r'versionName\s*=\s*"[^"]*"', f'versionName = "{bez}"', s, count=1)
    s = re.sub(r'versionCode\s*=\s*\d+', f'versionCode = {vc}', s, count=1)
    GRADLE.write_text(s, encoding="utf-8")


def _vt(v):
    """Wersja jako krotka liczb (do porownan)."""
    m = re.match(r"^v(\d+)\.(\d+)\.(\d+)$", v or "")
    return tuple(int(x) for x in m.groups()) if m else (0, 0, 0)


def remote_tag_list():
    """Lista tagow vX.Y.Z z GitHuba (origin)."""
    out = git("ls-remote", "--tags", "origin", check=False)
    tags = []
    for line in out.splitlines():
        m = re.search(r"refs/tags/(v\d+\.\d+\.\d+)$", line.strip())
        if m:
            tags.append(m.group(1))
    return tags


def latest_remote_tag():
    tags = remote_tag_list()
    return max(tags, key=_vt) if tags else None


def next_patch(v):
    m = re.match(r"^v(\d+)\.(\d+)\.(\d+)$", v)
    if not m:
        return None
    a, b, c = (int(x) for x in m.groups())
    return f"v{a}.{b}.{c + 1}"


def ensure_remote():
    """Ustawia origin i - jesli trzeba - tworzy repo na GitHubie (gh)."""
    remotes = git("remote", check=False).split()
    if "origin" not in remotes:
        print("Ustawiam zdalne repo (origin)…")
        git("remote", "add", "origin", REPO_URL)
    # czy repo juz istnieje na GitHubie?
    if git("ls-remote", "origin", check=False):
        return
    print(f"Repo {OWNER}/{NAME} nie odpowiada. Probuje utworzyc przez gh…")
    r = subprocess.run(["gh", "repo", "create", f"{OWNER}/{NAME}",
                        "--public", "--source", str(REPO), "--remote", "origin"],
                       capture_output=True, text=True,
                       encoding="utf-8", errors="replace")
    if r.returncode != 0:
        print("\nX Nie udalo sie utworzyc repo automatycznie.")
        print("  Zrob to raz recznie: wejdz na https://github.com/new")
        print(f"  i utworz repo o nazwie {NAME} (wlasciciel {OWNER}), potem uruchom")
        print("  release.py jeszcze raz.")
        if r.stderr.strip():
            print("  gh: " + r.stderr.strip())
        sys.exit(1)


def sanitize_notes(text):
    """Czysty markdown - Release na GitHubie renderuje go w calosci."""
    text = re.sub(r"\*\*([^*]+)\*\*", r"\1", text)
    return text


def edit_changelog(ver):
    """Changelog: notepad na Windows, wpisywanie w konsoli gdzie indziej."""
    header = f"# Co nowego w Grabio {ver}\n\n"
    if sys.platform == "win32":
        NOTES.write_text(header + "- \n", encoding="utf-8")
        print("\nOtwieram Notatnik - napisz changelog, ZAPISZ i zamknij okno.")
        try:
            subprocess.run(["notepad.exe", str(NOTES)], check=False)
        except FileNotFoundError:
            pass
        raw = NOTES.read_text(encoding="utf-8")
        clean = sanitize_notes(raw)
        if clean != raw:
            NOTES.write_text(clean, encoding="utf-8")
        body = clean.strip()
        if body in (header.strip(), header.strip() + "-"):
            print("   (changelog pusty - uzyje tylko listy commitow z GitHuba)")
        return
    print("\nWpisuj linie changelogu; pusta linia konczy:")
    lines = []
    while True:
        try:
            line = input()
        except EOFError:
            break
        if not line.strip():
            break
        lines.append(line)
    NOTES.write_text(sanitize_notes(header + "\n".join(lines)) + "\n",
                     encoding="utf-8")


def main():
    print("=" * 62)
    print("  GRABIO - wydawanie nowej wersji")
    print("=" * 62)

    # 0) czy to w ogole repo gita?
    git("rev-parse", "--verify", "HEAD")
    ensure_remote()

    # 0b) synchronizacja z GitHubem - bez tego push moze zostac odrzucony
    print("Sprawdzam GitHub (git pull)…")
    r = subprocess.run(["git", "-C", str(REPO), "pull", "--rebase", "--autostash",
                        "origin", "main"],
                       capture_output=True, text=True,
                       encoding="utf-8", errors="replace")
    if r.returncode != 0:
        print()
        print("X NIE MOGLE POBRAC ZMIAN Z GITHUBA - najpewniej konflikt pliku.")
        print("  Co zrobic: w folderze repo uruchom recznie:  git pull")
        print("  rozwiaz konflikt, a potem odpal release.py jeszcze raz.")
        if r.stderr.strip():
            print(r.stderr.strip())
        sys.exit(1)

    # 1) co sie zmienilo?
    status = git("status", "--short")
    unpushed = git("log", "--branches", "--not", "--remotes", "--oneline", check=False)
    if not status and not unpushed:
        print("\nBrak zmian - drzewo robocze czyste. Nie ma czego wydawac.")
        sys.exit(0)
    if status:
        print(f"\nZmienione / nowe pliki ({len(status.splitlines())}):")
        for line in status.splitlines():
            print("   " + line)
    if unpushed:
        print("\nUwaga: sa juz commity niewyslane na GitHub -")
        print("wydanie dokonczy ich wysylke.")

    # 2) nowa wersja
    cur = read_current_version()
    remote = latest_remote_tag()
    if remote:
        print(f"Ostatnia wersja na GitHub  : {remote}")
        prop = next_patch(remote) or "v1.0.0"
    else:
        prop = f"v{cur}"
        print("(brak tagow na GitHub - to pierwsze wydanie, proponuje wersje z pliku)")
    print(f"Wersja w pliku             : {cur} (podmienie przy wydaniu)")
    try:
        ans = input(f"Nowa wersja [{prop}]: ").strip() or prop
    except EOFError:
        ans = prop
    if not re.match(r"^v\d+\.\d+\.\d+$", ans):
        print("X Wersja musi byc w formacie vX.Y.Z (np. v1.0.1)")
        sys.exit(1)
    if git("tag", "-l", ans) or ans in remote_tag_list():
        print(f"X Tag {ans} juz istnieje (lokalnie lub na GitHub) - wybierz inny numer.")
        sys.exit(1)

    # 3) opis commita
    try:
        msg = input(f"Krotki opis zmian [Wersja {ans}]: ").strip() or f"Wersja {ans}"
    except EOFError:
        msg = f"Wersja {ans}"

    # 4) changelog
    edit_changelog(ans)

    # 5) potwierdzenie
    print("\n" + "-" * 62)
    print(f"Wersja : {ans}   (obecnie: {cur})")
    print(f"Commit : {msg}")
    if NOTES.exists():
        preview = [l for l in NOTES.read_text(encoding="utf-8").splitlines() if l.strip()]
        print("Release:")
        for l in preview[:5]:
            print("   " + l)
        if len(preview) > 5:
            print(f"   … (+{len(preview) - 5} linii)")
    print("-" * 62)
    if "-k" not in sys.argv:
        try:
            ok = input("\nWypuscic wersje? [T/n] (Enter = TAK): ").strip().lower()
        except EOFError:
            ok = "t"
        if ok in ("n", "nie", "no"):
            print("Anulowano - nic nie wyslano.")
            sys.exit(0)

    # 6) wykonanie
    print("\nUstawiam wersje w app/build.gradle.kts…")
    set_current_version(ans)
    print("Zapisuje pliki (git add + commit)…")
    git("add", "-A")
    staged = git("diff", "--cached", "--name-only")
    if staged:
        git("commit", "-m", msg)
    print("Wysylam zmiany na GitHub (push)…")
    git("push")
    print(f"Taguje {ans} i wysylam tag - Actions zbuduja APK i wystawia Release…")
    git("tag", ans)
    git("push", "origin", ans)

    print("\nOK - WYPUSZCZONO WERSJE " + ans)
    print(f"  Release z APK : {GITHUB_URL}/releases")
    print("  Aplikacja sama wykryje nowa wersje przy nastepnym uruchomieniu.")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nPrzerwano - nic nie wyslano.")
