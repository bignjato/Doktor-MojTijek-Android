# Paritet s iOS-om (MojTijek 2.0)

Stanje grane `android-1.0` na dan 2026-11-17. „Ekran” = postoji unos/pregled, bez iOS logike iza.

| Područje | iOS 2.0 | Android | Napomena |
|---|---|---|---|
| Obitelj, profil člana | da | ekran | uređivanje je brisalo podatke člana — popravljeno |
| Terapije, raspored, tjedne doze | da | da | raspored prenesen; zaliha se troši pri potvrdi |
| Potvrda doze, adherencija | da | da | jedan zapis po dozi; nema povijesti izmjena (audit) |
| Podsjetnici za doze i termine | da | **ne** | bio je samo kostur koji ništa nije zakazivao |
| Zaliha, „pri kraju”, narudžba lijekova | da | djelomično | nema `zalihaSet`, narudžbe e-mailom, 30/60/90 |
| Kartoteka: dokumenti (PDF, sken, foto) | da | **ne** | postoji samo zapis o dokumentu, bez datoteke |
| AI čitanje/objašnjenje nalaza (privola) | da | **ne** | ai-proxy traži App Attest → treba Play Integrity put |
| Lab vrijednosti i trendovi | da | **ne** | tablica postoji, nema unosa ni prikaza |
| Kalendar pregleda, uputnica uz termin | da | ekran | bez podsjetnika i sinkronizacije s kalendarom uređaja |
| Praćenje (tlak, šećer, težina…), grafovi | da | ekran | bez Health Connecta |
| Cijepljenja, dnevnik | da | ekran | |
| Ciklus / žensko zdravlje | da | **ne** | tablica postoji, nema ekrana |
| ICE iskaznica s QR kodom, sažetak za liječnika (PDF) | da | **ne** | |
| Sigurnosna kopija (JSON shema 3, izmjenjiva s iOS-om) | da | **ne** | gumb je bio lažan — uklonjen |
| Portal zdravlja: nalazi, uputnice, termini, lijekovi, cijepljenja | da | **ne** | parseri se prenose s testovima; prijavu (Certilia) treba isprobati na uređaju |
| Pametna terapija (usporedba, izgled lijeka, putni način) | da | **ne** | |
| Zaključavanje biometrijom | da | **ne** | |
| Onboarding, demo obitelj, „Što je novo” | da | **ne** | |
| Premium (do 2 člana besplatno) | da | **ne** | Play Billing |
| Jezici hr/en/it/de | da | **ne** | svi tekstovi su upisani u kod, na hrvatskom |
| Widget | da | **ne** | |
| Testovi | 87 | 3 | |

## Redoslijed do Android 1.0

1. **Temelji** (ovaj PR): gubitak podataka pri uređivanju, potvrda doze, privatnost, testovi u CI-ju.
2. **Shema v2 + sigurnosna kopija** izmjenjiva s iOS-om (polja koja nedostaju, migracija, round-trip test).
3. **Podsjetnici** za doze i termine.
4. **Kartoteka s datotekama** (PDF/foto/sken) + lab vrijednosti.
5. **AI čitanje nalaza** — ovisi o odluci za ai-proxy (Play Integrity).
6. **Tekstovi u resurse + 4 jezika**, onboarding, zaključavanje.
7. **Portal zdravlja.**
8. Play: `applicationId`, potpisivanje, `versionCode`, release build s R8, politika privatnosti, Data safety.

Nakon 1.0: Pametna terapija, ciklus, ICE/QR, Health Connect, widget, Premium.

## Otvorene odluke (Boris)

- **`applicationId`** — sada `com.mojtijek.doktor` (iz namespacea), naziv „Doktor MojTijek”. iOS je
  `hr.infobot.mojtijek` / „MojTijek”. Nakon prve objave na Playu se NE može promijeniti.
- **Play Console račun** (osobni ili tvrtkin) i uređaj za testiranje.
- **Premium u 1.0** ili besplatno do pariteta.
