# Android MojTijek - Feature Implementation Status

## Overview
This document tracks the feature implementation status of the Android MojTijek app relative to the iOS production app.

**Last Updated:** September 28, 2026  
**iOS Reference:** `https://github.com/bignjato/Doktor-MojTijek` branch `ios-app` - **NOT ACCESSIBLE**  
**Status:** iOS repository returns 404 Not Found from GitHub API despite being referenced in CI workflows

## Implemented Features ✅

### 1. Core Navigation & Structure
- ✅ Bottom navigation with 5 main tabs
- ✅ Material 3 / Jetpack Compose UI
- ✅ Room database for local-first data storage
- ✅ Koin dependency injection
- ✅ Kotlin coroutines with Flow for reactive data

### 2. Family Management (Obitelj Tab)
- ✅ Add family members with basic info (name, birth date, gender, blood type, height)
- ✅ Select active family member (filters other screens)
- ✅ **NEW:** Detailed family member screen with tabs:
  - ✅ Basic information (demographics, health data)
  - ✅ Medical documents (Dokumenti)
  - ✅ Vaccination records (Cijepljenja)
  - ✅ Health diary/journal (Dnevnik)
- ✅ Support for gynecological data (contraception, menstruation cycle, pregnancy)
- ✅ Emergency contact information
- ✅ Primary doctor information

### 3. Medication Management (Terapije Tab)
- ✅ Add/edit therapies with:
  - Name, strength, form (tablet, syrup, etc.)
  - Dosing schedule (times, days of week)
  - Stock tracking (quantity, doses per box)
  - Notes and reason for therapy
- ✅ Active/inactive therapy toggle
- ✅ Automatic stock level calculation (days remaining)
- ✅ Warning when stock is low (<3 days)

### 4. Daily Dose Management (Moj Dan Tab)
- ✅ Shows all scheduled doses for today
- ✅ Grouped by time slot
- ✅ Confirm dose taken / Skip dose
- ✅ Tracks which doses have been taken
- ✅ Counter showing remaining doses for the day
- ✅ Empty state when no doses scheduled

### 5. Appointments & Exams (Pregledi Tab)
- ✅ Schedule medical appointments/exams with:
  - Title, date, time
  - Location
  - Referral tracking (needed/issued status)
  - Appointment status (planned/completed)
- ✅ Sorted chronologically
- ✅ Mark appointments as completed
- ✅ Delete appointments

### 6. Health Tracking (Praćenje Tab)
- ✅ **Two tabs: Measurements & Lab Results**
- ✅ Record measurements:
  - Weight
  - Blood pressure (systolic/diastolic)
  - Pulse
  - Blood sugar
  - Temperature
  - Custom measurements with units
- ✅ View lab results (read-only, linked from documents)
- ✅ Timestamp tracking for all measurements
- ✅ Delete individual measurements

### 7. Documents & Medical Records (Kartoteka)
- ✅ **NEW:** Add medical documents per family member:
  - Document name and type (findings, reports, etc.)
  - Date of document
  - Healthcare institution
  - Doctor name
  - Notes and explanations
  - Link to lab results (LabNalazEntity)
- ✅ View document history
- ✅ Organized by family member in their detail screen

### 8. Vaccination Tracking
- ✅ **NEW:** Record vaccinations per family member:
  - Vaccine name
  - Date received
  - Next dose date (if applicable)
  - Notes
- ✅ View vaccination history
- ✅ Upcoming vaccination reminders (via next dose date)

### 9. Health Diary/Journal
- ✅ **NEW:** Personal health notes per family member:
  - Title and detailed text
  - Mood/disposition tracking (1-4 scale with emoji)
  - Date stamping
  - Free-form notes
- ✅ Delete diary entries
- ✅ Organized chronologically

## Data Model Support (Room Entities)

### Fully Implemented
- `ClanEntity` - Family members ✅
- `TerapijaEntity` - Therapies/medications ✅
- `UzimanjeEntity` - Dose tracking ✅
- `DogadjajEntity` - Appointments/events ✅
- `MjerenjeEntity` - Health measurements ✅
- `DokumentEntity` - Medical documents ✅
- `CijepljenjeEntity` - Vaccinations ✅
- `DnevnikUnosEntity` - Diary entries ✅
- `LabNalazEntity` - Lab results ✅

### Partially Implemented
- `MenstruacijaEntity` - Menstruation tracking ⚠️
  - Entity exists in database
  - UI not yet implemented (can be added to gynecological section)

## Recent Improvements (This PR)

### Build Configuration ✅
- ✅ Added `targetSdk = 35` to silence Android SDK warnings
- ✅ Added `android.suppressUnsupportedCompileSdk=35` to gradle.properties
- ✅ Fixed `@OptIn(ExperimentalCoroutinesApi::class)` warnings in ViewModel

### Edit Functionality ✅
- ✅ **NEW:** Edit family member details (name, OIB, gender, blood type, allergies, chronic conditions, doctor info, emergency contacts)
- ✅ **NEW:** Edit therapy details (name, strength, form, schedule, dosage, stock, notes)
- ✅ Delete confirmation dialogs for therapies (prevents accidental deletion)

### UX Polish ✅
- ✅ Improved therapy card with separate Edit and Delete buttons
- ✅ Confirmation dialogs for destructive actions
- ✅ Comprehensive edit dialogs with all relevant fields

## Missing Features (Possible iOS Parity Gaps)

### iOS Repository Access Issue
**Critical:** The iOS repository at `https://github.com/bignjato/Doktor-MojTijek` (branch `ios-app`) returns 404 Not Found from GitHub API. Without access to the iOS codebase, accurate feature comparison is impossible.

**Attempted access methods:**
- `gh repo clone bignjato/Doktor-MojTijek` → Repository not found
- `git clone https://github.com/bignjato/Doktor-MojTijek.git` → Repository not found  
- `gh api /repos/bignjato/Doktor-MojTijek` → HTTP 404
- `gh repo list bignjato` → Repository not in list

**Implication:** The following feature gap analysis is based on common medical app patterns, not actual iOS code comparison.

### Unknown iOS Features (Cannot Verify Without Access)
The following are reasonable assumptions about features that may exist in a production iOS medical app:

#### Potentially Missing:
1. **Push notifications** - Medication reminders, appointment alerts
2. **PDF/Export functionality** - Export medical data or summaries
3. **Photo/image attachments** - For documents, wounds, rashes, etc.
4. **Medication barcode scanning** - Quick add medications by scanning
5. **Health integrations** - Apple Health, Google Fit sync
6. **Sharing/family accounts** - Cloud sync, multi-device support
7. **AI assistance** - "Kartoteka" AI features mentioned in repo name "Doktor"
8. **Medication reminders** - Active notifications for upcoming doses
9. **Prescription renewal tracking** - Remind when prescriptions expire
10. **Doctor visit preparation** - Checklists, symptom summaries
11. **Analytics/charts** - Graphical trends for measurements, adherence
12. **Multi-language support** - Currently Croatian only, may have English/others
13. **Dark mode** - Uses dynamic theming but may need explicit dark theme work
14. **Backup/restore** - Local or cloud backup of data
15. **Menstruation cycle visualization** - Calendar view, predictions

#### Known Android-Specific Gaps:
- **No settings screen** - No app configuration, preferences, or about screen
- **No onboarding flow** - First-time user guidance
- **No data import/export** - No way to backup or transfer data
- **No search functionality** - Can't search medications, documents, etc.
- ~~**Limited edit capabilities**~~ - ✅ **FIXED:** Can now edit family members and therapies
- **No dose history view** - Can't see past dose adherence or patterns
- **No medication interaction warnings** - No checking for drug interactions
- **No edit for appointments** - Can only add/delete appointments, not edit
- **No edit for measurements** - Can only add/delete measurements, not edit
- **No edit for documents/vaccinations** - Can only add, not edit

## Technical Debt & Improvements Needed

### Code Quality
- ✅ ~~Add `@OptIn(ExperimentalCoroutinesApi::class)` to ViewModel~~ **FIXED**
- ⚠️ Deprecated `Modifier.menuAnchor()` in PracenjeScreen (minor warning, not critical)
- ⚠️ No error handling for database operations
- ⚠️ No loading states for async operations
- ⚠️ No input validation beyond empty checks (allows invalid data entry)

### UX Improvements
- ✅ ~~Add edit dialogs for existing entities~~ **FIXED for family members and therapies**
- ✅ ~~Add confirmation dialogs for delete operations~~ **FIXED for therapies**
- 📝 Add confirmation dialogs for other delete operations (appointments, measurements, etc.)
- 📝 Add undo functionality for delete operations
- 📝 Add search/filter capabilities to long lists
- 📝 Add sorting options (by name, date, etc.)
- 📝 Better empty states with illustrations
- 📝 Add swipe-to-delete gestures

### Architecture Improvements
- 📝 Add proper navigation with Navigation Compose (currently using state flags)
- 📝 Add ViewModel scoping per screen
- 📝 Add proper error handling and user feedback
- 📝 Add data validation layer
- 📝 Consider adding use cases layer between ViewModel and Repository
- 📝 Add unit tests
- 📝 Add UI tests

## Building & Running

```bash
# Build debug APK
./gradlew assembleDebug

# APK location
app/build/outputs/apk/debug/app-debug.apk

# CI/CD
# - Automated builds on push to main
# - APK published as GitHub Release
# - Available at: https://doktor.infobot.hr/mojtijek.apk
```

## Next Steps

To achieve full parity with the iOS app, the following steps are recommended:

1. **CRITICAL: Access iOS repository** - The iOS repo must be made accessible to this account for accurate feature comparison
2. ✅ ~~**Implement edit functionality**~~ - **DONE for family members and therapies**
3. **Complete edit functionality** - Add edit dialogs for appointments, measurements, documents, vaccinations
4. **Add menstruation tracking UI** - Complete the MenstruacijaEntity integration
5. **Implement notifications** - Medication and appointment reminders
6. **Add data export** - PDF generation, CSV export
7. **Improve navigation** - Proper Navigation Compose setup
8. **Add settings screen** - App configuration and about information
9. **Implement search** - Global search across all entities
10. **Add charts and analytics** - Visualize health data trends
11. **Implement photo attachments** - For documents and diary entries

## Croatian Localization (hr)

Currently, all UI strings are hardcoded in Croatian. For proper i18n:
- Move strings to `res/values-hr/strings.xml`
- Add English fallback in `res/values/strings.xml`
- Use `stringResource()` instead of hardcoded strings

## Notes

- This Android app uses a local-first architecture with Room database
- All data is stored locally on device
- No backend API is currently implemented (though Ktor client exists in shared module)
- The "shared" module suggests potential for KMP (Kotlin Multiplatform) in the future
- The app is designed as an informational tool, not medical advice (disclaimer needed in UI)
