# 14 — Play Store, Android 1.0

Priprema za prvu objavu na Google Playu.

## Status
- [x] `applicationId` = `hr.infobot.mojtijek` (ne mijenja se)
- [x] `versionName` 1.0.0, `versionCode` = CI run broj
- [x] `allowBackup = false`
- [x] Health permissions declaration (tekst u `PrivatnostZdravljeActivity`)
- [x] Podsjetnici, GUI paritet, sigurnosna kopija
- [ ] `exportSchema = true` (Room, obavezno prije prve objave)
- [ ] Release keystore + CI `bundleRelease`
- [ ] Ikona (privremena tirkizna M; doraditi s dizajnerom)
- [ ] Play Console: Store listing popunjen
- [ ] Play Console: Data Safety form
- [ ] Play Console: Health apps declaration
- [ ] Play Console: Content rating
- [ ] Privacy policy URL ažuriran za Android

---

## Što radim ja (automatski)

### exportSchema
Room shema mora biti exportana prije prve objave.
U `app/build.gradle.kts` dodati:
```
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
```
Schemas commit u git; ako shema promijeni verziju, CI puca i prisiljava na Migration.

### Release keystore + bundleRelease
- Generiram upload keystore (RSA 4096, 25 god., alias `mojtijek-upload`)
- Čuvam u GitHub secrets: `UPLOAD_KEYSTORE_B64`, `UPLOAD_KEY_ALIAS`,
  `UPLOAD_KEY_PASSWORD`, `UPLOAD_STORE_PASSWORD`
- CI gradi `bundleRelease` (AAB), minificiran s R8
- Otisak javnog ključa upisujem ovdje za evidenciju
- **Nije** isti ključ kao dev debug (`mojtijek-dev`)
- **Nije** Play App Signing ključ — Play taj sam čuva

---

## Što trebaš ti u Play Consoleu

### 1. Enroll u Play App Signing (obavezno, jednokratno)
Play Console → App integrity → App signing → „Enroll".
Play čuva release ključ, tebi ostaje samo upload key (koji mi generiramo).
Bez toga ne možeš uploadati AAB.

### 2. Upload Release AAB → Internal Testing
Play Console → Testing → Internal Testing → Releases → „Create new release"
→ „Upload" → odaberi `MojTijek-Android-1.0.0-b<N>.aab` koji ti dajem.
Release name: `1.0.0 (b<N>)`

### 3. Health apps declaration
Play Console → Policy → App content → Health apps declaration
→ „Start declaration" → odgovori:
- Does your app collect or use health data? **Yes**
- Health data types: Steps, Heart rate, Blood pressure, Sleep, Oxygen saturation, Blood glucose, Body weight
- Used for: Health and fitness tracking (personal use)
- Shared with third parties? **No**
- Uploaded to server? **No**

### 4. Data Safety form
Play Console → Policy → App content → Data safety
Popuni prema tablici:

| Kategorija | Prikuplja se | Dijeli se | Opcija |
|---|---|---|---|
| Health and fitness | Da (Health Connect) | Ne | Encrypted in transit, User can delete |
| Personal info (Ime člana) | Da (lokalno) | Ne | Not transmitted |

Lokalno pohranjenost podataka: **User's device only** (bez servera).

### 5. Content rating
Play Console → Policy → App content → App content rating
→ „Start questionnaire" → Category: **Health & Fitness**
→ Violence: No, Sexual content: No, Profanity: No
→ Na kraju dodijeli oznaku (vjerojatno **Everyone** ili **3+**)

### 6. Store listing
Play Console → Store presence → Main store listing

**Short description** (max 80 znakova):
```
Obiteljski medicinski portal: terapije, nalazi i podsjetnici.
```

**Full description** (max 4000 znakova):
```
MojTijek je privatni obiteljski medicinski portal koji sve zdravstvene
podatke čuva samo na tvom uređaju — ništa ne odlazi na server.

TERAPIJE
• Prati lijekove cijele obitelji s vremenima uzimanja i zalihem
• Podsjetnici za doze, istek recepta i narudžbu
• Tjedne terapije (Ozempic, injekcije) s točnim datumima

NALAZI I KARTOTEKA
• Spremi PDF nalaze i slike izravno u aplikaciju
• Lab trendovi — prati vrijednosti kroz vrijeme
• Uvezi nalaze s Portala zdravlja (e-Građani)

PRAĆENJE
• Poveži sa Google Health Connect: koraci, puls, tlak, šećer, SpO₂
• Ručni unos mjerenja s grafovima po danu, tjednu ili godini
• Cijepljenja i dnevnik simptoma, navika i bilješki

KALENDAR
• Budući pregledi i termini s podsjetnikom
• Uputnice vezane uz termin

PRIVATNOST
• Podaci ostaju na uređaju (Android Room baza, bez clouda)
• Sigurnosna kopija izmjenjiva s MojTijek iOS verzijom
• Nema računa, nema registracije, nema oglasa

Besplatno za jednog člana. MojTijek je informacijski i organizacijski
alat — ne zamjenjuje liječnika ni medicinsku dijagnozu. U hitnoj: 112.
```

**Screenshots** (minimum 2, preporučeno 4-8):
- Ekran Danas s dozama
- Ekran Terapije s karticom lijeka
- Ekran Nalazi
- Ekran Praćenje
→ Snimi sa stvarnog telefona nakon instalacije b<N>.apk

**Feature graphic** (1024×500 px):
→ Zamolit ću @picasso da napravi

**App icon** (512×512 px):
→ Zamolit ću @picasso da napravi (za Play listing)

### 7. Privacy policy
Ažurirati `https://infobot.hr/mojtijek/privatnost/` da uključuje:
- Health Connect koristi se samo za čitanje (ne piše)
- Podaci se ne dijele s trećim stranama
- Podaci se čuvaju samo na uređaju

---

## Ključevi

| Namjena | Alias | Gdje živi |
|---|---|---|
| Debug/testni APK | `mojtijek-dev` | GitHub secret `DEV_KEYSTORE_B64` |
| Upload za Play | `mojtijek-upload` | GitHub secret `UPLOAD_KEYSTORE_B64` |

SHA-256 otisak upload ključa: `EB:AC:AD:57:1D:36:09:33:38:1B:35:C8:F4:7B:CF:57:E7:3B:6C:B8:4F:19:8E:6C:E4:14:F4:E3:BE:9C:CD:44`
| App Signing (release) | — | Google Play (ne izlazi van) |

---

## Verzioniranje

| Polje | Vrijednost |
|---|---|
| `versionCode` | CI run broj (automatski raste) |
| `versionName` | `1.0.0` (mijenja se ručno pri objavi) |
| Naziv APK-a | `MojTijek-Android-1.0.0-b<N>.apk` |
| Naziv AAB-a | `MojTijek-Android-1.0.0-b<N>.aab` |
