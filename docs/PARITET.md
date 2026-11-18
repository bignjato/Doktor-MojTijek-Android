# Paritet s iOS-om (MojTijek 2.0)

Stanje grane `android-1.0` na dan 2026-11-17. „Ekran” = postoji unos/pregled, bez iOS logike iza.

| Područje | iOS 2.0 | Android | Napomena |
|---|---|---|---|
| Obitelj, profil člana | da | ekran | uređivanje je brisalo podatke člana — popravljeno |
| Terapije, raspored, tjedne doze | da | da | raspored prenesen; zaliha se troši pri potvrdi |
| Potvrda doze, adherencija | da | da | jedan zapis po dozi; nema povijesti izmjena (audit) |
| Podsjetnici za doze i termine | da | da | doze, recept, pri kraju, cjepivo, pregled; alarmi nisu točni (mogu kasniti nekoliko minuta); nema „Uzeto” u obavijesti ni odgode; nije isprobano na uređaju |
| Zaliha, „pri kraju”, narudžba lijekova | da | djelomično | nema narudžbe e-mailom ni 30/60/90 |
| Kartoteka: dokumenti (PDF, sken, foto) | da | djelomično | PDF i slike iz datoteka/galerije, otvaranje u aplikaciji; nema skeniranja ni slanja |
| AI čitanje/objašnjenje nalaza (privola) | da | **ne** | ai-proxy traži App Attest → treba Play Integrity put |
| Lab vrijednosti i trendovi | da | prikaz | trendovi se prikazuju; vrijednosti stižu samo iz iOS kopije (nema AI čitanja) |
| Kalendar pregleda, uputnica uz termin | da | ekran | bez podsjetnika i sinkronizacije s kalendarom uređaja |
| Praćenje (tlak, šećer, težina…), grafovi | da | da | Health Connect (samo čitanje, 7 pokazatelja, izvor `healthconnect`); nije isprobano na uređaju; nema pozadinskog osvježavanja ni upozorenja na vrijednost |
| Cijepljenja, dnevnik | da | ekran | |
| Ciklus / žensko zdravlje | da | **ne** | tablica postoji, nema ekrana |
| ICE iskaznica s QR kodom, sažetak za liječnika (PDF) | da | **ne** | |
| Sigurnosna kopija (JSON shema 3, izmjenjiva s iOS-om) | da | da | Profil → Sigurnosna kopija; nije isprobano na uređaju ni sa stvarnom iOS kopijom |
| Portal zdravlja: nalazi, uputnice, termini, lijekovi, cijepljenja | da | **ne** | parseri se prenose s testovima; prijavu (Certilia) treba isprobati na uređaju |
| Pametna terapija (usporedba, izgled lijeka, putni način) | da | **ne** | |
| Zaključavanje biometrijom | da | **ne** | |
| Onboarding, demo obitelj, „Što je novo” | da | **ne** | |
| Premium (do 2 člana besplatno) | da | **ne** | Play Billing |
| Jezici hr/en/it/de | da | djelomično | usklađeni ekrani su na 4 jezika; Profil, Kalendar i Dnevnik samo hrvatski |
| Widget | da | **ne** | |
| Testovi | 87 | 3 | |

## Izgled (mora biti jednak iOS-u)

Boje, tema (svijetla/tamna prema sustavu), oblik kartica i traka tabova preneseni su s iOS-a
(`ui/theme`, `ui/components/Mt.kt`). Ekrani se usklađuju jedan po jedan prema iOS pogledu:

| Ekran | Stanje |
|---|---|
| Danas (Moj dan) | usklađen; nedostaje: Odgodi, kratki opis lijeka, Uputnica/bolnica, ekran narudžbe, ICE, nalazi koji čekaju potvrdu |
| Terapije | usklađen; nedostaje: Pametna terapija, skeniranje kutije, AI opis lijeka |
| Nalazi / Kartoteka | usklađen; nedostaje: Skeniraj, Portal zdravlja, slanje, AI čitanje |
| Praćenje | usklađen; nedostaje: Žensko zdravlje, povlačenje za osvježavanje |
| Profil | usklađen; izbor teme i jezika; prikazuju se samo redci iza kojih postoji ekran; forma člana i Obitelj (uređivanje, redoslijed, brisanje) kao iOS; Cijepljenja zaseban ekran |
| Kalendar, Dnevnik | usklađeni; Kalendar nema zahtjev za uputnicu ni upis u Kalendar telefona |

Usporedba je zasad po kodu; snimke ekrana u CI-ju još ne postoje.

## Redoslijed do Android 1.0

1. **Temelji** (ovaj PR): gubitak podataka pri uređivanju, potvrda doze, privatnost, testovi u CI-ju.
2. ~~Sigurnosna kopija izmjenjiva s iOS-om~~ — gotovo (baza dopunjena poljima i tablicama koje nosi iOS kopija).
3. **Podsjetnici** za doze i termine.
4. **Kartoteka s datotekama** (PDF/foto/sken) + lab vrijednosti.
5. **AI čitanje nalaza** — ovisi o odluci za ai-proxy (Play Integrity).
6. **Tekstovi u resurse + 4 jezika**, onboarding, zaključavanje.
7. **Portal zdravlja.**
8. Play: izvoz Room sheme (`exportSchema`) prije prve objave, potpisivanje, `versionCode`, release build s R8, politika privatnosti, Data safety.

Nakon 1.0: Pametna terapija, ciklus, ICE/QR, widget, Premium.

Play: dozvole Health Connecta traže izjavu o zdravstvenim podacima u Play Consoleu (Health apps declaration) prije objave.

## Otvorene odluke (Boris)

- **Play Console račun** (osobni ili tvrtkin) i uređaj za testiranje.
- **Premium u 1.0** ili besplatno do pariteta.

## Odlučeno

- `applicationId` = `hr.infobot.mojtijek`, naziv „MojTijek” (kao iOS). Paket koda ostaje `com.mojtijek.doktor`.
