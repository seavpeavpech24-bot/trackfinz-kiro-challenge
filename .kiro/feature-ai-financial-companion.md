# Feature Spec — AI Financial Companion

> **Feature:** AI Financial Companion  
> **Parent spec:** `trackfinz-spec.md`  
> **Status:** Draft  
> **Target version:** 1.2.0  
> **Author:** SEAVPEAV PECH

---

## Table of Contents

1. [Overview](#1-overview)
2. [User Stories](#2-user-stories)
3. [Scope & Boundaries](#3-scope--boundaries)
4. [Technical Requirements](#4-technical-requirements)
5. [Data Layer Changes](#5-data-layer-changes)
6. [UI Flow](#6-ui-flow)
7. [Component Breakdown](#7-component-breakdown)
8. [Implementation Plan](#8-implementation-plan)
9. [Acceptance Criteria](#9-acceptance-criteria)
10. [i18n Keys](#10-i18n-keys)
11. [Open Questions](#11-open-questions)

---

## 1. Overview

The **AI Financial Companion** is a unified, context-aware AI hub that replaces the current three separate AI entry points
(AI Insights, AI Budget Recommendations, AI Assistant) with a single cohesive experience.

It introduces two new capabilities not currently present in the app:

- **Monthly Financial Report** — a generated narrative summary of the user's financial month, including KPIs, behavioral patterns, budget performance, and goal progress.
- **Personalized Recommendations Engine** — proactive, prioritized action cards driven by cross-domain analysis of transactions, budgets, and goals together.

The Companion is accessed from a dedicated bottom-sheet hub on the Dashboard, keeping it one tap away without consuming a bottom nav slot. It is fully offline-capable for the analysis layer; Gemini is called only for natural-language narrative generation, with a graceful offline fallback.

### 1.1 Relationship to Existing AI Features

| Existing feature | Fate |
|---|---|
| `AIInsightsScreen` | Absorbed into Companion → Insights tab; existing code reused |
| `AIAssistantScreen` | Absorbed into Companion → Chat tab; `ChatViewModel` + `ChatRepository` unchanged |
| `AIBudgetRecommendationsScreen` | Absorbed into Companion → Recommendations tab; apply flow unchanged |
| Dashboard AI banners | Replaced by a single "AI Companion" entry card |

The existing routes (`ai_insights`, `ai_budget_recommendations`, `ai_assistant`) are kept in `NavRoutes` as deep-link targets for backward compatibility but are no longer shown in the bottom nav or dashboard.

---

## 2. User Stories

### Epic: Monthly Financial Report

**US-01** — As a user, I want to read a plain-language summary of last month's finances so I can understand my overall performance without manually reading charts.

> **Acceptance:** A "Monthly Report" card is generated once per calendar month. It includes net balance, savings rate, top expense category, budget compliance, and a goal progress note. The narrative is readable in the user's selected language.

**US-02** — As a user, I want to share my monthly report as a screenshot or text so I can discuss it with a partner or financial advisor.

> **Acceptance:** A share icon on the report card triggers Android's share sheet with the report text pre-filled.

**US-03** — As a user, I want to view past monthly reports so I can track how my financial behavior changes over time.

> **Acceptance:** A "Past Reports" section shows up to 12 previous monthly summaries, newest first.

### Epic: Personalized Recommendations

**US-04** — As a user, I want to see actionable recommendations based on my actual spending, budgets, and goals so I know exactly what to do next.

> **Acceptance:** At least 3 recommendation cards appear when the user has ≥ 5 transactions. Each card has a clear title, explanation, and one-tap action button.

**US-05** — As a user, I want recommendations to tell me if I am on track to meet a savings goal so I can adjust my spending before the deadline.

> **Acceptance:** If a goal has a deadline and current contribution rate is insufficient, a goal-specific recommendation card appears with the projected shortfall.

**US-06** — As a user, I want to dismiss a recommendation I do not want to act on so the list stays relevant.

> **Acceptance:** Each recommendation card has a dismiss (✕) button. Dismissed recommendations do not reappear until the next calendar month.

**US-07** — As a user, I want to apply a budget recommendation directly from the Companion screen so I do not have to navigate to the Budget screen manually.

> **Acceptance:** Recommendation cards of type `BUDGET_ADJUSTMENT` have an "Apply" button that creates or updates the budget in one tap.

### Epic: Behavior Analysis

**US-08** — As a user, I want the Companion to detect spending pattern changes (week-over-week, month-over-month) and alert me before they become a problem.

> **Acceptance:** If a category's spending this month is ≥ 20% higher than the previous month's average, a `TREND_ALERT` recommendation card is shown.

**US-09** — As a user, I want to see a financial health score so I have a single number that reflects my current habits.

> **Acceptance:** A 0–100 score is displayed on the Companion hub card and updated each time the Companion is opened. The score is computed from savings rate, budget compliance, and goal progress.

### Epic: Chat & Continuity

**US-10** — As a user, I want to ask a follow-up question about my monthly report within the same Companion session so I can dig deeper without switching screens.

> **Acceptance:** From the Report tab, a "Ask AI" button pre-fills the Chat tab with the report context already loaded.

---

## 3. Scope & Boundaries

### In scope
- `AICompanionScreen` — tabbed hub (Report / Recommendations / Insights / Chat)
- `FinancialHealthScore` computation utility
- `MonthlyReportGenerator` — analysis + Gemini narrative call
- `RecommendationEngine` — cross-domain rule engine (transactions + budgets + goals)
- `MonthlyReportEntity` — Room table for persisted reports
- `DismissedRecommendationEntity` — Room table for dismissed recommendation IDs
- Dashboard entry card replacing existing AI banners
- New `Str` i18n keys for all new UI text
- `NavRoutes.AI_COMPANION` route

### Out of scope
- Removing existing `ai_insights`, `ai_budget_recommendations`, `ai_assistant` routes (kept for compatibility)
- Changing `ChatRepository`, `ChatMessageEntity`, or chat persistence logic
- Cloud sync of reports
- PDF/CSV export of reports (tracked in parent spec Phase 2)
- Push notification for new monthly report (tracked in parent spec Phase 3b)

---

## 4. Technical Requirements

### 4.1 Architecture

Follow the existing MVVM + Repository + Hilt pattern exactly as used in `AIAssistantScreen` and `AIBudgetRecommendationsScreen`:

```
AICompanionScreen (Compose)
    └── AICompanionViewModel (HiltViewModel)
            ├── TransactionRepository  (injected, existing)
            ├── BudgetRepository       (injected, existing)
            ├── GoalRepository         (injected, existing)
            ├── MonthlyReportRepository  (new)
            └── UserPreferences          (existing)
```

`AICompanionViewModel` exposes a single `UiState` sealed class collected via `collectAsStateWithLifecycle`.

### 4.2 UiState Design

```kotlin
sealed class AICompanionUiState {
    object Loading : AICompanionUiState()
    data class Ready(
        val healthScore: Int,                          // 0–100
        val healthLabel: HealthLabel,                  // EXCELLENT / GOOD / FAIR / NEEDS_WORK
        val currentReport: MonthlyReportEntity?,       // null if not yet generated
        val reportState: ReportGenerationState,
        val recommendations: List<CompanionRecommendation>,
        val recommendationsState: LoadingState,
        val currency: String
    ) : AICompanionUiState()
    data class Error(val message: String) : AICompanionUiState()
}

enum class HealthLabel { EXCELLENT, GOOD, FAIR, NEEDS_WORK }
enum class ReportGenerationState { IDLE, GENERATING, DONE, ERROR }
enum class LoadingState { IDLE, LOADING, DONE, ERROR }
```

### 4.3 Financial Health Score Algorithm

Computed entirely offline (no Gemini call) inside `FinancialHealthScore.compute()`:

```
score = (savingsRateScore * 0.40)
      + (budgetComplianceScore * 0.35)
      + (goalProgressScore * 0.25)

savingsRateScore     = clamp(savingsRate / 0.20, 0.0, 1.0) * 100
  // 20% savings rate = perfect score

budgetComplianceScore = (categoriesUnderBudget / totalBudgetedCategories) * 100
  // No budgets set → 50 (neutral)

goalProgressScore     = average(goal.savedAmount / goal.targetAmount) * 100
  // No goals → 50 (neutral)

label:
  80–100 → EXCELLENT
  60–79  → GOOD
  40–59  → FAIR
  0–39   → NEEDS_WORK
```

All inputs read from existing Room DAOs with date range = current calendar month.

### 4.4 Monthly Report Generation

`MonthlyReportGenerator.generate(transactions, budgets, goals, currency, language)`:

1. **Compute KPIs offline** (no network):
   - Total income, total expenses, net balance, savings rate
   - Top 3 expense categories + amounts
   - Budget compliance: categories over/under/on-track
   - Goal progress: goals completed, goals at risk (deadline approaching + underfunded)

2. **Build a structured prompt** containing the KPIs and send to Gemini (`generativeai` SDK, reusing the existing pattern from `AIFinancialAssistant`):
   ```
   "You are a personal finance advisor. Given the following data for [Month Year]:
   Income: X, Expenses: Y, Net: Z, Savings rate: W%
   Top categories: [list]
   Budgets: [over/under summary]
   Goals: [progress summary]
   Write a friendly, encouraging 3-paragraph financial summary in [language].
   Paragraph 1: overall performance. Paragraph 2: what went well. Paragraph 3: one key improvement tip."
   ```

3. **Offline fallback**: if Gemini call fails or device is offline, assemble the narrative from template strings using the computed KPIs.

4. **Persist** the result as a `MonthlyReportEntity` (month, year, narrative, kpiJson, generatedAt).

5. **Cache rule**: only re-generate if no report exists for the current month, or if the user explicitly taps "Regenerate".

### 4.5 Recommendation Engine

`RecommendationEngine.generate(transactions, budgets, goals, dismissedIds)` returns `List<CompanionRecommendation>`.

Runs entirely offline. Each rule produces zero or one `CompanionRecommendation`.

| Rule ID | Trigger condition | Type | Action |
|---|---|---|---|
| `R01` | Category spending ≥ 20% above prior month | `TREND_ALERT` | Navigate to Analytics |
| `R02` | Category over budget this month | `BUDGET_ALERT` | Apply suggested lower budget |
| `R03` | Category with no budget but consistent spending | `MISSING_BUDGET` | `BUDGET_ADJUSTMENT` — one-tap create |
| `R04` | Savings rate < 10% | `SAVINGS_ALERT` | Navigate to Goals |
| `R05` | Goal with deadline within 60 days and < 50% funded | `GOAL_AT_RISK` | Navigate to Goals |
| `R06` | Savings rate > 30% for 2+ consecutive months | `POSITIVE_HABIT` | Dismiss only (encouragement) |
| `R07` | No transactions recorded in last 7 days | `ENGAGEMENT` | Navigate to Transactions |
| `R08` | Bill overdue (dueDateMillis < now) | `OVERDUE_BILL` | Navigate to Bill Reminders |

Rules are evaluated in priority order: R08 → R05 → R02 → R01 → R03 → R04 → R07 → R06.
Maximum 6 recommendations shown at once. Dismissed IDs are filtered before display.

### 4.6 Dismiss Persistence

`DismissedRecommendationEntity(ruleId: String, dismissedAt: Long)` stored in Room.
Dismissed recommendations are excluded until `dismissedAt + 30 days < now` (auto-expire monthly).

### 4.7 Gemini Prompt Safety

- Wrap every Gemini call in `try/catch`; on exception set `reportState = ERROR` and surface the offline fallback.
- Reuse the existing `INTERNET` permission — no new permissions required.
- Do not send raw transaction titles to Gemini to avoid leaking merchant names. Send only aggregated category totals and amounts.

---

## 5. Data Layer Changes

### 5.1 New Entities

```kotlin
// Room table: monthly_reports
@Entity(tableName = "monthly_reports")
data class MonthlyReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val month: Int,           // 1–12
    val year: Int,
    val narrative: String,    // Gemini-generated or template narrative
    val kpiJson: String,      // JSON snapshot of KPIs used to generate the report
    val healthScore: Int,     // 0–100 at time of generation
    val generatedAt: Long = System.currentTimeMillis()
)

// Room table: dismissed_recommendations
@Entity(tableName = "dismissed_recommendations")
data class DismissedRecommendationEntity(
    @PrimaryKey val ruleId: String,   // e.g. "R01_FOOD", "R05_goal_42"
    val dismissedAt: Long = System.currentTimeMillis()
)
```

### 5.2 New DAOs

```kotlin
@Dao interface MonthlyReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: MonthlyReportEntity): Long

    @Query("SELECT * FROM monthly_reports WHERE month = :month AND year = :year LIMIT 1")
    suspend fun getForMonth(month: Int, year: Int): MonthlyReportEntity?

    @Query("SELECT * FROM monthly_reports ORDER BY year DESC, month DESC")
    fun getAllFlow(): Flow<List<MonthlyReportEntity>>

    @Query("DELETE FROM monthly_reports WHERE year < :year OR (year = :year AND month < :month)")
    suspend fun deleteOlderThan(month: Int, year: Int)
}

@Dao interface DismissedRecommendationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DismissedRecommendationEntity)

    @Query("SELECT ruleId FROM dismissed_recommendations WHERE dismissedAt + 2592000000 > :now")
    suspend fun getActiveDismissedIds(now: Long = System.currentTimeMillis()): List<String>

    @Query("DELETE FROM dismissed_recommendations WHERE dismissedAt + 2592000000 < :now")
    suspend fun pruneExpired(now: Long = System.currentTimeMillis())
}
```

### 5.3 New Repository

```kotlin
// data/repository/AICompanionRepository.kt
@Singleton
class AICompanionRepository @Inject constructor(
    private val reportDao: MonthlyReportDao,
    private val dismissedDao: DismissedRecommendationDao
) {
    fun allReportsFlow(): Flow<List<MonthlyReportEntity>> = reportDao.getAllFlow()
    suspend fun getReportForMonth(month: Int, year: Int) = reportDao.getForMonth(month, year)
    suspend fun saveReport(report: MonthlyReportEntity) = reportDao.insert(report)
    suspend fun getActiveDismissedIds() = dismissedDao.getActiveDismissedIds()
    suspend fun dismissRecommendation(ruleId: String) =
        dismissedDao.insert(DismissedRecommendationEntity(ruleId))
    suspend fun pruneExpiredDismissals() = dismissedDao.pruneExpired()
}
```

### 5.4 Database Migration (v5 → v6)

```kotlin
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `monthly_reports` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `month` INTEGER NOT NULL,
                `year` INTEGER NOT NULL,
                `narrative` TEXT NOT NULL,
                `kpiJson` TEXT NOT NULL,
                `healthScore` INTEGER NOT NULL,
                `generatedAt` INTEGER NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `dismissed_recommendations` (
                `ruleId` TEXT PRIMARY KEY NOT NULL,
                `dismissedAt` INTEGER NOT NULL
            )
        """)
    }
}
```

Register `MIGRATION_5_6` in `AppModule` alongside existing migrations.
Add `MonthlyReportDao` and `DismissedRecommendationDao` providers to `AppModule`.

---

## 6. UI Flow

### 6.1 Entry Point — Dashboard Card

The existing two AI banner cards on `DashboardScreen` are replaced by a single `AICompanionCard`:

```
┌────────────────────────────────────────────┐
│  🤖  AI Financial Companion          [→]   │
│  Health Score: 74  ·  GOOD                 │
│  3 new recommendations  ·  Report ready    │
└────────────────────────────────────────────┘
```

Tapping the card navigates to `NavRoutes.AI_COMPANION` (a full-screen destination, not a bottom sheet — keeps it consistent with existing AI screens that use `Scaffold`).

### 6.2 AI Companion Screen — Tab Structure

```
┌─────────────────────────────────────────┐
│  ←  AI Financial Companion         [⋮]  │
├────────┬────────────┬──────────┬────────┤
│ Report │   Recs (3) │ Insights │  Chat  │
├────────┴────────────┴──────────┴────────┤
│                                         │
│          [Tab content below]            │
│                                         │
└─────────────────────────────────────────┘
```

Tab badge shows unread count on Recs tab (dismissed ones do not count).
The overflow menu `[⋮]` contains: "Regenerate Report", "Clear Chat History".

### 6.3 Report Tab

```
┌── Gradient header card ─────────────────┐
│  📊  November 2026 Report               │
│  Health Score: 74 / 100   [GOOD badge]  │
└─────────────────────────────────────────┘

┌── KPI row ──────────────────────────────┐
│  Income: $2,400  Expenses: $1,680       │
│  Net: +$720      Savings: 30%           │
└─────────────────────────────────────────┘

┌── Top expense categories ───────────────┐
│  🍔 Food       $420   ████░░  (25%)     │
│  🛒 Groceries  $280   ███░░░  (17%)     │
│  🛍 Shopping   $190   ██░░░░  (11%)     │
└─────────────────────────────────────────┘

┌── AI Narrative ─────────────────────────┐
│  "November was a solid month for your   │
│  finances. Your savings rate of 30%..."  │
│                               [Share ↗] │
└─────────────────────────────────────────┘

┌── Budget Performance ───────────────────┐
│  ✅ 4 categories under budget           │
│  ⚠️  1 category over budget (Food)      │
└─────────────────────────────────────────┘

┌── Goal Progress ────────────────────────┐
│  🎯 Emergency Fund  ████████░░  80%     │
│  ✈️  Travel Fund    ████░░░░░░  42%  ⚠️ │
└─────────────────────────────────────────┘

[ Ask AI about this report ]  ← navigates to Chat tab
                                with report context pre-loaded

[ Past Reports ▼ ]            ← expandable section
```

**Loading state:** `CircularProgressIndicator` + "Generating your report…" text, same style as `AIInsightsScreen`.

**Error state:** `Card` with `errorContainer` color + retry button, same pattern as existing AI screens.

**Empty state (< 5 transactions):** Illustration + "Add at least 5 transactions to generate your first report."

### 6.4 Recommendations Tab

```
┌── Section: Action Required ─────────────┐
│  ┌── RecommendationCard ──────────────┐  │
│  │  ⚠️  Food spending up 35%           │  │
│  │  Your Food spending is 35% higher  │  │
│  │  than last month ($420 vs $312).   │  │
│  │  [ View in Analytics ]      [✕]   │  │
│  └───────────────────────────────────┘  │
│                                         │
│  ┌── RecommendationCard ──────────────┐  │
│  │  🎯  Travel Fund at risk            │  │
│  │  At current pace you'll be $240    │  │
│  │  short by your March deadline.     │  │
│  │  [ Add to Goal ]            [✕]   │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘

┌── Section: Quick Wins ──────────────────┐
│  ┌── RecommendationCard ──────────────┐  │
│  │  💡  No budget for Shopping         │  │
│  │  You spend ~$190/month on shopping │  │
│  │  but have no budget set.           │  │
│  │  [ Set Budget: $190 ]       [✕]   │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

Sections: "Action Required" (R01, R02, R05, R08), "Quick Wins" (R03, R04), "Doing Well" (R06).
Dismiss (✕) on any card persists the dismissal and removes the card with an animated exit (`AnimatedVisibility`).

### 6.5 Insights Tab

Reuses `InsightCard` composable from `AIInsightsScreen` with its existing logic and colors.
The refresh `IconButton` is moved to the overflow menu `[⋮]`.

### 6.6 Chat Tab

Reuses the chat bubble layout from `AIAssistantScreen` exactly (same `ChatBubble`, `WelcomeCard`, `SuggestedQuestionChip` composables). When the user arrives from "Ask AI about this report", the report KPI summary is prepended to the Gemini context for the next message only (not stored in `chat_messages`).

---

## 7. Component Breakdown

### 7.1 New Files

| File path | Type | Purpose |
|---|---|---|
| `ui/screens/companion/AICompanionScreen.kt` | Screen + tabs | Main tabbed screen |
| `ui/screens/companion/AICompanionViewModel.kt` | ViewModel | Single VM for entire companion |
| `ui/screens/companion/ReportTabContent.kt` | Composable | Report tab UI |
| `ui/screens/companion/RecommendationsTabContent.kt` | Composable | Recommendations tab UI |
| `ui/screens/companion/RecommendationCard.kt` | Composable | Single recommendation card |
| `ui/components/AICompanionCard.kt` | Composable | Dashboard entry card |
| `ui/components/HealthScoreBadge.kt` | Composable | Colored score pill (reusable) |
| `utils/FinancialHealthScore.kt` | Utility | Score computation |
| `utils/MonthlyReportGenerator.kt` | Utility | KPI analysis + Gemini call |
| `utils/RecommendationEngine.kt` | Utility | Rule-based recommendation logic |
| `data/model/MonthlyReportEntity.kt` | Entity | Monthly report DB model |
| `data/model/DismissedRecommendationEntity.kt` | Entity | Dismissed rec DB model |
| `data/model/CompanionRecommendation.kt` | Data class | In-memory recommendation model |
| `data/database/MonthlyReportDao.kt` | DAO | Report persistence |
| `data/database/DismissedRecommendationDao.kt` | DAO | Dismiss persistence |
| `data/repository/AICompanionRepository.kt` | Repository | Wraps both new DAOs |

### 7.2 Modified Files

| File | Change |
|---|---|
| `data/database/TrackFinzDatabase.kt` | Add 2 new entities + DAOs; bump version to 6 |
| `di/AppModule.kt` | Add `MIGRATION_5_6`; provide new DAOs + repository |
| `navigation/NavRoutes.kt` | Add `AI_COMPANION = "ai_companion"` constant |
| `navigation/AppNavGraph.kt` | Add `composable(NavRoutes.AI_COMPANION)` destination |
| `ui/screens/dashboard/DashboardScreen.kt` | Replace AI banner cards with `AICompanionCard` |
| `i18n/AppStrings.kt` | Add new `Str` keys (see §10) |
| `i18n/Translations.kt` | Add translations for all 11 languages |

### 7.3 Reused Without Change

| Component | Where reused |
|---|---|
| `InsightCard` from `AIInsightsScreen.kt` | Insights tab |
| `ChatBubble`, `WelcomeCard`, `SuggestedQuestionChip` from `AIAssistantScreen.kt` | Chat tab |
| `ChatViewModel`, `ChatRepository`, `ChatMessageEntity` | Chat tab |
| `AIInsightsGenerator` | Insights tab |
| `AIFinancialAssistant` | Chat tab + narrative generation |
| `BudgetViewModel.applyRecommendedBudgets()` | Recommendations tab apply action |
| `GradientCard`, `FinanceButton` components | Report tab header |
| `formatCurrency()` utility | KPI display throughout |

---

## 8. Implementation Plan

Effort estimates: XS < 2h · S = 2–4h · M = 4–8h · L = 1–2d · XL = 2–3d

### Step 1 — Data Layer (Day 1–2)

| Task | Effort |
|---|---|
| Create `MonthlyReportEntity` and `DismissedRecommendationEntity` | XS |
| Create `MonthlyReportDao` and `DismissedRecommendationDao` | S |
| Create `AICompanionRepository` | S |
| Write `MIGRATION_5_6` and register in `AppModule` | S |
| Register new entities in `TrackFinzDatabase` (bump to v6) | XS |
| Add DAO `@Provides` to `AppModule` | XS |

**Checkpoint:** App builds and migrates cleanly. No UI changes yet.

### Step 2 — Core Logic Utilities (Day 3–4)

| Task | Effort |
|---|---|
| Implement `FinancialHealthScore.compute()` | S |
| Implement `RecommendationEngine.generate()` with all 8 rules | L |
| Implement `MonthlyReportGenerator.generate()` — KPI analysis only (no Gemini yet) | M |
| Write unit tests for score and recommendation rules | M |

**Checkpoint:** Logic utilities return correct results on sample data. Score and recommendations are testable in isolation.

### Step 3 — ViewModel (Day 5)

| Task | Effort |
|---|---|
| Create `AICompanionViewModel` with `AICompanionUiState` | M |
| Wire `TransactionRepository`, `BudgetRepository`, `GoalRepository`, `AICompanionRepository` | S |
| Implement `loadCompanionData()`, `generateReport()`, `dismissRecommendation()` | M |
| Connect `UserPreferences.currency` and `UserPreferences.language` flows | S |

**Checkpoint:** ViewModel unit-testable; state transitions cover Loading → Ready → Error.

### Step 4 — Gemini Integration (Day 6)

| Task | Effort |
|---|---|
| Add Gemini narrative call to `MonthlyReportGenerator` using `AIFinancialAssistant` pattern | S |
| Implement offline fallback template narrative | S |
| Handle `ReportGenerationState` transitions in ViewModel | S |

**Checkpoint:** Report generates with real Gemini narrative on device with internet; falls back to template when offline.

### Step 5 — UI: Entry Card + Navigation (Day 7)

| Task | Effort |
|---|---|
| Create `AICompanionCard` composable for Dashboard | S |
| Add `HealthScoreBadge` composable | XS |
| Add `NavRoutes.AI_COMPANION` and `composable()` destination in `AppNavGraph` | S |
| Replace existing AI banner cards in `DashboardScreen` | S |

**Checkpoint:** Tapping the Dashboard card navigates to a placeholder Companion screen.

### Step 6 — UI: Companion Screen Tabs (Day 8–10)

| Task | Effort |
|---|---|
| Implement `AICompanionScreen` with `TabRow` and 4 tab content slots | M |
| Implement `ReportTabContent` — KPI row, category bars, narrative, budget performance, goal progress | XL |
| Implement `RecommendationsTabContent` — sectioned list with `RecommendationCard` | L |
| Implement `RecommendationCard` with action button + dismiss animation | M |
| Wire Insights tab (reuse `InsightCard`, call `AIInsightsGenerator`) | S |
| Wire Chat tab (reuse `AIAssistantScreen` composables) | S |
| Implement "Ask AI about this report" context injection | S |
| Implement overflow menu (Regenerate, Clear Chat) | S |

**Checkpoint:** All 4 tabs render correctly with real data. Dismiss animations work. Apply budget works end-to-end.

### Step 7 — i18n (Day 11)

| Task | Effort |
|---|---|
| Add new `Str` constants to `AppStrings.kt` | S |
| Add English translations to `Translations.kt` | S |
| Add Khmer translations | M |
| Add French translations | S |
| Add remaining 8 language translations | L |

**Checkpoint:** All new strings display correctly in EN, KH, FR. Other languages use EN fallback until translated.

### Step 8 — Polish & QA (Day 12–13)

| Task | Effort |
|---|---|
| Dark mode pass — verify all new cards and colors in dark theme | S |
| Empty states — verify < 5 transactions state on Report and Recs tabs | S |
| Error states — verify Gemini failure fallback on Report tab | S |
| Loading states — verify shimmer/progress indicators on all async operations | S |
| Verify `MIGRATION_5_6` on a device with existing v5 data | S |
| Manual QA on all 11 languages (layout overflow check) | M |

**Checkpoint:** Feature is complete. All acceptance criteria verified manually.

### Summary Timeline

| Day | Deliverable |
|---|---|
| 1–2 | Data layer complete |
| 3–4 | Logic utilities + unit tests |
| 5 | ViewModel |
| 6 | Gemini integration |
| 7 | Dashboard entry + navigation |
| 8–10 | All 4 tabs UI |
| 11 | Full i18n |
| 12–13 | Polish + QA |
| **Total** | **~13 working days** |

---

## 9. Acceptance Criteria

### AC-01: Health Score Display
- [ ] `AICompanionCard` on Dashboard shows a health score between 0 and 100
- [ ] Score updates every time the Companion screen is opened
- [ ] Score displays the correct label: EXCELLENT (80–100), GOOD (60–79), FAIR (40–59), NEEDS_WORK (0–39)
- [ ] Score is 50 when no transactions, budgets, or goals exist (neutral default)

### AC-02: Monthly Report Generation
- [ ] Report generates automatically on first open each calendar month
- [ ] Report is not re-generated on subsequent opens within the same month unless "Regenerate" is tapped
- [ ] Report displays: income, expenses, net balance, savings rate, top 3 expense categories
- [ ] Report displays a narrative paragraph (Gemini or fallback template)
- [ ] If Gemini is unavailable, a template narrative is displayed and no error blocks the report
- [ ] "Regenerate" in overflow menu triggers a fresh Gemini call and updates the stored report

### AC-03: Report History
- [ ] "Past Reports" section shows up to 12 previous monthly reports
- [ ] Reports are sorted newest first
- [ ] Each past report shows month/year, health score at time of generation, and the narrative

### AC-04: Report Sharing
- [ ] Tapping the share icon on the Report tab opens Android share sheet
- [ ] Shared text includes month/year, KPI summary, and the narrative paragraph
- [ ] Sharing works without internet

### AC-05: Recommendations
- [ ] At least 1 recommendation appears when the user has ≥ 5 transactions
- [ ] `BUDGET_ADJUSTMENT` recommendations have an "Apply" button that creates/updates the budget in one tap
- [ ] Navigation action buttons (`View in Analytics`, `Add to Goal`, `Go to Bills`) navigate to the correct screen
- [ ] Each recommendation card has a working dismiss (✕) button
- [ ] Dismissed recommendations disappear immediately with exit animation
- [ ] Dismissed recommendations do not reappear within the same calendar month
- [ ] Dismissed recommendations reappear after 30 days

### AC-06: Trend Alert Rule (R01)
- [ ] A `TREND_ALERT` card appears when a category's current month spending is ≥ 20% above the prior month
- [ ] The card text names the specific category and shows both the current and prior month amount

### AC-07: Goal At Risk Rule (R05)
- [ ] A `GOAL_AT_RISK` card appears when a goal has a deadline within 60 days AND `savedAmount / targetAmount < 0.5`
- [ ] The card text names the goal and shows the projected shortfall amount

### AC-08: Missing Budget Rule (R03)
- [ ] A `MISSING_BUDGET` card appears for categories with ≥ 3 expense transactions in the current month and no existing budget
- [ ] Tapping "Set Budget" pre-fills the recommended amount (average monthly spending for that category) and immediately creates the budget without navigating away

### AC-09: Insights Tab
- [ ] Insights tab content is identical in behavior to the existing `AIInsightsScreen`
- [ ] Insights are loaded when the tab is first selected (lazy load, not on screen open)

### AC-10: Chat Tab
- [ ] Chat tab is identical in behavior to the existing `AIAssistantScreen`
- [ ] Tapping "Ask AI about this report" on the Report tab switches to the Chat tab
- [ ] The first AI response after "Ask AI about this report" includes context from the current month's KPIs

### AC-11: Navigation & Routing
- [ ] `NavRoutes.AI_COMPANION` navigates to the Companion screen from the Dashboard card
- [ ] Back navigation from the Companion screen returns to Dashboard
- [ ] Deep links to `ai_insights`, `ai_budget_recommendations`, `ai_assistant` still resolve (backward compat)
- [ ] The Companion screen is NOT in the bottom nav bar

### AC-12: Dark Mode
- [ ] All new cards, badges, and recommendation colors look correct in both light and dark themes
- [ ] Health score badge color adapts: EXCELLENT=Emerald, GOOD=Teal, FAIR=Amber, NEEDS_WORK=Red

### AC-13: i18n
- [ ] All new UI text is served through `LocalStrings.current` — no hardcoded English strings
- [ ] English and Khmer translations are complete
- [ ] Missing translations in other languages fall back to English (no crashes)

### AC-14: Offline Behavior
- [ ] Score computation, recommendations, and report KPI section all work without internet
- [ ] Report narrative falls back to a template string when Gemini is unavailable
- [ ] An info note is shown below the narrative when the fallback was used: "Generated offline — connect to the internet for a personalized narrative"

### AC-15: Database Migration
- [ ] Existing users upgrading from DB v5 to v6 do not lose any data
- [ ] `monthly_reports` and `dismissed_recommendations` tables are created correctly on migration
- [ ] App does not crash on first launch after migration

---

## 10. i18n Keys

Add the following constants to `Str` object in `AppStrings.kt` and add corresponding translations to `Translations.kt` for all 11 languages.

```kotlin
// AI Companion hub
const val AI_COMPANION             = "ai_companion"
const val AI_FINANCIAL_COMPANION   = "ai_financial_companion"
const val COMPANION_SUBTITLE       = "companion_subtitle"
const val NEW_RECOMMENDATIONS      = "new_recommendations"    // "%d new recommendations"
const val REPORT_READY             = "report_ready"

// Health Score
const val HEALTH_SCORE             = "health_score"
const val SCORE_EXCELLENT          = "score_excellent"
const val SCORE_GOOD               = "score_good"
const val SCORE_FAIR               = "score_fair"
const val SCORE_NEEDS_WORK         = "score_needs_work"

// Report tab
const val MONTHLY_REPORT           = "monthly_report"         // "%s Report" (e.g. "November 2026 Report")
const val GENERATING_REPORT        = "generating_report"
const val REGENERATE_REPORT        = "regenerate_report"
const val REPORT_ERROR             = "report_error"
const val REPORT_NEEDS_MORE_DATA   = "report_needs_more_data"
const val BUDGET_PERFORMANCE       = "budget_performance"
const val UNDER_BUDGET             = "under_budget"           // "%d categories under budget"
const val OVER_BUDGET_COUNT        = "over_budget_count"      // "%d category over budget"
const val GOAL_PROGRESS_SECTION    = "goal_progress_section"
const val ASK_AI_ABOUT_REPORT      = "ask_ai_about_report"
const val PAST_REPORTS             = "past_reports"
const val OFFLINE_NARRATIVE_NOTE   = "offline_narrative_note"

// Recommendations tab
const val RECOMMENDATIONS          = "recommendations"
const val ACTION_REQUIRED          = "action_required"
const val QUICK_WINS               = "quick_wins"
const val DOING_WELL               = "doing_well"
const val NO_RECOMMENDATIONS       = "no_recommendations"
const val NO_RECS_DESC             = "no_recs_desc"

// Recommendation types
const val REC_TREND_ALERT_TITLE    = "rec_trend_alert_title"  // "%s spending up %d%%"
const val REC_TREND_ALERT_DESC     = "rec_trend_alert_desc"
const val REC_BUDGET_ALERT_TITLE   = "rec_budget_alert_title"
const val REC_MISSING_BUDGET_TITLE = "rec_missing_budget_title"
const val REC_SAVINGS_TITLE        = "rec_savings_title"
const val REC_GOAL_AT_RISK_TITLE   = "rec_goal_at_risk_title"
const val REC_POSITIVE_HABIT_TITLE = "rec_positive_habit_title"
const val REC_OVERDUE_BILL_TITLE   = "rec_overdue_bill_title"
const val SET_BUDGET               = "set_budget"             // "Set Budget: %s"
const val VIEW_IN_ANALYTICS        = "view_in_analytics"
const val ADD_TO_GOAL              = "add_to_goal"
const val GO_TO_BILLS              = "go_to_bills"
```

---

## 11. Open Questions

| # | Question | Owner | Status |
|---|---|---|---|
| Q1 | Should the Companion screen be a full-screen destination or a `ModalBottomSheet`? The spec proposes full-screen for consistency with existing AI screens, but a bottom sheet would feel more "companion-like". | SEAVPEAV | Open |
| Q2 | The `RecommendationEngine` rules use a 20% threshold for trend alerts. Is this too sensitive for users who track irregular income? Consider a configurable threshold or minimum 3-month baseline before triggering R01. | SEAVPEAV | Open |
| Q3 | Should `MonthlyReportEntity.kpiJson` store the full transaction list or only aggregates? Aggregates are safer (smaller DB rows, no raw data in narrative context), but the full list allows richer regeneration. Current spec uses aggregates only. | SEAVPEAV | Open |
| Q4 | Gemini API key handling — the existing app has this configured somewhere (not visible in the analyzed source). The Companion reuses the same configuration; confirm the key is not hardcoded in source. | SEAVPEAV | Needs confirmation |
| Q5 | For the "Ask AI about this report" context injection, should the report KPI be injected as a system message or prepended to the user message? The `AIFinancialAssistant` pattern uses user-message prepending; keep consistent or change? | SEAVPEAV | Open |

---

*This feature specification was authored based on TrackFinz v1.1.2 source analysis — July 2026.*
