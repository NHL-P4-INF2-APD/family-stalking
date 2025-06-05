# Family Stalking - Android Application

<h1 align="center">
  <a href="https://www.nhlstenden.com/"><img src="nhl.png" alt="NHL Logo" height="150"></a>
  <img src="logo-filled.png" alt="Family Stalking Logo" height="150">
</h1>

[![Android CI](https://github.com/NHL-P4-INF2-APD/family-stalking/actions/workflows/android.yml/badge.svg)](https://github.com/NHL-P4-INF2-APD/family-stalking/actions/workflows/android.yml)

## 📱 Project Overview

This project is developed as part of the Computer Science program at NHL Stenden University. The goal is to create a mobile Android application in a team environment with a focus on software quality, agile methodologies, and collaboration within a professional development environment.

The **Family Stalking** app is a real-time location sharing application built in **Kotlin** that enables family members to stay connected by sharing their locations, coordinating activities, and managing family events through an intuitive mobile interface.

🔗 [Jira Project Board](https://student-team-app-development.atlassian.net/jira/software/projects/FS/boards/1)

---

## ✨ Features

### 🗺️ Real-time Location Sharing
- **Live Location Tracking**: Share your real-time location with family members
- **Interactive Map View**: View all family members' locations on an interactive map
- **Location History**: Access historical location data and movement patterns

### 👥 Family Management
- **Family Networks**: Create and manage family groups
- **Friendship Requests**: Send and receive connection requests with other users
- **Member Profiles**: View family member information and status

### 📅 Event Coordination
- **Calendar Integration**: Plan and coordinate family activities
- **Event Management**: Create, edit, and manage family events
- **Attendance Tracking**: Track who's attending family events

### 🔐 Secure Authentication
- **Email/Password Authentication**: Secure user registration and login
- **Session Management**: Persistent login sessions with automatic renewal
- **Password Recovery**: Self-service password reset functionality

---

## 🏗️ Architecture

The application follows **Clean Architecture** principles with clear separation of concerns:

### 📁 Project Structure
```
app/src/main/java/com/familystalking/app/
├── data/                   # Data layer implementation
│   └── repository/         # Repository implementations
├── domain/                 # Business logic layer
│   ├── model/             # Domain models
│   └── repository/        # Repository interfaces
├── presentation/          # UI layer
│   ├── family/           # Family management screens
│   ├── login/            # Authentication screens
│   ├── map/              # Location and map features
│   └── navigation/       # Navigation configuration
├── di/                   # Dependency injection modules
└── ui/                   # UI theme and styling
```

### 🔧 Technical Architecture
- **MVVM Pattern**: Model-View-ViewModel architecture for UI components
- **Repository Pattern**: Abstraction layer for data access
- **Dependency Injection**: Dagger-Hilt for dependency management
- **Reactive Programming**: StateFlow and LiveData for reactive UI updates
- **Clean Architecture**: Clear separation between data, domain, and presentation layers

---

## 🛠️ Technologies & Tools

### Development Stack
- **Language**: Kotlin 1.9+
- **Platform**: Android SDK (API Level 24+)
- **IDE**: Android Studio (latest version recommended)
- **Build System**: Gradle with Kotlin DSL
- **Backend**: Supabase (Authentication & Database)

### UI Framework
- **Jetpack Compose**: Modern declarative UI toolkit
- **Material 3**: Latest Material Design components
- **Navigation Component**: Type-safe navigation

### Architecture Components
- **ViewModel**: UI state management
- **LiveData & StateFlow**: Reactive data streams
- **Coroutines**: Asynchronous programming
- **Hilt**: Dependency injection

### Testing & Quality
- **JUnit 5**: Unit testing framework
- **JaCoCo**: Code coverage analysis
- **Detekt**: Static code analysis
- **ktlint**: Code formatting and style checking

### Project Management
- **Jira**: Agile project management (Scrum)
- **Git & GitHub**: Version control and collaboration
- **GitHub Actions**: Continuous Integration/Continuous Deployment

---

## 📋 Prerequisites

Before setting up the project, ensure you have:

- **Java 17** or higher
- **Kotlin 1.9+**
- **Android Studio** (latest stable version)
- **Git** for version control
- **Android SDK** with minimum API level 24

---

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/NHL-P4-INF2-APD/family-stalking.git
cd family-stalking
```

### 2. Configure Environment

1. Open the project in Android Studio
2. Let Android Studio download and configure dependencies
3. Ensure you have the required Android SDK versions

### 3. Build and Run

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Install debug APK
./gradlew installDebug
```

---

## 📝 Code Quality Standards

Our project enforces strict code quality through automated tools and guidelines:

### Code Style Guidelines
- **4-space indentation** (mandatory)
- **120 character line length** maximum
- **Kotlin official coding conventions** strictly followed
- **No wildcard imports** allowed
- **Descriptive naming conventions** for all identifiers

### Quality Checks
```bash
# Check code style
./gradlew checkCodeStyle

# Format code automatically
./gradlew ktlintFormat

# Run static analysis
./gradlew detekt

# Generate code coverage report
./gradlew coverage
```

### Documentation Requirements
- **KDoc comments** required for all public classes, functions, and properties
- **Clear and concise** documentation describing purpose and usage
- **Parameter documentation** for all public method parameters
- **Return value documentation** for all non-void public methods

---

## 🔄 CI/CD Pipeline

Our GitHub Actions pipeline automatically performs the following on every push and pull request:

1. **🏗️ Build**: Compile the application with Gradle
2. **🔍 Code Analysis**: Run static code analysis with Detekt
3. **🧪 Testing**: Execute unit tests with JUnit 5
4. **📊 Coverage**: Generate code coverage reports with JaCoCo
5. **📦 Artifacts**: Generate release APKs for distribution

### Build Status
- ✅ **Build Passing**: All builds must pass before merging
- 📈 **Code Coverage**: Minimum coverage thresholds enforced
- 🔒 **Security**: Automated dependency vulnerability scanning

---

## 📊 Database Schema

The application uses a relational database with the following key entities:

- **Users**: User accounts and authentication
- **Families**: Family group management
- **Family Members**: Relationships between users and families
- **Locations**: Real-time location data
- **Calendar Events**: Family event planning
- **Event Attendees**: Event participation tracking

For detailed schema information, see [ERD.md](ERD.md).

---

## 📱 Releases

Each release includes:

- **📦 APK Files**: Debug and release builds
- **📋 Release Notes**: Detailed changelog with new features and bug fixes
- **🔍 Testing Reports**: Code coverage and quality metrics
- **📚 Documentation Updates**: Updated API and user documentation

---

## 👥 Development Team

| Developer | Role | Contributions |
|-----------|------|---------------|
| **Bram Suurd** | Lead Developer | Architecture, Backend Integration |
| **Bryan Potze** | Frontend Developer | UI/UX, Compose Implementation |
| **Bram Veninga** | QA Engineer | Testing, Code Quality |
| **Yunus Karakoç** | DevOps Engineer | CI/CD, Build Management |

---

## 📄 License

This project is developed as part of an educational program at NHL Stenden University. All rights reserved to the development team and the institution.

---

## 🤝 Contributing

This is an educational project with a fixed development team. For questions or feedback, please contact the development team through the official communication channels.

---

## 📧 Contact & Support

For project-related inquiries:
- 📋 **Project Board**: [Jira Workspace](https://student-team-app-development.atlassian.net/jira/software/projects/FS/boards/1)
- 🎓 **Institution**: NHL Stenden University of Applied Sciences
- 📧 **Academic Supervisor**: Contact through official university channels
