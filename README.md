# GreenMile

GreenMile is an Android app for tracking everyday sustainability activities on campus.

The current app is built with Kotlin and Jetpack Compose and uses Firebase Authentication and Firestore for accounts, activity logs, points, streaks, leaderboards, and admin data.

## What the current app contains

The main activity wires together these screens:

- **Login / Sign up** — Firebase email/password authentication, email verification, password reset, and a student profile with name, roll number, and department.
- **Home** — the main navigation screen after login.
- **Log Activity** — records travel, food, electricity use, and plastic use; calculates a carbon value and points; prevents more than one activity log for the same user on the same date.
- **Leaderboard** — student and department rankings backed by Firestore.
- **Profile** — user details, theme preference, and logout.
- **Activity History** — previous activity records.
- **Step Counter** — reads the Android step counter sensor and keeps a daily count in `SharedPreferences`; the counter can continue in the background through a foreground service.
- **Admin** — dashboard and management tools for users and activity logs.

The project also keeps a streak value in Firestore and updates it as users log activity.

## Stack

- Kotlin
- Jetpack Compose / Material 3
- Android SDK
- Firebase Authentication
- Firebase Firestore
- Gradle + Android Gradle Plugin

The app module currently targets SDK 35, uses Kotlin 2.0.0, Android Gradle Plugin 8.5.0, and Java/Kotlin JVM target 11 for compilation.

There is no Gradle wrapper checked into this repository, so **Android Studio is the easiest way to build the project**.

## Before you start

Install:

- Android Studio
- Android SDK Platform 35
- Android SDK Build Tools
- JDK 17

JDK 17 is needed to run Android Gradle Plugin 8.x. The project itself compiles Java/Kotlin sources with target 11, which is different from the JDK used to run Gradle. citeturn898260search0turn898260search2

### Important build compatibility note

The repository currently uses **AGP 8.5.0** while `compileSdk` is **35**. Google's compatibility table lists API 35 support starting with AGP 8.6.0. As a result, a clean build may require updating the Android Gradle Plugin version in `build.gradle.kts` before the project will build reliably with SDK 35. citeturn898260search3turn898260search1

I have not changed that build configuration as part of this README update, so the limitation is documented here instead of being hidden.

## 1. Clone the repository

```bash
git clone https://github.com/Chetan-code-lrca/GreenMile-SPSU-2026.git
cd GreenMile-SPSU-2026
```

## 2. Open it in Android Studio

Open Android Studio and choose:

```text
File → Open → GreenMile-SPSU-2026
```

Let Android Studio import the Gradle project and install any missing SDK components it reports.

The project includes the `:app` module in `settings.gradle.kts`.

## 3. Check the Firebase configuration

The app uses the Firebase Android Gradle plugin and expects:

```text
app/google-services.json
```

A `google-services.json` file is already present in the repository and is configured for the package:

```text
com.spsu.greenmile
```

### Using the existing Firebase project

You can leave the file as it is while developing against the project's existing Firebase configuration, provided the corresponding Firebase services and Firestore rules are still available.

### Using your own Firebase project

For a separate deployment, create an Android app in your Firebase project with the package name:

```text
com.spsu.greenmile
```

Download its `google-services.json` and replace the repository copy:

```text
app/google-services.json
```

Do not publish service-account JSON files or server-side Firebase credentials. The Android client configuration file is not a service-account credential, but you should still use your own Firebase project for an independent deployment.

## 4. Sync and build

In Android Studio:

```text
File → Sync Project with Gradle Files
```

Then build the debug APK from:

```text
Build → Make Project
```

The application module is:

```text
:app
```

Because the repository does not contain `gradlew`, command-line builds require a separately installed Gradle version that matches the Android Gradle Plugin configuration. Android Studio is preferred for this repository.

## 5. Run on an Android device or emulator

Create or start an Android emulator with API 26 or newer, or connect a physical Android device with USB debugging enabled.

Then select the `app` run configuration and press **Run**.

The app's `minSdk` is 26 and `targetSdk` is 35.

## 6. Firebase setup required for the app

The code calls Firebase Authentication and Firestore directly.

At a minimum, the Firebase project used by the app needs:

### Authentication

Enable **Email/Password** authentication.

The sign-up flow creates a Firebase account, writes a matching document to the `users` collection, and sends an email-verification message.

The login flow checks whether the Firebase account's email has been verified before allowing the user into the app.

### Firestore

The app reads and writes collections including:

```text
users
activities
```

User documents contain fields such as name, roll number, department, role, total points, total carbon saved, activity count, streak, and last active date.

Activity documents contain fields such as travel, food, electricity hours, plastic use, carbon value, points earned, step data, and date.

**Firestore rules are not included in this repository.** Before using this project outside a controlled test environment, review and configure the rules for your Firebase project.

## 7. Sign-up and admin access

The sign-up code creates every new account with:

```text
role = student
```

Users cannot choose `admin` from the sign-up form.

The UI asks for an `@spsu.ac.in` email, but the current validation only checks that the entered value contains `@`; it does not enforce the SPSU domain in code. If domain restriction matters, that should be added to the authentication logic and Firebase rules rather than relying on the text shown on screen.

Admin functionality is selected from the Firestore user role. The project therefore needs an appropriate way to assign approved users the `admin` role in the Firebase backend.

## 8. Step counter

GreenMile uses Android's `TYPE_STEP_COUNTER` sensor.

On Android 10 and newer the app requests:

```text
ACTIVITY_RECOGNITION
```

The step service is started when the application launches. Daily steps are calculated using a stored offset and reset for a new date.

The background service runs as a foreground service and shows an ongoing notification containing:

- today's steps;
- an estimated distance in kilometres;
- an estimated calorie value.

The distance and calorie figures are simple formulas in the current code, not measurements from GPS or a medical-grade activity tracker.

Devices without a step-counter sensor cannot provide step data through this feature.

## 9. How carbon and points are calculated

The current activity logger uses fixed values in `LogActivityScreen.kt`.

Travel:

```text
Car      → 3.0
Bike     → 1.5
Bus      → 0.8
Cycle    → 0.0
Walk     → 0.0
```

Food:

```text
Non-Veg   → 3.5
Veg       → 1.0
Junk Food → 2.0
```

Electricity adds `0.5` per entered hour and plastic use adds `0.3`.

Points are then assigned from the resulting carbon value:

```text
carbon < 2.0  → 50 points
carbon < 3.0  → 35 points
carbon < 4.0  → 20 points
carbon < 5.0  → 10 points
otherwise     → 5 points
```

These are application-specific scoring rules. They should not be presented as official carbon accounting standards without further validation.

## 10. Project structure

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
│           ├── SharedComponents.kt
│           └── utils/
│               ├── AuthManager.kt
│               ├── StreakManager.kt
│               └── LogValidator.kt
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

## 11. Build configuration

The root Gradle configuration currently uses:

```text
Android Gradle Plugin  8.5.0
Kotlin                 2.0.0
Kotlin Compose plugin  2.0.0
Google services plugin 4.4.1
```

The app module uses:

```text
compileSdk 35
minSdk     26
targetSdk  35
Java       11 (compile target)
Kotlin     JVM target 11
```

The project also uses Compose BOM `2024.06.00`, Navigation Compose `2.7.7`, Firebase BOM `33.0.0`, and the AndroidX versions defined in `app/build.gradle.kts`.

## 12. Common problems

### Gradle says Java 11 is not supported

Use **JDK 17** for the Gradle/Android Studio build environment. The `jvmTarget = "11"` line in `app/build.gradle.kts` does not mean Gradle itself should run on JDK 11.

### Build fails around `compileSdk 35`

Check the AGP version first. This repository currently uses AGP 8.5.0, while Google's documented API 35 support begins with AGP 8.6.0. Updating the plugin may be necessary. citeturn898260search3

### Firebase initialization fails

Check:

1. `app/google-services.json` exists.
2. Its package name matches `com.spsu.greenmile`.
3. Firebase Authentication is enabled for the project.
4. Firestore is enabled.
5. The device/emulator has internet access.

### Login succeeds but the app does not load the user

The app reads the matching document from:

```text
users/{firebase-uid}
```

Check that the document exists and contains the fields expected by `AuthManager` and `MainActivity`.

### Step counting does not work

Make sure the device has a `TYPE_STEP_COUNTER` sensor and that the app has `ACTIVITY_RECOGNITION` permission where Android requires it.

### Leaderboard or activity history is empty

Those screens read Firestore data. Check the Firestore collections, authentication state, network connection, and Firestore security rules.

## What is not in this repository

The current repository does **not** include:

- a backend server separate from Firebase;
- Firestore security rules;
- a documented production deployment configuration;
- a Gradle wrapper (`gradlew` / `gradlew.bat`);
- automated UI/instrumentation tests visible in the checked-in tree.

That means a new developer should open the project in Android Studio and configure Firebase before expecting the full application to work.

## Development notes

The code is organized around Compose screens with application state coordinated from `MainActivity`. Firebase access is performed directly from screen/util classes rather than through a separate repository/data layer.

That keeps the project straightforward for a small prototype, but it is also the first area I would refactor before growing the application: move Firebase queries and business rules behind dedicated repositories/view models and add Firebase rules plus tests.

## Team

- Chetan
- Srikanth

## License

No license file is currently documented in the repository README. Check the repository before redistributing the project.