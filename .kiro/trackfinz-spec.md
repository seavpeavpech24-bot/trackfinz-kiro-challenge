# TrackFinz — Application Specification

> **Version:** 1.1.2  
> **Author:** SEAVPEAV PECH  
> **Platform:** Android 8.0+ (minSdk 26, targetSdk 35)  
> **Package:** `com.trackfinz.app`  
> **License:** Free forever — no ads, no subscriptions

---

## Table of Contents

1. [Application Overview](#1-application-overview)
2. [Current Features](#2-current-features)
3. [Project Architecture](#3-project-architecture)
4. [Data Layer](#4-data-layer)
5. [Navigation Structure](#5-navigation-structure)
6. [UI & Design System](#6-ui--design-system)
7. [Internationalization](#7-internationalization)
8. [AI Features](#8-ai-features)
9. [Notifications & Background Work](#9-notifications--background-work)
10. [Possible Improvements](#10-possible-improvements)
11. [Development Plan](#11-development-plan)

---

## 1. Application Overview

TrackFinz is a personal finance tracker built entirely in Kotlin with Jetpack Compose. It allows users to record income and expenses, manage monthly budgets, set savings goals, track recurring bills, and get AI-powered financial insights — all in a single offline-first app distributed for free.

The app targets Southeast Asian users (Cambodia-first) with a strong multilingual focus covering 11 languages. The hero gradient, custom themes, and profile customization give it a polished consumer-app feel despite being the work of a solo developer.

---

## 2. Current Features

### 2.1 Onboarding & Auth

| Feature | Details |
|---|---|
| Splash screen | Animated logo + tagline, resolves correct start destination |
| 3-page onboarding | "Track Every Penny", "Set & Crush Goals", "Smart Budgeting" |
| Language selection | Available from the first onboarding screen |
| Registration | Name + email only (no external account required) |
| PIN setup | Custom numpad, two-step 4-digit PIN entry |
| PIN login | PIN-only lock screen on every app resume |
| Biometric unlock | Fingerprint via AndroidX Biometric |
| PIN change | In-app change via profile settings |

### 2.2 Dashboard

- Hero gradient card showing **total balance**, monthly **income**, and **expenses**
- Customizable gradient colors (stored as ARGB longs in DataStore)
- Quick action buttons: Scan Receipt, Budget, Goals
- Promo banners linking to AI Insights and AI Assistant
- Recent 5 transactions list

### 2.3 Transactions

- Full transaction list with **search bar** and **All / Income / Expense** filter tabs
- FABs for adding income, expense, and opening the receipt scanner
- **Add / Edit transaction** screen:
  - Keypad-style amount input with hero amount display
  - Income/Expense tab switcher
  - **SmartCategorizer** chip — suggests a category from the title (≥ 3 chars)
  - FlowRow emoji + label category picker
  - Date picker dialog
  - Optional note field
- Swipe-to-delete and tap-to-edit on transaction items
- Receipt image path stored per transaction (set via scanner)

### 2.4 Budget Management

- Monthly budgets per `TransactionCategory`
- Month/year picker for historical viewing
- `BudgetProgressBar` cards showing budgeted / spent / remaining
- "Over!" badge when a category exceeds its limit
- **Budget history** bottom sheet — full audit log (CREATED / UPDATED / DELETED) per budget
- AI Budget Recommendations banner linking to the AI sub-screen

### 2.5 Savings Goals

- Goal cards with emoji icon, progress ring, and target / saved amounts
- Add / Remove funds dialogs
- Completed goals shown with a "Done" badge
- **Per-goal fund history** and **global fund history** bottom sheets
- Goals sorted by creation date (newest first)

### 2.6 Analytics

- Period filters: **Today, Week, Month, Year, Custom** (date range)
- Type filters: All, Income, Expense
- KPI cards: Net Balance, Savings Rate
- **5 chart types** (all custom Canvas-drawn — no third-party chart library):
  - Donut chart (expense breakdown)
  - Pie chart (expense breakdown)
  - Bar chart (top expense categories)
  - Line / Trend chart (daily expense trend)
  - Grouped comparison bar chart (income vs expense)
- Category breakdown table with percentages
- Top transactions list for the selected period

### 2.7 Bill Reminders

- Bills grouped by status: **Overdue, Upcoming, Paused**
- Summary header card (total bills, overdue count, upcoming count)
- Add/edit bill: name, amount, due date, reminder time, frequency (Once/Weekly/Monthly/Yearly), category, note
- **Mark as Paid** dialog — records an expense automatically and advances the next due date
- Alarm-based notifications with auto-rescheduling for recurring bills

### 2.8 Profile & Settings

| Section | Settings |
|---|---|
| Profile | Avatar (gallery + in-app crop), name, email |
| Appearance | Dark mode toggle, hero card gradient color picker, language selector |
| Finance | Currency selector |
| Notifications | Over-budget alert toggle, large expense alert + configurable threshold |
| Security | Change PIN, biometric login toggle |
| About | App info, donate dialog (KHQR/ABA Pay + SWIFT), share app, rate app |

### 2.9 Receipt Scanner

- Live CameraX preview (optional hardware camera feature)
- One-tap photo capture
- ML Kit OCR text recognition
- Auto-parsing: merchant name, total amount, item list, suggested category
- Confirmation dialog with editable fields before saving
- Saves transaction + stores receipt image file path

### 2.10 AI Features

Three separate AI-powered screens (see §8 for details):

| Screen | Function |
|---|---|
| AI Insights | Personalized spending analysis cards |
| AI Budget Recommendations | Category-by-category budget suggestions with priority tiers |
| AI Assistant | Freeform chat interface backed by Google Gemini |

---

## 3. Project Architecture

### 3.1 Layer Overview

```
┌─────────────────────────────────────────────┐
│                  UI Layer                   │
│  Compose Screens  ←→  ViewModels (MVVM)     │
│        ↕                    ↕               │
│   Composition Locals    Hilt DI             │
├─────────────────────────────────────────────┤
│               Domain / Use-Case             │
│  Repositories (interfaces + impl)           │
│  SmartCategorizer, AIInsightsGenerator,     │
│  AIBudgetRecommender, AIFinancialAssistant, │
│  ReceiptScanner, BillReminderScheduler      │
├─────────────────────────────────────────────┤
│               Data Layer                   │
│  Room Database (v5)  │  DataStore Prefs     │
│  8 DAOs              │  UserPreferences     │
└─────────────────────────────────────────────┘
```

### 3.2 Tech Stack

| Category | Technology |
|---|---|
| Language | Kotlin |
| UI toolkit | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose (single `NavHost`) |
| Dependency injection | Hilt (with KSP) |
| Database | Room 5 (SQLite) |
| Preferences | DataStore Preferences |
| Asynchrony | Kotlin Coroutines + Flow |
| Image loading | Coil Compose |
| Camera | CameraX (core, camera2, lifecycle, view) |
| OCR | ML Kit Text Recognition |
| AI / LLM | Google Generative AI SDK (Gemini) |
| Biometrics | AndroidX Biometric |
| Splash | AndroidX Core SplashScreen |
| Networking | OkHttp |
| Build | Gradle KTS, KSP, AGP |

### 3.3 Key Patterns

- **MVVM** — each screen has a dedicated `ViewModel`; UI collects `StateFlow` / `Flow` via `collectAsStateWithLifecycle`
- **Repository pattern** — all data access goes through repository classes injected into ViewModels
- **Single-activity** — `MainActivity` hosts the single `NavHostController`; all screens are composable destinations
- **Offline-first** — Room is the single source of truth; no remote sync
- **Reactive UI** — Room DAOs return `Flow<List<T>>` that Compose collects; DataStore prefs also exposed as `Flow`
- **Composition locals** — `LocalStrings` and `LocalLanguage` distribute the current language/translations throughout the tree without prop-drilling
- **Seed data** — `SeedDataInitializer` inserts sample transactions/budgets/goals on first launch if no real user exists

### 3.4 Package Structure

```
com.trackfinz.app
├── data/
│   ├── database/       # Room DB, DAOs, Converters
│   ├── datastore/      # UserPreferences (DataStore)
│   ├── model/          # Entities + Enums
│   └── repository/     # Repository implementations
├── di/                 # Hilt modules, SeedDataInitializer
├── i18n/               # AppLanguage enum, Str keys, Translations
├── navigation/         # AppNavGraph, NavRoutes
├── receiver/           # BillReminderReceiver (BroadcastReceiver)
├── ui/
│   ├── components/     # Reusable composables
│   ├── screens/        # Feature screens + ViewModels
│   └── theme/          # Color, Typography, Theme
├── utils/              # BillReminderScheduler, formatCurrency, SmartCategorizer, etc.
├── MainActivity.kt
└── TrackFinzApp.kt     # @HiltAndroidApp
```

---

## 4. Data Layer

### 4.1 Room Database (v5)

Database file: `trackfinz.db`

| Table | Primary key | Notable columns |
|---|---|---|
| `users` | `id` (autoincrement) | name, email, pin, currency, notification prefs |
| `transactions` | `id` (autoincrement) | title, amount, type, category, date, receiptImagePath, scannedItems (JSON) |
| `budgets` | `id` (autoincrement) | category, limit, month, year |
| `budget_history` | `id` (autoincrement) | budgetId, oldLimit, newLimit, action, timestamp |
| `goals` | `id` (autoincrement) | title, targetAmount, savedAmount, emoji, deadline, isCompleted |
| `goal_history` | `id` (autoincrement) | goalId, amount, action, balanceBefore, balanceAfter |
| `chat_messages` | `id` (autoincrement) | text, isUser, timestamp |
| `bill_reminders` | `id` (autoincrement) | name, amount, dueDateMillis, reminderTimeMillis, frequency, isActive |

**Enums stored as TEXT:** `TransactionType`, `TransactionCategory`, `BudgetHistoryAction`, `GoalFundAction`, `BillFrequency`

**Migrations:**
- v3 → v4: no DDL change (new DAO query added)
- v4 → v5: `CREATE TABLE bill_reminders`
- Fallback destructive migration for schema older than v3

### 4.2 DataStore Preferences

All user preferences stored in `user_prefs` DataStore:

| Key | Type | Default |
|---|---|---|
| `is_onboarded` | Boolean | false |
| `is_logged_in` | Boolean | false |
| `dark_mode` | Boolean | false |
| `currency` | String | "USD" |
| `biometric_enabled` | Boolean | false |
| `notify_over_budget` | Boolean | true |
| `notify_large_expense` | Boolean | true |
| `large_expense_threshold` | Double | 100.0 |
| `hero_gradient_start` | Long (ARGB) | 0xFF00BCD4 (Teal500) |
| `hero_gradient_end` | Long (ARGB) | 0xFF4CAF50 (Emerald500) |
| `avatar_path` | String | "" |
| `language` | String | "en" |

### 4.3 Repositories

| Repository | Responsibilities |
|---|---|
| `UserRepository` | User CRUD, PIN management |
| `TransactionRepository` | Transaction CRUD, aggregations, search, date range queries |
| `BudgetRepository` | Budget CRUD + category/month lookups |
| `GoalRepository` | Goal CRUD, fund add/remove with history |
| `ChatRepository` | Chat message persistence |
| `BillReminderRepository` | Bill CRUD, active bill queries |

---

## 5. Navigation Structure

Single `NavHost` in `AppNavGraph.kt`. Start destination is resolved dynamically by `SplashScreen`.

```
splash
  ↓
┌── onboarding → register → pin_setup?name&email ──┐
│                                                    ↓
├── login ─────────────────────────────────────────→ dashboard
│                                                    ↑
└── pin_lock ──────────────────────────────────────┘

dashboard (bottom nav hub)
  ├── transactions
  │     └── add_transaction?id&type
  ├── budget
  │     └── ai_budget_recommendations
  ├── goals
  ├── bill_reminders
  ├── analytics
  ├── profile   (via avatar icon in dashboard header)
  ├── receipt_scanner
  ├── ai_insights
  └── ai_assistant
```

**Bottom nav items (6):** Home, Txns, Budget, Goals, Bills, Analytics

Main-route navigation uses `saveState = true` / `restoreState = true` to preserve scroll position when tab-switching.

---

## 6. UI & Design System

### 6.1 Color Palette

| Token | Light | Dark |
|---|---|---|
| Primary | Teal500 `#00BCD4` | Teal400 `#26C6DA` |
| Secondary | Emerald500 `#4CAF50` | Emerald400 `#66BB6A` |
| Background | SoftGray100 `#F5F7FA` | Navy900 `#0D1B2A` |
| Surface | White | Navy800 `#1B2A3B` |
| Income | IncomeGreen `#00C853` | (same) |
| Expense | ExpenseRed `#FF5252` | (same) |
| Warning | WarningAmber `#FFAB40` | (same) |

Hero card gradient defaults: Teal500 → Emerald500 (user-customizable).

### 6.2 Notable UI Components

| Component | Notes |
|---|---|
| `TrackFinzBottomBar` | Rounded top corners, animated color, circular selected indicator |
| `BudgetProgressBar` | Linear progress with percentage, over-budget badge |
| `BarChart` / `PieChart` | Custom `Canvas` drawing — no third-party chart library |
| `GradientCard` | Configurable gradient background card |
| `AvatarCropDialog` | In-app crop (no external library needed) |
| `ColorPickerDialog` | ARGB color picker for hero gradient |
| `SmartCategorizer chip` | Suggestion chip on AddTransactionScreen |

### 6.3 Theme

- Material 3 `MaterialTheme` with `TrackFinzTheme` wrapper
- Edge-to-edge with transparent status/nav bars
- `WindowCompat` insets for light/dark status bar icons
- Custom `TrackFinzTypography`

---

## 7. Internationalization

### Supported Languages (11)

| Code | Language | Script |
|---|---|---|
| en | English | Latin |
| kh | Khmer | Khmer script |
| fr | French | Latin |
| vi | Vietnamese | Latin + diacritics |
| lo | Lao | Lao script |
| zh | Chinese (Simplified) | CJK |
| ja | Japanese | CJK + Kana |
| ko | Korean | Hangul |
| my | Myanmar (Burmese) | Myanmar script |
| ms | Malay | Latin |
| id | Indonesian | Latin |

### Implementation

- **Custom Kotlin i18n** system — not Android `strings.xml`
- `Str` object holds all string constant keys
- `Translations.kt` contains one `Map<String, String>` per language (≈300+ keys each)
- `translate(key, language)` function with automatic English fallback
- `LocalStrings` composition local distributes translation lambda throughout the Compose tree
- `LocalLanguage` composition local holds the current `AppLanguage`
- Language change triggers recomposition without Activity restart
- Available on Onboarding screen and Profile → Language setting

---

## 8. AI Features

### 8.1 AI Insights (`AIInsightsScreen`)

- Analyzes all transactions to produce `AIInsight` objects
- 6 insight types: `SPENDING_INCREASE`, `SPENDING_DECREASE`, `SAVING_TIP`, `CATEGORY_ALERT`, `POSITIVE_HABIT`, `WARNING`
- Each insight has a title, message body, emoji icon, and type
- Refreshable on demand; regenerated on each screen entry
- Rule-based analysis engine (`AIInsightsGenerator`) — likely augmented by Gemini for message generation

### 8.2 AI Budget Recommendations (`AIBudgetRecommendationsScreen`)

- Analyzes transactions to suggest monthly limits per category
- Recommendations grouped by priority tier:
  - **Essential** — must-have expenses (rent, bills, groceries)
  - **Important** — regular expenses
  - **Flexible** — optional spending
- Shows current spending vs recommended amount + reasoning text
- Multi-select checkboxes per recommendation
- "Apply Budgets" button bulk-creates/updates budgets for selected categories
- Uses device locale country code for regional context (`AIBudgetRecommender`)

### 8.3 AI Assistant (`AIAssistantScreen`)

- Freeform chat interface backed by **Google Gemini** (`generativeai` SDK)
- Persistent chat history stored in `chat_messages` Room table
- Welcome card with 3 suggested questions generated from transaction patterns
- Clear history button
- Context-aware: sends current transaction summary to Gemini alongside user question
- Questions answered in the app's currently selected language

### 8.4 Receipt Scanner + SmartCategorizer

- **ReceiptScanner** — CameraX capture → ML Kit OCR → parse merchant, total, items, suggest category
- **SmartCategorizer** — live suggestion on AddTransactionScreen based on transaction title; learns from user corrections

---

## 9. Notifications & Background Work

### 9.1 Bill Reminder Notifications

- Scheduled via `AlarmManager` (exact alarms, `SCHEDULE_EXACT_ALARM` permission)
- Fires `BillReminderReceiver` (`BroadcastReceiver`, Hilt-injected) at the configured `reminderTimeMillis`
- Notification channel: `trackfinz_alerts`, high priority, custom sound + vibration
- Deep-link intent navigates to Bill Reminders screen on tap
- On fire, for recurring bills:
  1. Calculates next due date using `BillReminderScheduler.nextDueDate()`
  2. Updates the DB record
  3. Schedules the next alarm
- Boot receiver declared (`RECEIVE_BOOT_COMPLETED`) — alarms should be rescheduled after reboot

### 9.2 Declared Permissions

| Permission | Purpose |
|---|---|
| `USE_BIOMETRIC` / `USE_FINGERPRINT` | Biometric unlock |
| `POST_NOTIFICATIONS` | Android 13+ notification posting |
| `INTERNET` | Gemini AI API calls |
| `VIBRATE` | Notification haptics |
| `RECEIVE_BOOT_COMPLETED` | Reschedule alarms after reboot |
| `SCHEDULE_EXACT_ALARM` | Exact bill reminder alarms |
| `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE` | Avatar gallery picker |
| `CAMERA` | Receipt scanner |
| `BLUETOOTH` | Declared but no apparent current usage |

---

## 10. Possible Improvements

### 10.1 High Priority

| Area | Issue | Suggested Fix |
|---|---|---|
| **Security** | PIN stored as plain text in the `users` table | Hash PIN with BCrypt or PBKDF2 before storing |
| **Boot receiver** | `RECEIVE_BOOT_COMPLETED` is declared but no `BootReceiver` was found | Implement a `BootReceiver` that reschedules active bill alarms on reboot |
| **Bluetooth permission** | `BLUETOOTH` permission declared with no apparent use | Remove to reduce attack surface |
| **Data backup** | No export/import or cloud sync | Add JSON export and Google Drive backup |
| **Crash reporting** | No error tracking in place | Integrate Firebase Crashlytics or Sentry |

### 10.2 Architecture & Code Quality

| Area | Suggestion |
|---|---|
| **Password hashing** | Move PIN hash logic into a `SecurityRepository` |
| **ViewModel state** | Standardize on `UiState` sealed class per screen to avoid scattered `StateFlow` fields |
| **AI error handling** | Gemini API calls need retry logic + proper error states |
| **SmartCategorizer storage** | Currently unclear where learned corrections are persisted — should use Room or DataStore explicitly |
| **Receipt images** | No cleanup strategy for orphaned receipt image files when transactions are deleted |
| **Database export schema** | `exportSchema = false` should be changed to `true` and schemas committed to version control |

### 10.3 Feature Gaps

| Feature | Value |
|---|---|
| **Recurring transactions** | Auto-insert salary/rent on a schedule (similar to bill reminders) |
| **Multi-currency support** | Transactions are stored as raw doubles with a single global currency — add per-transaction currency + exchange rates |
| **Data export (CSV/PDF)** | Power users want to export for spreadsheets or tax purposes |
| **Widgets** | Home screen widget showing balance + quick-add button |
| **Category management** | Custom user-defined categories with custom emoji |
| **Cloud backup / sync** | Firebase Firestore or Google Drive for cross-device restore |
| **Spending notifications** | Push notification when a large expense is recorded (the setting exists but implementation is unclear) |
| **Transaction attachments** | Currently only receipt images from scan; allow any gallery photo to be attached |
| **Monthly summary notification** | Digest at month end: how much saved, top spending category |
| **Budget carry-over** | Option to roll unspent budget to next month |
| **Dark mode scheduling** | Auto-switch dark mode based on sunrise/sunset or a schedule |

### 10.4 UX Improvements

| Area | Suggestion |
|---|---|
| **Onboarding currency** | Let users pick their default currency during onboarding (not just in settings) |
| **Empty states** | Some screens have minimal empty states — add illustrated empty states with clear CTAs |
| **Analytics custom range** | The custom date picker could show a calendar view instead of text fields |
| **Transaction bulk actions** | Select multiple transactions to delete or re-categorize |
| **Goal deadline tracking** | Goals have a `deadline` field but no deadline progress or alert visible in the UI |
| **Bill overdue notification** | No explicit notification for overdue bills — only visual in-app badge |

---

## 11. Development Plan

The plan below is organized into three phases, building on the existing solid foundation.

---

### Phase 1 — Stability & Security (2–4 weeks)

**Goal:** Make the app production-ready and secure before adding new features.

| Task | Priority | Effort |
|---|---|---|
| Hash PIN with PBKDF2 before storing | Critical | S |
| Implement `BootReceiver` to reschedule bill alarms after reboot | High | S |
| Remove unused `BLUETOOTH` permission | High | XS |
| Add `exportSchema = true` to Room and commit schema files | Medium | XS |
| Add receipt image cleanup on transaction delete | Medium | S |
| Standardize ViewModels to sealed `UiState` pattern | Medium | M |
| Add Gemini API retry logic + user-visible error states | High | S |
| Clarify and persist SmartCategorizer learned corrections in Room/DataStore | Medium | S |

---

### Phase 2 — Core Feature Expansion (4–8 weeks)

**Goal:** Add the most-requested features that complement existing flows.

#### 2a. Recurring Transactions
- New `RecurringTransactionEntity` with frequency, start date, next date
- Background scheduling (AlarmManager, similar to bills) to insert transactions
- Management screen linked from Dashboard quick actions

#### 2b. Data Export
- CSV export of transactions for a date range
- Share via Android share sheet (no external service needed)
- Optional: simple PDF summary using Android's `PdfDocument` API

#### 2c. Custom Categories
- Allow users to create categories with name + emoji
- Extend `TransactionCategory` or add a `CustomCategoryEntity` table
- Show custom categories in the category picker alongside built-in ones

#### 2d. Spending Notifications
- Wire the existing `notifyLargeExpense` and `notifyOverBudget` settings to actual notification posting in `TransactionRepository.insert()`
- Use the already-created `trackfinz_alerts` notification channel

#### 2e. Goal Deadline Alerts
- Read `deadline` from `GoalEntity` and schedule a reminder alarm if a deadline is set
- Show a deadline progress bar or countdown in the goal card

---

### Phase 3 — Polish & Growth (ongoing)

**Goal:** Improve discoverability, performance, and user retention.

#### 3a. Home Screen Widget
- Balance + quick-add expense widget using `GlanceAppWidget`
- Read data from Room via a simple repository query
- Deep-link to AddTransactionScreen on tap

#### 3b. Monthly Summary Notification
- At month end, post a digest: net savings, top spending category, savings rate
- Scheduled via `WorkManager` `PeriodicWorkRequest` (more reliable than AlarmManager for long intervals)

#### 3c. Cloud Backup (Optional / Future)
- JSON export uploaded to user's Google Drive via Drive REST API (requires `driveFile` scope)
- Restore from backup on new device after login
- Keep fully optional — core app must work offline

#### 3d. AI Enhancements
- Pass budget data alongside transactions to Gemini for richer advice
- Add a "Monthly Report" AI feature that generates a natural-language summary of the month
- Consider caching AI responses to reduce API calls and work offline

#### 3e. Multi-Currency
- Add `currencyCode: String` field to `TransactionEntity`
- Store exchange rates (fetched once daily via OkHttp) in a new `ExchangeRateEntity`
- Display amounts in original currency + converted to home currency

---

### Milestone Summary

| Milestone | Target | Deliverable |
|---|---|---|
| M1 — Secure Foundation | +4 weeks | Hashed PIN, boot receiver, clean permissions, stable UiState |
| M2 — Recurring Transactions | +6 weeks | Auto-insert recurring income/expenses |
| M3 — Export & Notifications | +8 weeks | CSV export, spending alerts wired |
| M4 — Custom Categories | +10 weeks | User-defined categories end-to-end |
| M5 — Widget & Summary | +14 weeks | Home screen widget, monthly digest notification |
| M6 — AI Report & Cloud Backup | +18 weeks | AI monthly summary, optional Drive backup |

---

*This document was generated by analyzing the TrackFinz source code in July 2026.*
