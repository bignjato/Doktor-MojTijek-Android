# MojTijek Android — pravila rada

Android inačica MojTijeka (obiteljski medicinski portal, local-first). **iOS je specifikacija**:
`bignjato/Doktor-MojTijek`, grana `ios-app`, mapa `MojTijek_V2/`. Prije nove značajke pročitaj
kako je riješena ondje (model, rubni slučajevi, testovi) i prenesi ponašanje, ne izgled koda.
Stanje pariteta i redoslijed rada: `docs/PARITET.md`.

## Grane
- `android-1.0` — integracijska grana za prvu javnu verziju. Rad ide feature granom + PR u nju.
- `main` — svaki push gradi i objavljuje APK (release). U `main` se ne mergea bez Borisovog „da”.

## Pravila
- **Zdravstveni podaci ostaju na uređaju.** Nikad u logcat (`Log.*`, Ktor `LogLevel.BODY`),
  nikad u Google auto-backup (`allowBackup=false`), nikad trećoj strani bez izričite privole.
- **Baza:** do prve javne objave shema je v1 i mijenja se slobodno (pod novim `applicationId`-jem
  nema instalacija). Od objave: svaka promjena entiteta = nova verzija + `Migration` + test. Nema
  `fallbackToDestructiveMigration`. DAO upis je `@Upsert`, NIKAD `@Insert(REPLACE)` — REPLACE
  briše redak i `ON DELETE CASCADE` odnese svu djecu (uređivanje člana brisalo je sve njegovo).
- **Sigurnosna kopija** (`data/Kopija.kt`) dijeli format s iOS-om (`ExportService`/`ImportService`).
  Novo polje u entitetu = dodaj ga u izvoz, uvoz i u `KopijaTest`; ključevi se mijenjaju samo zajedno s iOS-om.
- **Nema lažnih značajki.** Gumb koji ništa ne radi ili „would schedule…” ne ide u granu.
- **Tekstovi** idu u `strings.xml` za hr (izvor) + en/it/de — isti ključni tekstovi kao iOS katalog.
- **Gotovo = CI zelen** (`./gradlew testDebugUnitTest assembleDebug`) + novi test za novu logiku.
  Na razvojnom hostu nema JDK-a ni Android SDK-a; build i testovi vrte se u GitHub Actions.
- Aplikacija ne postavlja dijagnoze; u hitnom slučaju 112.
