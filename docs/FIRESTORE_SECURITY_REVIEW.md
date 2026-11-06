# Firestore security and deployment review

**Status: not approved for deployment with real student records.** This review reflects the client code currently in this repository. It is an implementation checklist, not a substitute for a Firebase Emulator rules test or institutional privacy/security review.

## Scope inspected

The Android client uses Firebase Authentication and Cloud Firestore. The main Firestore collections and operations visible in the current source are:

| Collection | Current use | Fields or access pattern |
| --- | --- | --- |
| `users/{uid}` | Account profile and aggregate statistics | Name, roll number, email, department, role, points, carbon total, activity count, streak, joined date |
| `activities/{activityId}` | Daily activity log | User ID, name/roll number, travel, food, electricity hours, plastic flag, carbon, points, timestamp, steps, distance |

The client creates student documents in `AuthManager.signUp`. It reads individual user documents for profile/home views. The leaderboard listens to the entire `users` collection. The admin screen reads the entire `users` and `activities` collections and can delete logs. Activity submission writes an activity from the Android client and then increments fields in the user's document.

## Deployment blockers

### 1. No Firestore Security Rules are included

The Android client is not a security boundary. Hiding the Admin Panel unless a Firestore role is `admin` only affects navigation in the UI. A signed-in user can call Firestore directly using the SDK or another client.

Until restrictive Firestore rules are deployed and tested, do not connect this app to a Firebase project containing real student records.

### 2. Private profile fields and public leaderboard data share one document

The `users/{uid}` document contains email and roll number alongside leaderboard fields. The current leaderboard reads the whole `users` collection. Firestore Rules authorize document access; they do not return a field-filtered version of an allowed document. Allowing every signed-in user to read the current leaderboard query therefore also exposes the other fields stored in those documents.

**Required data-model change:** separate private profile documents from deliberately public leaderboard projections. For example:

- `users/{uid}`: private fields such as email, roll number, joined date, and account preferences.
- `publicProfiles/{uid}`: only the display name (or approved display alias), department, and explicitly approved leaderboard totals.
- `activities/{activityId}`: activity details accessible to the owner and verified administrators only.

Do not copy email, roll number, or other identifying information into `publicProfiles`. Decide with the institution whether even display names and environmental activity totals may be public to all authenticated students.

### 3. Client-controlled role and aggregate fields

The app currently stores `role`, `totalPoints`, `totalCarbonSaved`, `totalActivitiesLogged`, and `currentStreak` in the user document. A client must not be allowed to change its own role or award itself points by writing these values.

A secure deployment should issue administrator privileges using a trusted server-side process and Firebase custom claims (for example, `request.auth.token.admin == true` in Rules). Do not trust a role value supplied by the client or accept an admin designation from the public sign-up form.

### 4. Activity scoring is calculated in the Android client

The activity screen computes carbon and points on the device, then writes those values and increments profile totals from the client. A modified client can change the payload. The duplicate-daily-log check is also a separate query followed by a write, not an atomic operation; two concurrent requests can race.

**Required change for trustworthy points:** move activity validation, scoring, duplicate prevention, and aggregate updates into a trusted Cloud Function or backend transaction. Derive points and totals on the server from validated input. Treat existing client-supplied carbon/points as untrusted during migration.

### 5. Administrative reads and deletion need server-enforced authorization

The Admin screen loads all user documents and all activity documents, then deletes an activity and recalculates that user's totals from the client. This is a privileged operation regardless of which button is displayed.

Use custom-claim authorization in Rules for any administrator-only reads and writes. Prefer paginated admin endpoints or carefully constrained queries for larger deployments. Keep score recalculation and deletion-related aggregation in a trusted server transaction/function.

## Suggested security policy shape

The following is the intended authorization model, **not a deployable Rules file**. It depends on completing the data-model and server-side scoring changes above.

| Resource | Student | Verified administrator |
| --- | --- | --- |
| `users/{ownUid}` | Read own private profile; update only an explicit allowlist of non-privileged profile fields | Read as operationally required |
| `users/{otherUid}` | No access | Restricted administrative access |
| `publicProfiles/{uid}` | Read only approved public projection | Read |
| `activities/{id}` | Read own activity history; no direct client write of score/aggregate fields | Read and use privileged moderation workflow |
| `leaderboards/{id}` | Read approved aggregate output | Read |
| Role claims and aggregate statistics | No client writes | Set/update only through privileged server tooling |

The final Rules must also validate field types, required keys, value ranges, ownership, and permitted changes. Use default-deny behavior for unlisted paths. Add Firebase App Check as an additional abuse-control layer, not as a replacement for authentication, authorization, or server-side validation.

## Safe rollout sequence

1. **Freeze real-data rollout.** Use a non-production Firebase project with synthetic accounts while the security model is built.
2. **Agree on data visibility.** Obtain institutional approval for what appears on student and department leaderboards; decide whether display names should be aliases.
3. **Separate public and private data.** Move leaderboard fields into `publicProfiles` and update the leaderboard client to query only that collection.
4. **Move privileged writes to trusted code.** Create an authenticated server-side path for activity creation, scoring, streak updates, aggregate updates, and admin deletion/recalculation. Grant admin custom claims through a trusted operator process.
5. **Write and test Firestore Rules.** Use the Firebase Emulator Suite to prove that unauthenticated access, cross-user profile reads/writes, role escalation, forged score updates, and unauthorized admin reads/deletes are rejected. Include positive owner/admin test cases.
6. **Enable production controls.** Configure App Check, monitoring, backup/retention, and a student-data deletion/contact process. Confirm who owns Firebase billing and incident response.
7. **Deploy only after verification.** Record the Firebase project, deployed Rules version, tested app build, and approver.

## Validation checklist

- [ ] Unauthenticated users cannot read or write any profile, activity, or leaderboard data.
- [ ] A student cannot read another student's private profile or activity history.
- [ ] A student cannot set their own role, points, carbon totals, streak, or activity count.
- [ ] A student cannot create an activity for another UID or forge its score.
- [ ] Two simultaneous daily submissions cannot create duplicate records or double-count totals.
- [ ] Students can read only the approved public leaderboard projection.
- [ ] Only a verified administrator can perform admin reads, moderation, and deletion.
- [ ] Admin deletion and aggregate recalculation are atomic or recoverable.
- [ ] Rules are tested in the Firebase Emulator Suite and deployed to the intended project.
- [ ] A data-retention and deletion process is documented and owned.

## Files reviewed

- `AuthManager.kt`
- `MainActivity.kt`
- `HomeScreen.kt`
- `ProfileScreen.kt`
- `LeaderboardScreen.kt`
- `AdminScreen.kt`
- `LogActivityScreen.kt`
- `ActivityHistoryScreen.kt`
- `AndroidManifest.xml`
- `app/build.gradle.kts`

## Current decision

**Do not label GreenMile production-ready for an institution until all deployment blockers above are resolved and tested.** The repository README already notes that Firestore Rules are absent; this review expands that warning into a specific migration and validation plan. No permissive or speculative Rules file has been added.
