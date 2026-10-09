#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Grabio - jednorazowa konfiguracja GitHuba
=========================================
Uruchom RAZ, w folderze repo Grabio, na komputerze, gdzie masz:
  - git,
  - GitHub CLI (gh) zalogowane:  gh auth login.

Klucz podpisu generuje keytool (z JDK/Android Studio) - a jesli go nie ma,
skrypt sprobuje wygenerowac go bez Javy (biblioteka 'cryptography').

Skrypt robi wszystko, co potrzebne do dzialajacej auto-aktualizacji:
  1. sprawdza gh i logowanie,
  2. tworzy repo GitHub (jesli nie istnieje),
  3. generuje klucz podpisu release.keystore (jesli go nie ma),
  4. ustawia 3 sekrety w repo (ANDROID_KEYSTORE_B64 / _PASS / _ALIAS),
  5. zaklada git, robi pierwszy commit i wysyla na GitHub.

Po tym uruchom release.bat (albo zrob_repo.bat robi to od razu).
"""

import base64
import getpass
import glob
import os
import shutil
import subprocess
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent
OWNER = "wskakuj"
NAME = "Grabio"
SLUG = f"{OWNER}/{NAME}"
URL = f"https://github.com/{SLUG}.git"
KEYSTORE = REPO / "release.keystore"
ALIAS = "android"


def run(cmd, check=True, **kw):
    r = subprocess.run(cmd, capture_output=True, text=True,
                       encoding="utf-8", errors="replace", **kw)
    if check and r.returncode != 0:
        print("X blad:", " ".join(str(c) for c in cmd))
        if r.stdout.strip():
            print(r.stdout.strip())
        if r.stderr.strip():
            print(r.stderr.strip())
        sys.exit(1)
    return r


def need(tool):
    if shutil.which(tool) is None:
        print(f"X brak narzedzia '{tool}' w PATH.")
        sys.exit(1)


def gh_auth_ok():
    return subprocess.run(["gh", "auth", "status"],
                          capture_output=True, text=True).returncode == 0


def repo_exists():
    return subprocess.run(["gh", "repo", "view", SLUG],
                          capture_output=True, text=True).returncode == 0


def find_keytool():
    """Szuka keytool: PATH -> JAVA_HOME -> typowe lokalizacje."""
    p = shutil.which("keytool")
    if p:
        return p
    jh = os.environ.get("JAVA_HOME")
    if jh:
        for exe in ("keytool.exe", "keytool"):
            cand = Path(jh) / "bin" / exe
            if cand.exists():
                return str(cand)
    wzorce = [
        r"C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe",
        r"C:\Program Files\Android\Android Studio\jre\bin\keytool.exe",
        r"C:\Program Files\Android\Android Studio\bin\keytool.exe",
        os.path.expandvars(r"%LOCALAPPDATA%\Programs\Android Studio\jbr\bin\keytool.exe"),
        os.path.expandvars(r"%PROGRAMFILES%\Java\jdk*\bin\keytool.exe"),
        os.path.expandvars(r"%PROGRAMFILES%\Eclipse Adoptium\jdk*\bin\keytool.exe"),
        os.path.expandvars(r"%PROGRAMFILES%\Microsoft\jdk*\bin\keytool.exe"),
        os.path.expandvars(r"%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk*\bin\keytool.exe"),
        "/usr/bin/keytool",
        "/usr/local/bin/keytool",
        "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool",
    ]
    for pat in wzorce:
        for hit in glob.glob(pat):
            if os.path.exists(hit):
                return hit
    return None


def generate_keystore(haslo):
    kt = find_keytool()
    if kt:
        print(f"Uzywam keytool: {kt}")
        run([kt, "-genkeypair", "-v",
             "-keystore", str(KEYSTORE), "-alias", ALIAS,
             "-keyalg", "RSA", "-keysize", "2048", "-validity", "10000",
             "-storetype", "PKCS12",
             "-storepass", haslo, "-keypass", haslo,
             "-dname", f"CN={NAME}, O={OWNER}"])
        return

    # Bez Javy: klucz w formacie PKCS12 przez biblioteke 'cryptography'.
    try:
        import datetime

        from cryptography import x509
        from cryptography.hazmat.primitives import hashes, serialization
        from cryptography.hazmat.primitives.asymmetric import rsa
        from cryptography.hazmat.primitives.serialization import pkcs12
        from cryptography.x509.oid import NameOID
    except ImportError:
        print("X Nie znalazlem 'keytool' ani biblioteki 'cryptography'.")
        print("  Wybierz jedno:")
        print("   a) zainstaluj JDK 17 (https://adoptium.net) i uruchom setup ponownie,")
        print("   b) albo zainstaluj biblioteke:  pip install cryptography")
        sys.exit(1)

    print("Brak keytool - generuje klucz bez Javy (cryptography)…")
    klucz = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    nazwa = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, NAME)])
    teraz = datetime.datetime.utcnow()
    cert = (
        x509.CertificateBuilder()
        .subject_name(nazwa)
        .issuer_name(nazwa)
        .public_key(klucz.public_key())
        .serial_number(x509.random_serial_number())
        .not_valid_before(teraz - datetime.timedelta(days=1))
        .not_valid_after(teraz + datetime.timedelta(days=10000))
        .sign(klucz, hashes.SHA256())
    )
    dane = pkcs12.serialize_key_and_certificates(
        name=ALIAS.encode(),
        key=klucz,
        cert=cert,
        cas=None,
        encryption_algorithm=serialization.BestAvailableEncryption(haslo.encode()),
    )
    KEYSTORE.write_bytes(dane)


def main():
    print("=" * 62)
    print("  GRABIO - konfiguracja GitHuba (jednorazowo)")
    print("=" * 62)
    need("git")
    need("gh")

    if not gh_auth_ok():
        print("X GitHub CLI nie jest zalogowane. Uruchom najpierw:")
        print("    gh auth login")
        sys.exit(1)

    # 1) repozytorium
    if repo_exists():
        print(f"Repo {SLUG} juz istnieje - pomijam tworzenie.")
    else:
        print(f"Tworze repo {SLUG}…")
        run(["gh", "repo", "create", SLUG, "--public"])

    # 2) klucz podpisu
    if KEYSTORE.exists():
        print("Klucz release.keystore juz istnieje - uzyje go.")
        haslo = getpass.getpass("Podaj haslo tego keystore: ")
    else:
        haslo = getpass.getpass("Ustaw haslo klucza podpisu (zapamietaj je!): ")
        powtorka = getpass.getpass("Powtorz haslo: ")
        if haslo != powtorka:
            print("X Hasla sie nie zgadzaja.")
            sys.exit(1)
        generate_keystore(haslo)

    # 3) sekrety w repo
    print("Ustawiam sekrety w repo…")
    b64 = base64.b64encode(KEYSTORE.read_bytes()).decode()
    for nazwa, wartosc in (
        ("ANDROID_KEYSTORE_B64", b64),
        ("ANDROID_KEYSTORE_PASS", haslo),
        ("ANDROID_KEYSTORE_ALIAS", ALIAS),
    ):
        run(["gh", "secret", "set", nazwa, "--repo", SLUG], input=wartosc)
        print(f"   ustawiono {nazwa}")

    # 4) git + pierwszy push
    if not (REPO / ".git").exists():
        run(["git", "-C", str(REPO), "init"])
    run(["git", "-C", str(REPO), "add", "-A"])
    if subprocess.run(["git", "-C", str(REPO), "diff", "--cached", "--quiet"]).returncode != 0:
        run(["git", "-C", str(REPO), "commit", "-m", f"{NAME} 1.0.0"])
    run(["git", "-C", str(REPO), "branch", "-M", "main"], check=False)
    remotes = run(["git", "-C", str(REPO), "remote"], check=False).stdout.split()
    if "origin" not in remotes:
        run(["git", "-C", str(REPO), "remote", "add", "origin", URL])
    else:
        run(["git", "-C", str(REPO), "remote", "set-url", "origin", URL])
    print("Wysylam na GitHub…")
    run(["git", "-C", str(REPO), "push", "-u", "origin", "main"])

    print("\nOK - gotowe!")
    print(f"  Repo       : https://github.com/{SLUG}")
    print("  Nastepny krok: uruchom release.bat (wypusci wersje z podpisanym APK).")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nPrzerwano.")
