# 💰 Flowly - Personal Finance & Task Tracker

<p align="center">
  <img src="app/src/main/res/drawable/ic_launcher_foreground.xml" width="120" alt="Flowly Logo"/>
</p>

<p align="center">
  <strong>A privacy-first, offline personal finance, debt tracking & task management app for Android</strong>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#screenshots">Screenshots</a> •
  <a href="#installation">Installation</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#tech-stack">Tech Stack</a> •
  <a href="#contributing">Contributing</a>
</p>

---

## 📱 Overview

**Flowly** is a fully offline Android application designed to help you track personal finances, manage debts with friends and family, monitor bill payments, and organize your tasks. Built with privacy in mind - no internet permissions, no data collection, everything stays on your device.

### Why Flowly?

- 🔒 **100% Offline** - No internet permission, your data never leaves your device
- 🛡️ **Privacy First** - Encrypted local database using SQLCipher
- 📊 **Smart Analytics** - Monthly summaries, category breakdowns, debt tracking
- 🎨 **5 Beautiful Themes** - Neo-Minimal, Glass, Retro-Futurism, Neo-Brutal, Hyper-Bloom
- ⚡ **Fast & Lightweight** - Built with Jetpack Compose for smooth 60fps UI

---

## ✨ Features

### 💸 Transaction Management

- Quick add transactions with numeric keypad
- Track money given (GAVE) and received (RECEIVED)
- Bill payment tracking (Electricity, TV, Mobile, Internet)
- Partial payment support for settling debts
- Transaction history with search and filters

### 👥 People/Counterparty Management

- Track who owes you and who you owe
- Net balance calculation per person
- Favorite contacts for quick access
- Individual ledger view per person

### 📈 Analytics Dashboard

- Monthly income/expense summary
- Category-wise breakdown
- Top 5 debtors and creditors
- Unrecovered amount tracking (12 months)
- Real-time updates when transactions change

### ✅ Task Management

- Create financial tasks with due dates
- Priority levels (High, Medium, Low)
- Checklist items within tasks
- Convert tasks to transactions
- Completion animations

### 🔔 Reminders

- Set reminders for payments
- Recurring bill reminders
- Notification support with snooze

### 💾 Backup & Restore

- Export data to JSON backup
- Restore from backup files
- Uses Android Storage Access Framework (no permissions needed)
- Backup saved to Downloads folder

### 🎨 Theming

- Light/Dark/System theme modes
- 5 unique design skins:
  - **Neo-Minimal** - Clean, modern design
  - **Glassmorphism** - Frosted glass effects
  - **Retro-Futurism** - Vintage meets future
  - **Neo-Brutalism** - Bold, raw aesthetics
  - **Hyper-Bloom** - Vibrant gradients

### 📝 Audit Log

- Complete history of all changes
- Track creates, updates, deletes
- Transparency and accountability

---

## 📸 Screenshots

|         Home Dashboard         |           Quick Add            |             People              |
| :----------------------------: | :----------------------------: | :-----------------------------: |
| Dashboard with balance summary | Numeric keypad for quick entry | Counterparty list with balances |

|       Analytics        |      Tasks      |        Settings         |
| :--------------------: | :-------------: | :---------------------: |
| Monthly analytics view | Task management | Theme & backup settings |

---

## 🚀 Installation

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 35 (compile) / SDK 26 (minimum)

### Build from Source

1. **Clone the repository**

   ```bash
   git clone https://github.com/yourusername/flowly.git
   cd flowly
   ```

2. **Open in Android Studio**

   - File → Open → Select the project folder

3. **Build the project**

   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on device**

   ```bash
   ./gradlew installDebug
   ```

   Or via ADB:

   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### Run Tests

```bash
# Unit tests
./gradlew testDebugUnitTest

# All tests
./gradlew test
```

---

## 🏗️ Architecture

The app follows **Clean Architecture** with **MVVM** pattern:

```
app/
├── data/                    # Data Layer
│   ├── local/
│   │   ├── dao/            # Room DAOs
│   │   ├── db/             # Database configuration
│   │   ├── entity/         # Room entities
│   │   └── backup/         # Backup/Restore logic
│   └── repository/         # Repository implementations
│
├── domain/                  # Domain Layer
│   ├── model/              # Domain models
│   ├── repository/         # Repository interfaces
│   ├── usecase/            # Business logic use cases
│   └── worker/             # Background workers
│
├── presentation/            # Presentation Layer
│   ├── navigation/         # Navigation setup
│   ├── theme/              # Theming & skins
│   ├── ui/                 # Composable screens
│   │   ├── analytics/
│   │   ├── audit/
│   │   ├── bills/
│   │   ├── components/     # Reusable UI components
│   │   ├── counterparty/
│   │   ├── graphics3d/     # 3D balance visualization
│   │   ├── home/
│   │   ├── quickadd/
│   │   ├── reminders/
│   │   ├── settings/
│   │   ├── tasks/
│   │   └── transaction/
│   └── viewmodel/          # ViewModels
│
└── di/                      # Dependency Injection (Hilt)
```

### Data Flow

```
UI (Compose) → ViewModel → UseCase → Repository → DAO → Room Database
```

---

## 🛠️ Tech Stack

| Category                 | Technology                   |
| ------------------------ | ---------------------------- |
| **Language**             | Kotlin 2.0                   |
| **UI Framework**         | Jetpack Compose              |
| **Architecture**         | MVVM + Clean Architecture    |
| **Dependency Injection** | Hilt                         |
| **Database**             | Room + SQLCipher (encrypted) |
| **Async**                | Kotlin Coroutines + Flow     |
| **Navigation**           | Navigation Compose           |
| **Testing**              | JUnit 5, Kotest, Robolectric |
| **Build**                | Gradle 8.9 with Kotlin DSL   |

### Key Dependencies

```kotlin
// Compose BOM
androidx-compose-bom = "2024.09.03"

// Room
room = "2.6.1"

// Hilt
hilt = "2.51.1"

// SQLCipher
sqlcipher = "4.5.4"

// Kotest (Testing)
kotest = "5.9.1"
```

---

## 📁 Project Structure

```
flowly/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/ledger/app/
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   ├── test/           # Unit tests
│   │   └── androidTest/    # Instrumented tests
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   ├── wrapper/
│   └── libs.versions.toml  # Version catalog
├── build.gradle.kts        # Root build file
├── settings.gradle.kts
├── gradle.properties
├── .gitignore
└── README.md
```

---

## 🔐 Security

- **No Internet Permission** - App cannot access the network
- **Encrypted Database** - SQLCipher encryption for local data
- **Secure Keystore** - Database passphrase stored in Android Keystore
- **No Analytics** - Zero tracking or data collection

---

## 🧪 Testing

The project includes comprehensive tests:

- **Property-based tests** using Kotest
- **DAO tests** for database operations
- **UseCase tests** for business logic
- **ViewModel tests** for UI logic

```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Run specific test class
./gradlew test --tests "com.ledger.app.domain.usecase.*"
```

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style

- Follow Kotlin coding conventions
- Use meaningful commit messages
- Add tests for new features
- Update documentation as needed

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern Android UI toolkit
- [SQLCipher](https://www.zetetic.net/sqlcipher/) - Database encryption
- [Hilt](https://dagger.dev/hilt/) - Dependency injection
- [Material Design 3](https://m3.material.io/) - Design system

---

<p align="center">
  Made with ❤️ by Destroyer 
</p>
