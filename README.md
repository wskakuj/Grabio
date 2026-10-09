# Grabio

Natywna aplikacja na Androida (Kotlin + Jetpack Compose) — lista rzeczy do spakowania
na trening danego dnia, z planem tygodnia, przypomnieniami i auto-aktualizacją z GitHuba.

## Funkcje

- **Dziś** — przed pokazaniem listy pyta, czy jedziesz **samochodem czy rowerem**.
  Przy rowerze dopisuje kluczyk od łańcucha i światełka. Lista jest pogrupowana:
  *Trening* (rzeczy na dany dzień), *Codziennie*, *Rower*, *Własne*.
- **Plan** — plan tygodnia (pon–nd) oraz dodatki *codzienne* (kłódka do szafki,
  bateria słuchawek, inhalator, miętówki/gumy) i *rowerowe*.
- **Historia** — zapis minionych dni, procent spakowania, najczęściej zapominane rzeczy.
- **Ustawienia** — przypomnienia: **pon–pt o 16:20**, **sb–nd o 17:00**, przycisk
  „Sprawdź aktualizacje”.
- **Auto-aktualizacja** — przy starcie sprawdza najnowszy Release na GitHubie;
  gdy jest nowszy, pobiera APK i otwiera systemowy instalator (jak w `forestly-go`).

Domyślny plan tygodnia:

| Dzień | Rzeczy |
|-------|--------|
| Poniedziałek | Buty do siadów, buty na zmianę, neopreny, pas, paski do allahów |
| Wtorek | Pomarańczowe paski, paski na nadgarstek |
| Środa | Pas do podciągania, paski 8, paski pomarańczowe |
| Czwartek | Buty do siadów, buty na zmianę, neopreny, paski pomarańczowe |
| Piątek | Paski 8, paski na nadgarstek |
| Sobota | Pas do podciągania |
| Niedziela | Dzień wolny |

Wszystko można edytować w zakładce **Plan**.

## Instalacja

Skopiuj plik `Grabio.apk` na telefon i otwórz go. Android poprosi o zgodę na instalację
z nieznanych źródeł — zaakceptuj. Wymaga Androida 8.0+.

## Auto-aktualizacja — jak to działa

1. `release.py` (albo `release.bat` dwuklikiem) podbija wersję, commituje, wysyła zmiany
   i wypuszcza tag `vX.Y.Z`.
2. GitHub Actions (`.github/workflows/release.yml`) na ten tag buduje **podpisany** APK
   i dołącza go do Release.
3. Aplikacja przy starcie pobiera `…/releases/latest`, porównuje wersje i proponuje
   aktualizację.

Ważne: auto-aktualizacja działa tylko dla **podpisanych buildów release** (z Release na
GitHubie). APK zbudowany lokalnie kluczem *debug* ma inny podpis i nie zainstaluje się
jako aktualizacja.

## Konfiguracja GitHub (raz)

### Najszybciej — jeden dwuklik: `zrob_repo.bat`

Uruchom **`zrob_repo.bat`** w folderze projektu. Skrypt zaloguje Cię do GitHuba (jeśli
trzeba), utworzy repo `wskakuj/Grabio`, wygeneruje klucz podpisu, ustawi trzy sekrety
w repo, wypchnie projekt i wypuści pierwszą wersję — APK pojawi się w zakładce
**Releases**. Wymaga zainstalowanego Git, GitHub CLI (`gh`) i Pythona 3.

Sam `setup.bat` robi to samo bez wypuszczania wersji (repo + klucz + sekrety + push);
przydaje się, gdy chcesz osobno uruchomić potem `release.bat`.

### Ręcznie — krok po kroku

#### 1. Utwórz repozytorium

Utwórz na GitHubie repo **`wskakuj/Grabio`** (publiczne). Możesz też pozwolić
`release.py` utworzyć je automatycznie, jeśli masz zainstalowane [GitHub CLI](https://cli.github.com/)
(`gh auth login`).

### 2. Klucz podpisu (keystore)

Wygeneruj raz klucz (zapamiętaj hasła!):

```bash
keytool -genkeypair -v -keystore release.keystore -alias android \
  -keyalg RSA -keysize 2048 -validity 10000
```

Zakoduj keystore do base64:

```bash
# Linux/macOS
base64 -w0 release.keystore > keystore.b64
# Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | Set-Content keystore.b64
```

### 3. Sekrety w repo (Settings → Secrets and variables → Actions)

| Sekret | Wartość |
|--------|---------|
| `ANDROID_KEYSTORE_B64` | zawartość `keystore.b64` |
| `ANDROID_KEYSTORE_PASS` | hasło keystore |
| `ANDROID_KEYSTORE_ALIAS` | `android` |

### 4. Pierwsze wydanie

```bash
git init
git add -A
git commit -m "Grabio 1.0.0"
git branch -M main
git remote add origin https://github.com/wskakuj/Grabio.git
git push -u origin main
python release.py        # albo dwuklik release.bat
```

Po chwili APK pojawi się w zakładce **Releases**. Zainstaluj go na telefonie — od tej
pory aplikacja sama zaproponuje kolejne aktualizacje.

## Budowanie lokalnie

Wymaga JDK 17 i Android SDK (`sdk.dir` w `local.properties`):

```bash
./gradlew assembleDebug      # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease    # APK release (podpisany debug, jeśli brak keystore)
```

W Android Studio: *Open* → wskaż folder projektu → zielony przycisk *Run*.

## Struktura projektu

```
app/src/main/java/com/wskakuj/grabio/
  MainActivity.kt          – punkt wejścia, dolna nawigacja, okno aktualizacji
  AppViewModel.kt          – logika i stan aplikacji
  data/Models.kt           – model danych + domyślny plan tygodnia
  data/Store.kt            – zapis danych (JSON)
  notify/                  – powiadomienia i przypomnienia (WorkManager)
  update/UpdateManager.kt  – auto-aktualizacja z GitHub Releases
  ui/                      – ekrany: Dziś, Plan, Historia, Ustawienia
.github/workflows/release.yml  – budowa i publikacja APK
release.py / release.bat       – wydawanie nowej wersji
```

## Licencja

MIT.
