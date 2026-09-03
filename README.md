# QRCheckIn

Android **QR attendance / check-in** for places that have no membership or access-control software.

Staff generate a time-limited QR for a person (name + mobile number), share it on WhatsApp, and scan it at the door to log attendance. Visitors never install the app. No accounts, billing, or POS — just check-in.

Works anywhere a front desk and an entrance exist: gyms, yoga and dance studios, sports clubs, classes, coworking spaces, tuition centres, community halls, small events.

This repo is a **modern Android starter** (Kotlin, Jetpack Compose, MVVM, Hilt, Firebase, CameraX, ML Kit) you can fork, rebrand, and point at your own Firebase project.

---

## Who this is for

- Independent venues still using paper registers, WhatsApp photos, or verbal check-in
- Developers looking for a Compose + Firestore attendance app to extend

Staff phones only. Device approval in Firestore is the access gate: a device must be approved before it can generate or scan codes.

This is **not** school roll-call (periods, classes) or employee time-tracking (payroll, shifts). It is door check-in with a QR pass.

---

## How it works

1. A staff device registers itself (`approved_devices` in Firestore, status `pending`).
2. An admin sets that document’s `deviceStatus` to `approved`.
3. Staff optionally enable biometrics on that device.
4. **Generate QR** — name, 10-digit mobile, expiry in days. Payload is AES-GCM encrypted.
5. Share the QR to the visitor on WhatsApp.
6. **Scan QR** at the entrance — ML Kit validates expiry, disable flag, and daily use, then writes an attendance record.

---

## Stack

| Layer | Choice |
| --- | --- |
| Language / UI | Kotlin, Jetpack Compose, Material 3 |
| Architecture | MVVM, use cases, repository interfaces |
| DI | Hilt |
| Backend | Firebase Firestore |
| Scan / generate | CameraX + ML Kit Barcode, ZXing |
| Security | AES-GCM QR payload, HMAC, biometric lock, device approval |

```
app/src/main/java/v2/
├── data/           models + Firestore repositories
├── domain/         repository contracts + use cases
├── presentation/   Compose screens, ViewModels, navigation
├── di/             Hilt modules
├── theme/          colors, typography, Material 3 theme
└── utils/          crypto, QR bitmap, device id, WhatsApp share
```

---

## Run locally

**Needs:** Android Studio (Hedgehog+), JDK 17, SDK 35, a Firebase project with Firestore.

```bash
git clone https://github.com/anshshetty/QRCheckIn.git
cd QRCheckIn
```

1. The repo includes a dummy `app/google-services.json` so Gradle and CI can compile. It does not talk to a real Firebase project.
2. For a live venue: create a Firebase project → add Android apps for `app.qrcheckin` and `app.qrcheckin.debug` (debug uses an `applicationId` suffix) → replace `app/google-services.json` with the file from the console. Do not commit a production file if it contains live keys you want private.
3. Enable **Cloud Firestore**. This app does **not** sign in with Firebase Auth, so rules must not require `request.auth`. Starter rules (tighten before you go live):

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /qr_codes/{id} { allow read, write: if true; }
    match /attendance/{id} { allow read, write: if true; }
    match /daily_usage/{id} { allow read, write: if true; }
    match /approved_devices/{id} { allow read, write: if true; }
  }
}
```

4. Create indexes if the console prompts you after the first queries.

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Or open the project in Android Studio and Run.

### Approve the first staff phone

After install, the app registers the device and waits.

In Firestore → `approved_devices` → the device document → set `deviceStatus` to `"approved"` (`pending` / `approved` / `rejected`). Open the app again.

---

## Use this at your venue

You do not need to rewrite the attendance flow. Change identity, backend, and copy.

### 1. Firebase (required)

Each venue should have **its own Firebase project**. Replace the dummy `app/google-services.json` with that project's file so visitor QRs and attendance never mix with another site. Include both the release package and the `.debug` variant.

### 2. Application id (required for Play Store / side-by-side install)

In `app/build.gradle.kts`:

- `applicationId` (today: `app.qrcheckin`)
- debug/release `app_name` `resValue`s
- APK `outputFileName` if you care about the file name

Re-download `google-services.json` for the new package name.

Kotlin package `v2` / `namespace = "v2"` can stay. Changing it is optional and touches every source file.

### 3. Branding (required so visitors see your name)

| What | Where |
| --- | --- |
| Launcher / system name | `app/src/main/res/values/strings.xml` → `app_name`, and the `resValue("string", "app_name", ...)` entries in `app/build.gradle.kts` |
| In-app title + WhatsApp org name | `org_display_name` in `strings.xml` |
| WhatsApp QR message body | `QrShareMessage.build()` in `app/src/main/java/v2/utils/QrShareMessage.kt` |
| Launcher icon | `app/src/main/res/mipmap-*` |
| Theme colors | `app/src/main/java/v2/theme/Color.kt` (Material 3 also uses dynamic color on Android 12+) |

### 4. Staff device policy

Treat `approved_devices` as your allowlist. Only venue phones should be `approved`. Reject lost devices by setting `deviceStatus` to `"rejected"`.

### 5. Common extensions (optional)

| Change | Where |
| --- | --- |
| 10-digit (India) mobile validation | `GenerateQRCodeUseCase` and `QRCodeRepositoryImpl` |
| Max 5 active QRs per mobile | `GenerateQRCodeUseCase` |
| Expiry is in **days** (`QRCodeModel.expiryDuration`) | generate-QR UI + model |
| Firestore collection names | `QRCodeRepositoryImpl`, `DeviceApprovalRepositoryImpl` |
| Test seed locations | `DataSeeder` (debug testing menu only) |

### 6. Release signing

`app/build.gradle.kts` currently signs release with the debug keystore. Replace `signingConfigs.release` with your own keystore and env-based passwords before shipping.

Production hardening: R8 is already on for release; add Firebase App Check, lock Firestore rules, and consider staff Firebase Auth if you outgrow device-approval-only access.

---

## App surfaces

- **Dashboard** — generate, scan, today’s check-ins, recent QRs
- **Generate QR** — visitor form, bitmap, save, WhatsApp
- **Scan QR** — camera, torch, validation feedback
- **QR list** — extend, disable, share
- **Device check / waiting** — registration until an admin approves
- **Biometric setup / unlock** — optional lock on an approved device
- **Testing menu** — debug builds only (`ENABLE_TESTING_MENU`)

---

## Data

### QR payload (encrypted in the code)

```json
{
  "name": "Jane Visitor",
  "mobileNumber": "9876543210",
  "timestamp": "2026-01-15T10:30:00.000Z",
  "expiryDuration": 30,
  "qrId": "uuid",
  "version": "1.0"
}
```

`expiryDuration` is **days**, not minutes.

### Collections

**`qr_codes/{qrId}`** — `name`, `mobileNumber`, `createdAt`, `expiryDuration`, `status`, `usageCount`, `lastUsedAt`, `encryptedPayload`, `salt`, `deviceId`

**`attendance/{id}`** — `mobileNumber`, `name`, `scanTime`, `qrId`, `deviceId`, `location`, `scannerInfo`

**`daily_usage/{YYYY-MM-DD-mobileNumber}`** — `date`, `mobileNumber`, `usedQRIds`, `scanCount`

**`approved_devices/{deviceId}`** — `deviceModel`, `deviceManufacturer`, `androidVersion`, `appVersion`, `registrationDate`, `lastActiveDate`, `deviceStatus`

---

## Tests

```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```

---

## License

MIT. See [LICENSE](LICENSE).

Fork it, set `org_display_name` to your venue, point it at your Firebase project.
