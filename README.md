# Doktor-MojTijek-Android

Android aplikacija za Doktor platformu — zaseban repo s čistom Gradle konfiguracijom.

## Tech Stack

- **AGP** 8.5.0
- **Kotlin** 2.0.21
- **Compose BOM** 2024.12.01
- **Gradle** 8.10.2
- **Koin** 3.5.6 (DI)
- **Ktor** 2.3.12 (networking)

## Struktura

```
app/          — Android aplikacija (Compose UI)
shared/       — Android library s poslovnom logikom
```

## Build

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## CI/CD

- GitHub Actions gradi APK na svaki push na `main`
- APK se uploaduje kao GitHub Release artifact
- SHA-256 checksum u release notes
- Link: `https://doktor.infobot.hr/mojtijek.apk` (preusmjerava na APK)

## Sinkronizacija s iOS

Workflow `sync-from-ios.yml` se pokreće na push u `ios-app` branch i automatski kreira PR u ovaj repo s ažuriranim shared modulom.

## Varijable i Secreti

- `secrets.GITHUB_TOKEN` — default (automatski dostupan)
- `secrets.GH_TOKEN_FOR_PRS` — GitHub PAT s pristupom oba repoa (za automatski PR)
