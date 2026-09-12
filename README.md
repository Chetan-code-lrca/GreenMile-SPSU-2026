# GreenMile

GreenMile is an Android app for tracking everyday sustainability activities on campus.

The app is built with Kotlin and Jetpack Compose and uses Firebase Authentication and Firestore for accounts, activity logs, points, streaks, leaderboards, and admin data.

## Features

- Email/password sign-up and login with email verification and password reset
- Student profile with name, roll number, and department
- Daily sustainability activity logging for travel, food, electricity, and plastic use
- Activity history and streak tracking
- Student and department leaderboards
- Android step counting with a foreground service
- Admin dashboard for users and activity logs
- Light/dark theme preference

## Tech stack

- Kotlin 2.0.0
- Jetpack Compose / Material 3
- Android SDK
- Firebase Authentication
- Firebase Firestore
- Android Gradle Plugin 8.5.0

The app targets `compileSdk 35`, has `minSdk 26`, and compiles Java/Kotlin sources with JVM target 11. Use JDK 17 for the Android Studio/Gradle environment.

There is no Gradle wrapper in the repository, so Android Studio is the simplest way to build it.

## Getting started

### 1. Clone

```bash
git clone https://github.com/Chetan-code-lrca/GreenMile-SPSU-2026.git
cd GreenMile-SPSU-2026
```

### 2. Install Android Studio and JDK

Install:

- Android Studio
- Android SDK Platform 35
- Android SDK Build Tools
- JDK 17

Open the repository in Android Studio and allow Gradle to sync.

### 3. Firebase

The project expects:

```text
app/google-services.json
```

The repository includes an Android client configuration for the package:

```text
com.spsu.greenmile
```

For an independent deployment, create the Firebase project yourself, add an Android app with the same package name, enable Email/Password authentication and Firestore, and replace `app/google-services.json` with your project's client configuration.

Keep Firebase server credentials and service-account JSON files out of the repository.

### 4. Build and run

In Android Studio:

```text
File → Sync Project with Gradle Files
Build → Make Project
```

Run the `app` configuration on an Android emulator or physical device running API 26 or newer.

## Firestore data

The application uses these main collections:

```text
users
activities
```

User records include profile information, role, points, carbon totals, activity count, streak, and last active date.

Activity records include the logged activity values, carbon value, points earned, steps, and date.

Firestore security rules are not included in this repository, so the Firebase project must be configured with suitable rules before a production deployment.

## Roles

New accounts are created with:

```text
role = student
```

The sign-up screen cannot create an admin account. Admin access is controlled by the user's Firestore role.

## Step counter

GreenMile uses Android's `TYPE_STEP_COUNTER` sensor. Android 10+ requires `ACTIVITY_RECOGNITION` permission for step access.

Daily steps are stored locally and updated through the foreground step-counting service. Distance and calorie values shown by the app are estimates calculated by the application, not GPS measurements.

## Carbon and points

The activity logger uses fixed application values.

```text
Travel
Car      3.0
Bike     1.5
Bus      0.8
Cycle    0.0
Walk     0.0

Food
Non-Veg  3.5
Veg      1.0
Junk Food 2.0
```

Electricity adds `0.5` per hour and plastic use adds `0.3`.

Points are calculated from the resulting carbon value:

```text
< 2.0  → 50
< 3.0  → 35
< 4.0  → 20
< 5.0  → 10
≥ 5.0  → 5
```

These numbers are application scoring rules rather than an official carbon-accounting standard.

## Build note

The project currently uses Android Gradle Plugin 8.5.0 with `compileSdk 35`. Some Android Studio/Gradle combinations may require an AGP update for SDK 35 compatibility. The plugin version is defined in the root `build.gradle.kts`.

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
