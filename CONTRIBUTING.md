# Contributing to Offline Ledger

First off, thank you for considering contributing to Offline Ledger! It's people like you that make this app better for everyone.

## Code of Conduct

This project and everyone participating in it is governed by our commitment to creating a welcoming environment. Please be respectful and constructive in all interactions.

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check existing issues to avoid duplicates.

**When reporting a bug, include:**

- Android version
- Device model
- Steps to reproduce
- Expected behavior
- Actual behavior
- Screenshots (if applicable)

### Suggesting Features

Feature suggestions are welcome! Please provide:

- Clear description of the feature
- Use case / why it's needed
- Any mockups or examples

### Pull Requests

1. **Fork** the repository
2. **Clone** your fork locally
3. **Create a branch** for your feature/fix:
   ```bash
   git checkout -b feature/your-feature-name
   ```
4. **Make your changes** following our coding standards
5. **Test** your changes thoroughly
6. **Commit** with clear messages:
   ```bash
   git commit -m "Add: description of feature"
   ```
7. **Push** to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```
8. **Open a Pull Request** against the `main` branch

## Development Setup

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17
- Android SDK 35

### Building

```bash
# Clone the repo
git clone https://github.com/yourusername/offline-ledger.git
cd offline-ledger

# Build debug APK
./gradlew assembleDebug

# Run tests
./gradlew testDebugUnitTest
```

## Coding Standards

### Kotlin Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Keep functions small and focused
- Add KDoc comments for public APIs

### Architecture

- Follow Clean Architecture principles
- Keep UI logic in ViewModels
- Business logic goes in UseCases
- Data access through Repositories

### Compose Guidelines

- Use `remember` and `derivedStateOf` appropriately
- Extract reusable composables
- Follow Material 3 guidelines
- Support both light and dark themes

### Testing

- Write unit tests for UseCases
- Write tests for ViewModels
- Use property-based testing where applicable
- Aim for meaningful test coverage

## Commit Messages

Use clear, descriptive commit messages:

- `Add:` for new features
- `Fix:` for bug fixes
- `Update:` for changes to existing features
- `Remove:` for removed features
- `Refactor:` for code refactoring
- `Docs:` for documentation changes
- `Test:` for test additions/changes

## Questions?

Feel free to open an issue for any questions about contributing.

Thank you for your contribution! 🎉
