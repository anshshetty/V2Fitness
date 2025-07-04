# Gym Attendance App with QR Code System

A modern Android application for gym attendance management using customizable QR codes. Members generate time-limited QR codes, and staff scan them to log attendance using mobile numbers as unique identifiers.

## 🚀 Features

### Core Functionality
- **QR Code Generation**: Create secure, time-limited QR codes with AES-256 encryption
- **QR Code Scanning**: ML Kit-powered barcode scanning with real-time validation
- **Attendance Tracking**: Comprehensive attendance logging with Firebase Firestore
- **Mobile-First Design**: Mobile number as primary unique identifier
- **Security**: End-to-end encryption with tamper detection

### User Experience
- **Material Design 3**: Modern, beautiful UI with dynamic theming
- **Dark Mode Support**: Automatic theme switching
- **Offline Support**: Generate QR codes offline, sync when online
- **Real-time Updates**: Live data synchronization across devices
- **Accessibility**: Full accessibility compliance with screen readers

### Advanced Features
- **Rate Limiting**: Maximum 5 QR codes per hour per mobile number
- **Usage Analytics**: Track attendance patterns and statistics
- **QR Code Management**: Extend, disable, and share QR codes
- **Daily Usage Tracking**: Monitor daily scan counts and patterns

## 🏗️ Architecture

### Tech Stack
- **Language**: Kotlin
- **Architecture**: MVVM with Repository pattern
- **UI Framework**: Jetpack Compose
- **Database**: Firebase Firestore
- **QR Processing**: ML Kit Barcode Scanning API + ZXing
- **Security**: AES-256-GCM encryption
- **Dependencies**: Hilt for DI, Coroutines, Navigation Component

### Project Structure
```
app/
├── data/
│   ├── models/          # Data classes (QRCodeModel, AttendanceRecord, etc.)
│   └── repository/      # Repository implementations
├── domain/
│   ├── repository/      # Repository interfaces
│   └── usecases/        # Business logic (GenerateQRCodeUseCase, etc.)
├── presentation/
│   ├── ui/
│   │   ├── screens/     # Compose screens
│   │   └── components/  # Reusable UI components
│   ├── viewmodels/      # ViewModels
│   └── navigation/      # Navigation setup
├── utils/               # Utility classes (CryptoUtils, QRCodeGenerator)
└── di/                  # Dependency injection modules
```

## 🛠️ Setup Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK API 35
- Firebase project with Firestore enabled

### 1. Clone the Repository
```bash
git clone <repository-url>
cd V2Fitness
```

### 2. Firebase Setup
1. Create a new Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Enable Firestore Database
3. Download `google-services.json` and place it in `app/` directory
4. Configure Firestore security rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // QR Codes collection
    match /qr_codes/{qrId} {
      allow read, write: if request.auth != null;
    }
    
    // Attendance collection
    match /attendance/{attendanceId} {
      allow read, write: if request.auth != null;
    }
    
    // Daily usage collection
    match /daily_usage/{usageId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### 3. Build and Run
```bash
./gradlew assembleDebug
```

Or open in Android Studio and run the project.

## 📱 App Screens

### 1. Dashboard Screen
- Two primary action buttons (Generate QR, Scan QR)
- Quick stats display (Active QRs, Today's scans)
- Recent QR codes list with status indicators

### 2. Generate QR Screen
- Form inputs: Name, Mobile Number, Expiry Duration
- Quick duration selection (1 Hour, 4 Hours, 1 Day)
- Real-time QR code generation and display
- Save and share functionality

### 3. QR Scanner Screen
- Camera viewfinder with overlay guide
- Real-time scanning feedback
- Flashlight toggle
- Manual input option for damaged QR codes

### 4. QR Management Screen
- List of all user's QR codes
- Status badges (Active, Used, Expired, Disabled)
- Actions: Extend, Disable, Share

## 🔒 Security Features

### Encryption
- **AES-256-GCM**: Industry-standard encryption for QR payloads
- **Unique Salt**: Each QR code uses a unique salt for encryption
- **HMAC**: Tamper detection using HMAC-SHA256

### QR Payload Structure
```json
{
  "name": "John Doe",
  "mobileNumber": "1234567890",
  "timestamp": "2024-01-15T10:30:00.000Z",
  "expiryDuration": 60,
  "qrId": "uuid-string",
  "version": "1.0"
}
```

### Validation Rules
- Name: Minimum 2 characters
- Mobile Number: Exactly 10 digits
- Expiry Duration: 1 minute to 7 days
- Rate Limiting: Maximum 5 active QR codes per mobile number

## 🗄️ Database Schema

### Firestore Collections

#### qr_codes/
```javascript
{
  qrId: string,
  name: string,
  mobileNumber: string,
  createdAt: timestamp,
  expiryDuration: number, // minutes
  isDisabled: boolean,
  usageCount: number,
  lastUsedAt: timestamp,
  encryptedPayload: string,
  salt: string
}
```

#### attendance/
```javascript
{
  id: string,
  mobileNumber: string,
  name: string,
  scanTime: timestamp,
  qrId: string,
  deviceId: string,
  location: string,
  scannerInfo: string
}
```

#### daily_usage/
```javascript
{
  id: string, // format: "YYYY-MM-DD-mobileNumber"
  date: string, // YYYY-MM-DD
  mobileNumber: string,
  usedQRIds: array,
  scanCount: number
}
```

## 🧪 Testing

### Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Integration Tests
```bash
./gradlew connectedDebugAndroidTest
```

### Test Coverage
- QR payload encryption/decryption
- Expiry validation logic
- Business rules validation
- Repository implementations
- UI component testing

## 🚀 Deployment

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### Security Checklist
- [ ] Obfuscate encryption keys
- [ ] Enable ProGuard/R8
- [ ] Remove debug logs
- [ ] Validate Firebase security rules
- [ ] Test on various devices and Android versions

## 📊 Performance Considerations

### Optimizations
- QR code generation performance optimization
- Camera preview optimization
- Firestore query optimization with proper indexing
- Image caching for QR codes
- Background task management with WorkManager

### Memory Management
- Proper camera resource cleanup
- Efficient bitmap handling for QR codes
- Coroutine scope management
- Lifecycle-aware components

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

For support and questions:
- Create an issue in the repository
- Check the [Wiki](wiki-url) for detailed documentation
- Review the [FAQ](faq-url) for common questions

## 🔄 Version History

### v1.0.0 (Current)
- Initial release with core QR generation and scanning
- Firebase Firestore integration
- Material Design 3 UI
- AES-256 encryption
- Basic attendance tracking

### Planned Features
- [ ] Advanced analytics dashboard
- [ ] Bulk QR code generation
- [ ] Export attendance reports
- [ ] Push notifications
- [ ] Multi-gym support
- [ ] Admin panel
- [ ] API for third-party integrations

---

**Built with ❤️ using modern Android development practices**

