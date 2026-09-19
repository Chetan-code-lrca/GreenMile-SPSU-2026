# GreenMile

GreenMile is an Android app for tracking everyday sustainability activities and turning them into points, streaks, and leaderboard progress.

The app is built with Kotlin and Jetpack Compose. Firebase Authentication is used for accounts, while Cloud Firestore stores user profiles and activity data.

## Features

- Email/password sign-up and login
- Email verification and password reset
- Student profile with name, roll number, and department
- Daily activity logging for travel, food, electricity, and plastic use
- Activity history and streak tracking
- Student and department leaderboards
- Android step counting with a foreground service
- Admin view for users and activity logs
- Light and dark theme support

## Tech stack

- Kotlin 2.0.0
- Jetpack Compose with Material 3
- Android Gradle Plugin 8.7.3
- Firebase Authentication
- Cloud Firestore
- Android SDK 35

The app uses `minSdk 26`, `targetSdk 35`, and Java/Kotlin JVM target 11. Use **JDK 17** for the Android Studio and Gradle environment.

## Getting started

### 1. Clone the repository

```bash
git clone https://github.com/Chetan-code-lrca/GreenMile-SPSU-2026.git
cd GreenMile-SPSU-2026
```

### 2. Open the project in Android Studio

Open the repository in a recent Android Studio release with the Android SDK Platform 35 installed. The repository includes the Gradle wrapper (`gradlew` and `gradlew.bat`), so Gradle does not need to be installed separately.

The project is configured for Java 11 bytecode and should be run with JDK 17. The wrapper uses Gradle 8.9.

After opening the project, let Android Studio sync the Gradle configuration before building.

### 3. Configure Firebase

The application is configured for the Android package:

```text
com.spsu.greenmile
```

A Firebase client configuration file is present at:

```text
app/google-services.json
```

For your own Firebase project:

1. Create a Firebase project.
2. Add an Android app with package name `com.spsu.greenmile`.
3. Enable **Email/Password** under Firebase Authentication.
4. Create a Cloud Firestore database.
5. Download the generated `google-services.json` and place it at `app/google-services.json`.

Do not commit Firebase service-account credentials or other server-side secrets.

### 4. Build and run

From Android Studio:

```text
Build → Make Project
Run → Run 'app'
```

Or use the Gradle wrapper from the project root:

```bash
./gradlew assembleDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

Use an Android emulator or physical device running API 26 or newer.

## Firestore data

The app uses Cloud Firestore for its main application data. The primary collections are:

```text
users
activities
```

User documents contain profile information and application statistics such as role, points, carbon totals, activity count, streak, and last active date.

Activity documents contain the logged activity, carbon value, points earned, steps, and date.

This repository does not include Firestore security rules. Before using the app with a production Firebase project, configure rules that restrict users to the records and operations they are authorized to access.

## Roles

Accounts created through the app start with the student role. The sign-up flow does not expose an admin option.

Admin access is based on the user's Firestore role, so the Firebase project must be configured carefully before deploying the application beyond local or classroom use.

## Step counter

GreenMile uses Android's `TYPE_STEP_COUNTER` sensor through a foreground service.

On Android 10 and later, the app requires `ACTIVITY_RECOGNITION` permission to access step data. Step totals are stored locally and updated by the service.

Distance and calorie values shown by the application are estimates derived from the recorded steps; they are not GPS measurements.

## Carbon and points

The current activity logger uses fixed values defined by the application.

### Travel

| Mode | Carbon value |
|---|---:|
| Car | 3.0 |
| Bike | 1.5 |
| Bus | 0.8 |
| Cycle | 0.0 |
| Walk | 0.0 |

### Food

| Type | Carbon value |
|---|---:|
| Non-Veg | 3.5 |
| Veg | 1.0 |
| Junk Food | 2.0 |

Electricity contributes `0.5` per hour and plastic use contributes `0.3`.

Points are then assigned from the resulting carbon value:

| Carbon value | Points |
|---|---:|
| `< 2.0` | 50 |
| `< 3.0` | 35 |
| `< 4.0` | 20 |
| `< 5.0` | 10 |
| `≥ 5.0` | 5 |

These values are application scoring rules, not an official carbon-accounting standard.

## Project structure

```text
GreenMile-SPSU-2026/
├── app/
│   ├── build.gradle.kts
│   ├── google-services.json
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/spsu/greenmile/
│           ├── MainActivity.kt
│           ├── LoginScreen.kt
│           ├── HomeScreen.kt
│           ├── LogActivityScreen.kt
│           ├── LeaderboardScreen.kt
│           ├── ProfileScreen.kt
│           ├── ActivityHistoryScreen.kt
│           ├── StepCounterScreen.kt
│           ├── StepCounterService.kt
│           ├── AdminScreen.kt
│           └── utils/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

## Team

- Chetan
- Srikanth
