<div align="center">

# 💰 TrackFinz

### Free Personal Finance Manager for Android

**Track smarter. Save better. Powered by AI.**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Design-Material%203-757575?style=flat-square&logo=materialdesign&logoColor=white)](https://m3.material.io)
[![Gemini AI](https://img.shields.io/badge/AI-Google%20Gemini-8E75B2?style=flat-square&logo=google&logoColor=white)](https://ai.google.dev)
[![Version](https://img.shields.io/badge/Version-1.1.2-brightgreen?style=flat-square)](https://github.com)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26-orange?style=flat-square)](https://developer.android.com/tools/releases/platforms)

---

*Built for the **Kiro Birthday Week Coding Challenge** — powered by Kiro AI.*

</div>

---

## 📖 Overview

TrackFinz is a free, privacy-focused personal finance Android application that helps users take control of their money. From logging daily expenses to scanning receipts with your camera, setting savings goals, managing bill reminders, and getting an AI-powered monthly financial analysis — TrackFinz brings everything together in a clean, modern interface.

All your data stays on your device. No subscriptions. No ads. Just smart finance tracking.

---

## ✨ Features

### 📊 Core Finance Management
- **Expense & Income Tracking** — Log transactions with categories, notes, and dates. Edit or delete entries anytime.
- **Budget Management** — Set monthly spending limits per category. Get visual progress bars and over-budget alerts.
- **Savings Goals** — Create goals with target amounts, deadlines, and emoji identifiers. Track progress and add funds over time.
- **Analytics Dashboard** — Visualize spending patterns with charts, category breakdowns, and month-over-month comparisons.
- **Bill Reminders** — Schedule recurring reminders (weekly, monthly, yearly) with exact alarm notifications so you never miss a payment.

### 🤖 AI-Powered Features
- **AI Spending Insights** — Automatically analyzes your transaction history and surfaces actionable patterns: spending increases, saving opportunities, category alerts, and positive habits.
- **AI Budget Recommendations** — Get AI-suggested budget limits based on your actual spending history.
- **AI Financial Assistant** — A conversational chat interface powered by **Google Gemini 2.5 Flash**. Ask anything about your finances and get personalized, data-driven answers.
- **AI Monthly Financial Report** ⭐ — The flagship feature. A comprehensive monthly analysis combining local data processing with a Gemini-generated narrative. *(See dedicated section below.)*

### 📷 Receipt Scanner
- Scan paper receipts using your device camera with **CameraX**.
- **ML Kit OCR** extracts merchant names, line items, and totals automatically.
- Scanned data pre-fills the transaction form, saving manual entry time.

### 🔒 Security & Privacy
- **PIN protection** on every launch.
- **Biometric authentication** (fingerprint/face unlock).
- All data stored locally in an on-device **Room database** — nothing is sent to any server except for optional AI requests.

### 🌐 Localization
- Full multi-language support via a custom i18n system.
- Dark mode and light mode with **Material 3** dynamic theming.

---

## ⭐ Kiro Challenge Feature: AI Monthly Financial Report

> *This is the standout feature built specifically for the Kiro Birthday Week Coding Challenge.*

### The Problem

Most finance apps show you raw numbers — income, expenses, a pie chart. But numbers alone don't tell you *what to do*. Users are left to interpret their own data, and without context, it's easy to miss trends, overspend silently, or fail to reach savings goals.

### The Solution

The **AI Monthly Financial Report** transforms your raw transaction data into a complete, actionable financial health assessment — every month, automatically.

### How It Works

```
Your Transactions + Budgets + Goals
           │
           ▼
    AIReportAnalyzer
    ┌─────────────────────────────┐
    │  1. Compute monthly KPIs    │  ← Income, expenses, net balance, savings rate
    │  2. Category breakdown      │  ← Top 5 categories with MoM delta %
    │  3. Budget compliance       │  ← Under/over budget per category
    │  4. Goal progress           │  ← % toward target, deadline risk detection
    │  5. Health score (0–100)    │  ← Weighted: savings (40%) + budget (35%) + goals (25%)
    │  6. Smart recommendations   │  ← Up to 6 prioritized action items
    └─────────────────────────────┘
           │
           ▼
    Google Gemini 1.5 Flash
    ┌─────────────────────────────┐
    │  Generates a warm, personal │  ← 2-paragraph narrative in the user's language
    │  financial narrative        │  ← Works offline with a template fallback
    └─────────────────────────────┘
           │
           ▼
    Persisted in Room DB
    (cached for instant re-opens)
```

### What the Report Includes

| Section | Details |
|---|---|
| **Financial Health Score** | 0–100 score with label: Excellent / Good / Fair / Needs Work |
| **KPI Summary** | Income vs expenses with month-over-month delta chips |
| **Savings Rate** | Progress bar toward 20% target with MoM comparison |
| **Top Spending Categories** | Up to 5 categories with budget status and trend arrows |
| **Budget Performance** | Under/over budget count across all budgeted categories |
| **Goal Progress** | Visual progress for all active goals, risk flags for approaching deadlines |
| **AI Narrative** | Gemini-generated 2-paragraph personal summary (fallback works offline) |
| **Recommendations** | Actionable cards: fix over-budget, add missing budgets, protect at-risk goals, and more |

### Why It Stands Out

- **Works offline** — if Gemini is unavailable, a structured template narrative is generated locally.
- **Cached smartly** — reports are persisted in Room. Revisiting the same month is instant, while live KPIs (budgets, goals) always stay fresh.
- **Actionable** — recommendation cards have one-tap actions: set a budget directly from the report, navigate to goals, or jump to analytics.
- **Shareable** — the full report can be shared as formatted text via the Android share sheet.
- **Historical** — a month picker lets users browse and compare any previous month's report.

---

## 🛠️ Technology Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose |
| **Design System** | Material 3 |
| **Architecture** | MVVM (ViewModel + StateFlow + Repository) |
| **Dependency Injection** | Hilt (Dagger) |
| **Local Database** | Room (SQLite) |
| **Preferences** | DataStore (Preferences) |
| **Navigation** | Navigation Compose |
| **Camera** | CameraX (core, camera2, lifecycle, view) |
| **OCR / Text Recognition** | ML Kit Text Recognition |
| **AI / LLM** | Google Gemini AI (generativeai SDK) — `gemini-2.5-flash` for chat, `gemini-1.5-flash` for reports |
| **Image Loading** | Coil |
| **Biometric Auth** | AndroidX Biometric |
| **Networking** | OkHttp |
| **Async** | Kotlin Coroutines + Flow |
| **Code Gen** | KSP |

---

## 🏗️ Project Structure

```
app/src/main/java/com/trackfinz/app/
├── MainActivity.kt                  # Single-activity entry point
├── TrackFinzApp.kt                  # Application class (Hilt)
│
├── data/
│   ├── database/
│   │   ├── TrackFinzDatabase.kt     # Room database definition
│   │   ├── Daos.kt                  # All DAOs (transactions, budgets, goals, etc.)
│   │   ├── MonthlyReportDao.kt      # Monthly report persistence
│   │   └── Converters.kt            # Type converters (enums, etc.)
│   ├── datastore/
│   │   └── UserPreferences.kt       # DataStore: dark mode, language, currency
│   ├── model/
│   │   ├── Entities.kt              # All Room entities + enums (Transaction, Budget, Goal, etc.)
│   │   └── MonthlyReportEntity.kt   # Monthly report cache entity
│   └── repository/
│       ├── TransactionRepository.kt
│       ├── BudgetRepository.kt
│       ├── GoalRepository.kt
│       ├── MonthlyReportRepository.kt
│       ├── BillReminderRepository.kt
│       ├── UserRepository.kt
│       └── ChatRepository.kt
│
├── di/
│   ├── AppModule.kt                 # Hilt module — DB, DAOs, repos
│   └── SeedDataInitializer.kt       # Optional seed data on first launch
│
├── i18n/
│   ├── AppStrings.kt                # String key definitions
│   ├── LocalStrings.kt              # CompositionLocal for current strings
│   └── Translations.kt             # Multi-language translations
│
├── navigation/
│   ├── NavRoutes.kt                 # Route constants + helpers
│   └── AppNavGraph.kt              # Full NavHost with all composables
│
├── receiver/
│   └── BillReminderReceiver.kt      # AlarmManager broadcast receiver
│
├── ui/
│   ├── components/                  # Reusable composables (BarChart, BudgetProgressBar, etc.)
│   ├── screens/
│   │   ├── auth/                    # Login, Register, PIN setup, PIN lock, Onboarding
│   │   ├── dashboard/               # Home dashboard
│   │   ├── transactions/            # Transaction list + add/edit form
│   │   ├── budget/                  # Budget screen + AI budget recommendations
│   │   ├── goals/                   # Savings goals
│   │   ├── analytics/               # Charts and analytics
│   │   ├── bills/                   # Bill reminders
│   │   ├── receipt/                 # CameraX + ML Kit receipt scanner
│   │   ├── insights/                # AI spending insights
│   │   ├── assistant/               # AI chat assistant
│   │   ├── report/                  # AI monthly financial report ⭐
│   │   └── profile/                 # User profile + settings
│   └── theme/                       # Material 3 theme, colors, typography
│
├── utils/
│   ├── AIReportAnalyzer.kt          # Monthly report engine + Gemini narrative
│   ├── AIFinancialAssistant.kt      # Chat assistant Gemini integration
│   ├── AIInsightsGenerator.kt       # Rule-based spending insights
│   └── NotificationHelper.kt        # Notification channel + scheduling
│
└── viewmodel/
    ├── TransactionViewModel.kt
    ├── BudgetViewModel.kt
    ├── GoalViewModel.kt
    ├── AIReportViewModel.kt         # Monthly report state management
    ├── ChatViewModel.kt
    ├── SettingsViewModel.kt
    └── ...
```

---

## 🚀 Installation

### Prerequisites
- Android Studio Ladybug (2024.2.1) or later
- JDK 17
- Android device or emulator running Android 8.0+ (API 26+)
- A Google Gemini API key ([get one free at ai.google.dev](https://ai.google.dev))

### Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/TrackFinz.git
   cd TrackFinz
   ```

2. **Open in Android Studio**
   - File → Open → select the `TrackFinz` folder

3. **Configure the Gemini API key**

   Create or edit `local.properties` in the project root and add:
   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```
   Then reference it in `app/build.gradle.kts`:
   ```kotlin
   buildConfigField("String", "GEMINI_API_KEY", "\"${properties["GEMINI_API_KEY"]}\"")
   ```
   > ⚠️ Never commit your API key to version control. Add `local.properties` to `.gitignore`.

4. **Sync Gradle**
   - Android Studio will prompt you — click **Sync Now**

5. **Run the app**
   - Select your device/emulator and press ▶ **Run**

---

## 🤝 Built With Kiro

This project was developed with the assistance of **[Kiro](https://kiro.dev)**, an AI-powered development environment. Kiro was used throughout the entire development lifecycle:

- **Specification creation** — Defining feature requirements, acceptance criteria, and edge cases for each screen and data model.
- **Architecture planning** — Designing the MVVM layer separation, Hilt dependency graph, Room schema, and navigation structure.
- **Feature implementation** — Writing Compose UI, ViewModels, repositories, and the full AI Monthly Report pipeline from scratch.
- **Code improvement** — Refactoring, performance optimization, and applying best practices like smart caching for monthly reports and offline fallback narratives.
- **Debugging** — Diagnosing coroutine lifecycle issues, Gemini API error handling (retry with exponential backoff), and navigation back-stack edge cases.

Kiro's spec-driven workflow (Requirements → Design → Tasks → Implementation) made it possible to build a full-featured finance app with multiple AI integrations efficiently and with confidence.

---

## 🔮 Future Improvements

- [ ] **Export data** — CSV/PDF export of transactions and monthly reports
- [ ] **Cloud backup** — Optional encrypted sync via Firebase or Google Drive
- [ ] **Widgets** — Home screen widget showing current balance and budget status
- [ ] **Multi-currency** — Real-time exchange rates for travelers
- [ ] **Recurring transactions** — Auto-log salary and subscription charges
- [ ] **Bank import** — Parse bank statement PDFs or CSV exports
- [ ] **Shared budgets** — Household/family budget splitting
- [ ] **AI predictions** — Forecast end-of-month balance based on spending trends
- [ ] **Secure API key management** — Move Gemini API key to backend proxy to remove key from APK

---

## 📄 License

```
MIT License

Copyright (c) 2025 TrackFinz

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

<div align="center">

Made with ❤️ and Kiro AI · **TrackFinz v1.1.2**

</div>
