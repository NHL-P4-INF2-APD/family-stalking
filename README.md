# Family Stalking App Development

<h1 align="center">
  <a href="https://www.nhlstenden.com/"><img src="nhl.png" alt="NHL Logo" height="150"></a>
  <img src="logo-filled.png" alt="Family Stalking Logo" height="150">
</h1>

## Project Overview

Dit project is ontwikkeld als onderdeel van de opleiding Informatica aan NHL Stenden. Het doel is het realiseren van een mobiele Android-applicatie in teamverband met focus op softwarekwaliteit, agile werkwijze en samenwerking binnen een professionele ontwikkelomgeving.

De app wordt geschreven in **Kotlin** en richt zich op het bouwen van een Android-toepassing genaamd **Family Stalking**, beschikbaar via de volgende repository:  
🔗 [Jira Project](https://student-team-app-development.atlassian.net/jira/software/projects/FS/boards/1)

---

## Technologies & Tools Used

### Development

- Kotlin
- Android SDK
- Android Studio
- Gradle

### Project Management

- Jira (Scrum)
- Git & GitHub

### Quality Assurance

- JUnit 5
- JaCoCo (code coverage)
- Detekt (static code analysis)
- ktlint (code formatting)
- Git Hooks (automated checks)
- GitHub Actions (CI/CD)

---

## Prerequisites

- Java 17 or higher
- Kotlin 1.9+
- Android Studio (latest version recommended)
- Git

---

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/NHL-P4-INF2-APD/family-stalking.git
cd family-stalking
```

### 2. Code Style Guidelines

Our project enforces consistent code style through:

- **ktlint**: Code formatting
- **detekt**: Code quality analysis

The rules include:

- 4-space indentation
- 120 character line length
- Kotlin official code style
- No wildcard imports
- Consistent naming conventions

To manually check code style:

```bash
./gradlew checkCodeStyle
```

To format code manually:

```bash
./gradlew ktlintFormat
```

---

## CI/CD Pipeline

De GitHub Actions pipeline voert automatisch de volgende stappen uit bij elke push of pull request:

- Builden van de applicatie met Gradle
- Uitvoeren van statische codeanalyse met Detekt
- Draaien van unit tests met JUnit 5
- Genereren van code coverage rapport met JaCoCo
- Automatisch genereren van release-artifacten (APK)

---

## Releases

Bij elke release worden de volgende onderdelen meegeleverd:

- Een uitvoerbaar APK-bestand (debug of release build)
- Release notes met wijzigingen en updates

---

## Development Team

- Bram Suurd
- Bryan Potze
- Bram Veninga
- Yunus Karakoç
