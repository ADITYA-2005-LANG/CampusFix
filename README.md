# CampusFix

CampusFix is a modern, real-time Android application built with Kotlin, Jetpack Compose, and Firebase designed to streamline campus issue reporting, tracking, and resolution between students and administration.

---

## Key Features

**Secure Authentication:** Complete Firebase Authentication workflow supporting user registration, secure login, and password reset.
**User Profile Setup:** Dedicated profile configuration capturing academic details, hostel information, and course mapping.
**Student Dashboard & Issue Reporting:**
  Report campus issues across multiple categories.
  Attach photo evidence directly captured or selected from device storage, synced via Firebase Storage.
  Track personal issue history logs and status updates.
**Issue Engagement (Me Too Upvoting):** Allows students to upvote existing reported issues to highlight high-priority campus problems.
**Admin Dashboard:** 
  Campus-wide visibility of all submitted issues.
  Advanced status filtering (Pending, In Progress, Resolved).
  Direct status updates and review capabilities.
**Real-Time Sync:** Instant UI updates across clients powered by Cloud Firestore real-time snapshot listeners.
**Modern Native UI:** Built entirely with Jetpack Compose and Material 3 components for a responsive and clean user interface.

---

## Tech Stack & Libraries

**Language:** Kotlin
**UI Toolkit:** Jetpack Compose, Material 3
**Backend & Database:** Cloud Firestore (Metadata, User Profiles, and Issue tracking)
**Storage:** Firebase Storage (`/issue_images/`)
**Authentication:** Firebase Auth
**Architecture:** MVVM (Model-View-ViewModel)

---
You can download the apk from the link given below:
https://drive.google.com/file/d/1PGMht9pY41GQ_ZNz7hyBJOUuens7QYvx/view?usp=sharing

Note- For admin accounts, we need to separately go to the Firebase Console and change the role of account from Student to Admin. All newly created accounts have the default role as "Student".

