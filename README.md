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

- Razvoj ide na grani `android-1.0` (feature grana + PR); GitHub Actions na svakom PR-u gradi debug APK i vrti testove.
- U Android Studiju: checkout `android-1.0`, *Pull*, konfiguracija **app**, *Run*.
- Verzija APK-a je `1.0.0-b<N>`, gdje je N redni broj CI builda (veći = noviji); vidi se u imenu datoteke i u Profil → Verzija.
- Testni APK-ovi iz CI-ja potpisani su stalnim ključem (secreti `DEV_KEYSTORE_B64`, `DEV_KEYSTORE_PASSWORD`), pa se instaliraju jedan preko drugog. Ključ za Play je zaseban i još ne postoji.
- `main` objavljuje APK kao GitHub Release na svaki push — u `main` se mergea tek kad je verzija za objavu.

## Sinkronizacija s iOS

Workflow `sync-from-ios.yml` se pokreće na push u `ios-app` branch i automatski kreira PR u ovaj repo s ažuriranim shared modulom.

## Varijable i Secreti

- `secrets.GITHUB_TOKEN` — default (automatski dostupan)
- `secrets.GH_TOKEN_FOR_PRS` — GitHub PAT s pristupom oba repoa (za automatski PR)
