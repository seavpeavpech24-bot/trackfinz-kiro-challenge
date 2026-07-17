# AI Monthly Financial Report — Feature Specification

> **Feature:** AI Monthly Financial Report  
> **App:** TrackFinz v1.1.2  
> **Target version:** 1.2.0  
> **Package:** `com.trackfinz.app`  
> **Status:** Specification — Ready for Implementation  
> **Author:** SEAVPEAV PECH  
> **Spec date:** July 2026

---

## Table of Contents

1. [Codebase Analysis Summary](#1-codebase-analysis-summary)
2. [User Stories](#2-user-stories)
3. [Functional Requirements](#3-functional-requirements)
4. [Technical Requirements](#4-technical-requirements)
5. [Data Layer Changes](#5-data-layer-changes)
6. [UI Design Specification](#6-ui-design-specification)
7. [File Changes Required](#7-file-changes-required)
8. [Development Plan](#8-development-plan)
9. [Testing Checklist](#9-testing-checklist)

---

## 1. Codebase Analysis Summary

### 1.1 Architecture

TrackFinz uses a clean **MVVM + Repository + Hilt** pattern across a single-Activity app:


```
UI Layer (Jetpack Compose + Material 3)
   └── ViewModel (@HiltViewModel, StateFlow / collectAsStateWithLifecycle)
          └── Repository (@Singleton, Hilt-injected)
                 ├── Room Database (v5, 8 tables, 8 DAOs)
                 └── DataStore Preferences (UserPreferences)
```

Key patterns observed:
- **Single Activity** — `MainActivity` hosts one `NavHostController`; all screens are composable destinations.
- **Offline-first** — Room is the single source of truth; no remote sync.
- **Reactive UI** — DAOs return `Flow<List<T>>`; DataStore prefs exposed as `Flow`.
- **Composition Locals** — `LocalStrings` (translation lambda) and `LocalLanguage` propagate i18n throughout the tree.
- **Sealed UiState** — some ViewModels use data classes; AI screens use `var` state + `LaunchedEffect`. This spec introduces a proper sealed `UiState` for the new screen.

### 1.2 Package Structure

```
com.trackfinz.app
├── data/
│   ├── database/   → Room DB, 8 DAOs, TypeConverters
│   ├── datastore/  → UserPreferences (12 keys)
│   ├── model/      → Entities + Enums
│   └── repository/ → 6 repositories
├── di/             → AppModule (Hilt), SeedDataInitializer
├── i18n/           → AppStrings.kt (Str object), Translations.kt (11 languages), LocalStrings
├── navigation/     → NavRoutes, AppNavGraph
├── receiver/       → BillReminderReceiver
├── ui/
│   ├── components/ → Reusable composables (BottomNavBar, BudgetProgressBar, BarChart, etc.)
│   ├── screens/    → Feature screens
│   └── theme/      → Color.kt, Type.kt, Theme.kt
├── utils/          → AI utilities, formatCurrency, etc.
├── MainActivity.kt
└── TrackFinzApp.kt
```


### 1.3 Room Database (v5)

| Table | Key columns |
|---|---|
| `users` | id, name, email, pin, currency |
| `transactions` | id, title, amount, type (INCOME/EXPENSE), category, date (Long epoch ms), note |
| `budgets` | id, category, limit, month, year |
| `budget_history` | id, budgetId, category, oldLimit, newLimit, action, timestamp |
| `goals` | id, title, targetAmount, savedAmount, emoji, deadline, isCompleted |
| `goal_history` | id, goalId, amount, action, balanceBefore, balanceAfter |
| `chat_messages` | id, text, isUser, timestamp |
| `bill_reminders` | id, name, amount, dueDateMillis, reminderTimeMillis, frequency, isActive |

**Enums:** `TransactionType` (INCOME, EXPENSE), `TransactionCategory` (14 values), `BillFrequency`, `BudgetHistoryAction`, `GoalFundAction`

**TransactionCategory expense values:** FOOD, GROCERIES, SHOPPING, TRAVEL, GAS, ENTERTAINMENT, BILLS, HEALTHCARE, OTHER

### 1.4 Existing Repositories and Their Useful Methods

| Repository | Methods relevant to this feature |
|---|---|
| `TransactionRepository` | `allTransactions: Flow`, `getByDateRange(from, to)`, `totalIncomeInRange(from, to)`, `totalExpenseInRange(from, to)`, `spentInCategory(cat, from, to)` |
| `BudgetRepository` | `getForMonth(month, year)`, `getAll()`, `upsert(budget)` |
| `GoalRepository` | `allGoals: Flow` |
| `UserPreferences` | `currency: Flow<String>`, `language: Flow<String>` |

### 1.5 Existing ViewModels

| ViewModel | Relevant state |
|---|---|
| `TransactionViewModel` | `allTransactions: StateFlow<List<TransactionEntity>>`, `totalIncome`, `totalExpense`, `balance` |
| `BudgetViewModel` | `budgetsWithSpent: StateFlow<List<BudgetWithSpent>>`, `applyRecommendedBudgets()` |
| `GoalViewModel` | `goals: StateFlow<List<GoalEntity>>` |
| `SettingsViewModel` | `currency`, `language`, `user` |
| `ChatViewModel` | `allMessages`, `insertMessage()`, `clearHistory()` |


### 1.6 Existing AI Infrastructure

| Utility | SDK / Model | Used for |
|---|---|---|
| `AIFinancialAssistant` | `gemini-2.5-flash` | Chat Q&A with transaction context |
| `AIInsightsGenerator` | `gemini-1.5-flash` | Spending insight cards (w/ rule-based fallback) |
| `AIBudgetRecommender` | `gemini-1.5-flash` | Category budget suggestions (w/ rule-based fallback) |

All three follow the same pattern:
1. Analyze data locally (no network)
2. Build a structured prompt
3. Call Gemini via `GenerativeModel.generateContent()`
4. Parse the response
5. Fall back to rule-based results on API failure or timeout

The `INTERNET` permission is already declared. No new permissions needed.

### 1.7 Navigation Structure

Routes live in `NavRoutes` object; composable destinations registered in `AppNavGraph.kt`.

**Existing routes:** `splash`, `onboarding`, `login`, `register`, `pin_setup`, `pin_lock`, `dashboard`, `transactions`, `add_transaction`, `budget`, `goals`, `analytics`, `profile`, `receipt_scanner`, `ai_insights`, `ai_budget_recommendations`, `ai_assistant`, `bill_reminders`

**Bottom nav routes:** `dashboard`, `transactions`, `budget`, `goals`, `bill_reminders`, `analytics` — the new screen does **not** consume a bottom nav slot.

### 1.8 UI Design System

| Token | Value |
|---|---|
| Primary color | `Teal500` = `#00BCD4` |
| Secondary color | `Emerald500` = `#4CAF50` |
| Income color | `IncomeGreen` = `#00C853` |
| Expense color | `ExpenseRed` = `#FF5252` |
| Warning color | `WarningAmber` = `#FFAB40` |
| Background (light) | `SoftGray100` = `#F5F7FA` |
| Background (dark) | `Navy900` = `#0D1B2A` |
| Surface (dark) | `Navy800` = `#1B2A3B` |

**Common card pattern:** `Card` with `containerColor = Color.Transparent` containing a `Box` with `Brush.horizontalGradient()` background — used in `AIBudgetRecommendationsScreen`, `AIAssistantScreen`, `AIInsightsScreen`, and `DashboardScreen`.

**Typography:** `TrackFinzTypography` extends Material 3; `headlineSmall` (24sp, SemiBold) for section headers; `titleMedium` (16sp, Medium) for card titles; `bodyMedium` (14sp, Normal) for body text.

**i18n:** `LocalStrings.current` returns `(String) -> String`; all UI text goes through `s(Str.KEY)`. 11 languages supported. New keys must be added to `Str` object and `Translations.kt`.

---


## 2. User Stories

### Epic A — Monthly Summary

**US-01 Monthly Summary View**  
As a user, I want to see a complete monthly financial summary so I can understand how I performed without manually reading multiple charts.

> Acceptance: A dedicated "Monthly Report" screen shows income, expenses, net balance, savings rate, top spending categories, budget compliance, and a plain-language AI narrative — all for the selected month.

**US-02 Previous Months**  
As a user, I want to select and view previous months' reports so I can track how my financial habits change over time.

> Acceptance: A month/year selector lets me browse any calendar month that has transaction data.

**US-03 Report Persistence**  
As a user, I want my generated report to persist so it loads instantly when I revisit the screen in the same month.

> Acceptance: A generated report is stored in Room. On re-open within the same month the cached version loads; a "Regenerate" option forces a fresh analysis.

### Epic B — Spending Habit Analysis

**US-04 Top Spending Categories**  
As a user, I want to see which categories I spent the most on and how they compare to the prior month so I can spot changes in my behavior.

> Acceptance: Top 5 expense categories are shown with amount, percentage of total expenses, and a month-over-month delta indicator (▲/▼ with % change).

**US-05 Income vs Expense Analysis**  
As a user, I want to see a clear income vs expense breakdown for the month so I understand my cash flow at a glance.

> Acceptance: A visual summary card shows total income, total expenses, net balance, and a savings rate percentage.

**US-06 Daily Spending Pattern**  
As a user, I want to understand on which days of the week I tend to spend more so I can build better habits.

> Acceptance: A weekday vs weekend spending comparison is included in the report.

### Epic C — Savings Rate

**US-07 Savings Rate Display**  
As a user, I want to see my savings rate for the month so I know if I am on track with my financial goals.

> Acceptance: `savingsRate = (income - expenses) / income * 100` is displayed prominently. A contextual label (Excellent / Good / Fair / Needs Improvement) is shown alongside.

**US-08 Month-over-Month Savings Comparison**  
As a user, I want to know if my savings rate improved or worsened compared to last month so I can measure progress.

> Acceptance: The report shows the current month savings rate and the prior month savings rate with a delta.

### Epic D — Top Spending Categories

**US-09 Category Breakdown with Rank**  
As a user, I want to see my top expense categories ranked by amount so I know where my money actually goes.

> Acceptance: Categories are ranked 1–5 by total spending in the selected month. Each entry shows emoji, label, amount, and % of total.

**US-10 Budget vs Actual per Category**  
As a user, I want to see how each category's actual spending compares to its budget so I know where I overspent.

> Acceptance: For categories with a budget set, a progress indicator shows actual vs budget with an "Over!" badge when exceeded.

### Epic E — Previous Month Comparison

**US-11 Month-over-Month Comparison**  
As a user, I want to compare this month's totals directly with last month's so I can see whether things are improving.

> Acceptance: The report shows income, expenses, and net balance for both the selected month and the prior month side by side.

### Epic F — Financial Health Score

**US-12 Financial Health Score**  
As a user, I want a single score that reflects my overall financial health so I can quickly gauge how I am doing.

> Acceptance: A 0–100 score is computed from savings rate (40%), budget compliance (35%), and goal progress (25%). It is displayed prominently with a label and color.

**US-13 Score Explanation**  
As a user, I want to understand how my score is calculated so I know what to improve.

> Acceptance: Tapping the score shows a breakdown card explaining each component's contribution.

### Epic G — AI Recommendations

**US-14 Personalized Recommendations**  
As a user, I want to receive specific, actionable recommendations based on my actual data so I know exactly what to do next.

> Acceptance: At least 3 recommendation cards are shown when the user has ≥ 5 transactions for the month.

**US-15 One-Tap Budget Application**  
As a user, I want to apply a suggested budget directly from the report screen so I do not have to navigate away.

> Acceptance: Recommendations of type "Set Budget" have an "Apply" button that creates or updates the budget in one tap.

---


## 3. Functional Requirements

### 3.1 Monthly Income Analysis

- Sum all `INCOME` transactions in the selected month using `TransactionRepository.totalIncomeInRange(monthStart, monthEnd)`.
- Compare with prior month income; compute delta amount and percentage.
- Group income by category; show top income sources.

### 3.2 Monthly Expense Analysis

- Sum all `EXPENSE` transactions in the selected month using `TransactionRepository.totalExpenseInRange(monthStart, monthEnd)`.
- Group by `TransactionCategory`; rank categories by total amount descending.
- Show top 5 categories with amount, percentage of total expenses, and MoM delta.
- Compare with prior month expenses.

### 3.3 Savings Rate Calculation

```
savingsRate = if (income > 0) ((income - expenses) / income) * 100 else 0.0
```

Label mapping:
- `≥ 20%` → **Excellent** (Emerald color)
- `10–19%` → **Good** (Teal color)
- `0–9%` → **Fair** (Amber color)
- `< 0%` → **Needs Improvement** (Red color)

### 3.4 Top Spending Categories

- Compute `spentInCategory(cat, monthStart, monthEnd)` for every expense category.
- Sort descending by amount; take top 5.
- Compute prior month amounts for the same categories for delta display.
- Show percentage of total monthly expenses.

### 3.5 Month-over-Month Comparison

Prior month = first day of `(year, month - 1)` to last day of `(year, month - 1)`.  
Handle year boundary (month 1 → year-1, month 12).

Display for both current and prior month:
- Total income
- Total expenses
- Net balance
- Savings rate

### 3.6 Financial Health Score

Computed offline, no network required:

```kotlin
object FinancialHealthScore {
    fun compute(
        savingsRate: Double,         // 0.0–100.0
        budgetsWithSpent: List<BudgetWithSpent>,
        goals: List<GoalEntity>
    ): Int {
        val savingsRateScore = (savingsRate / 20.0).coerceIn(0.0, 1.0) * 100.0
        // 20% savings rate = perfect savings score

        val budgetComplianceScore = if (budgetsWithSpent.isEmpty()) 50.0 else {
            val underBudget = budgetsWithSpent.count { it.spent <= it.budget.limit }
            (underBudget.toDouble() / budgetsWithSpent.size) * 100.0
        }

        val goalProgressScore = if (goals.isEmpty()) 50.0 else {
            goals.map { it.savedAmount / it.targetAmount.coerceAtLeast(1.0) }
                 .average() * 100.0
        }

        val score = (savingsRateScore * 0.40 +
                     budgetComplianceScore * 0.35 +
                     goalProgressScore * 0.25).toInt()
        return score.coerceIn(0, 100)
    }

    fun label(score: Int): HealthLabel = when {
        score >= 80 -> HealthLabel.EXCELLENT
        score >= 60 -> HealthLabel.GOOD
        score >= 40 -> HealthLabel.FAIR
        else        -> HealthLabel.NEEDS_IMPROVEMENT
    }
}
```

### 3.7 AI Narrative Generation

Follows the same pattern as `AIFinancialAssistant.askQuestion()`:

1. Build a prompt string from computed KPIs (aggregates only — no raw transaction titles are sent to protect privacy).
2. Call `GenerativeModel.generateContent(prompt)` with `gemini-1.5-flash`.
3. On success, return the narrative string.
4. On failure (network error, 503, timeout), return a template narrative assembled from the KPI values.
5. Store the narrative in Room (`MonthlyReportEntity`).

**Template fallback (offline/error):**  
> "In [Month Year], your total income was [X] and expenses were [Y], giving you a net [balance/deficit] of [Z] and a savings rate of [W]%. Your top spending category was [cat] ([amount]). [Budget compliance sentence]. [Goal progress sentence]."

### 3.8 Personalized Recommendations

Rule engine runs entirely offline. Returns up to 6 `MonthlyRecommendation` objects.

| Rule | Trigger | Recommendation type |
|---|---|---|
| R01 | Category spending ≥ 20% above prior month | TREND_ALERT |
| R02 | Category spending > budget limit | OVER_BUDGET |
| R03 | Category with ≥ 3 transactions and no budget set | MISSING_BUDGET |
| R04 | Savings rate < 10% | LOW_SAVINGS |
| R05 | Goal deadline within 60 days and savedAmount < 50% of target | GOAL_AT_RISK |
| R06 | Savings rate ≥ 25% for the month | POSITIVE_HABIT |
| R07 | No transactions in the last 7 days | ENGAGEMENT |

Rules are evaluated in priority order: R02 → R05 → R01 → R03 → R04 → R07 → R06.

---


## 4. Technical Requirements

### 4.1 Follow Existing MVVM Architecture

```
MonthlyReportScreen (Compose)
    └── MonthlyReportViewModel (@HiltViewModel)
            ├── TransactionRepository  (existing, injected)
            ├── BudgetRepository       (existing, injected)
            ├── GoalRepository         (existing, injected)
            ├── MonthlyReportRepository  (NEW)
            ├── UserPreferences          (existing, injected via SettingsViewModel pattern)
            └── BudgetViewModel.applyRecommendedBudgets()  (reused for one-tap budget apply)
```

### 4.2 ViewModel UiState Design

```kotlin
// ui/screens/report/MonthlyReportUiState.kt

data class MonthlyKpi(
    val income: Double,
    val expenses: Double,
    val netBalance: Double,
    val savingsRate: Double,
    val priorIncome: Double,
    val priorExpenses: Double,
    val priorSavingsRate: Double
)

data class CategorySummary(
    val category: TransactionCategory,
    val amount: Double,
    val percentage: Double,
    val priorAmount: Double,
    val deltaPercent: Double,
    val budgetLimit: Double?,
    val budgetUsedPercent: Double?
)

data class MonthlyRecommendation(
    val ruleId: String,
    val type: RecommendationType,
    val title: String,
    val description: String,
    val actionLabel: String?,
    val actionData: String?    // e.g. category name for budget apply
)

enum class RecommendationType {
    TREND_ALERT, OVER_BUDGET, MISSING_BUDGET, LOW_SAVINGS,
    GOAL_AT_RISK, POSITIVE_HABIT, ENGAGEMENT
}

enum class HealthLabel { EXCELLENT, GOOD, FAIR, NEEDS_IMPROVEMENT }

sealed class MonthlyReportUiState {
    object Loading : MonthlyReportUiState()

    data class Ready(
        val selectedMonth: Int,
        val selectedYear: Int,
        val kpi: MonthlyKpi,
        val healthScore: Int,
        val healthLabel: HealthLabel,
        val topCategories: List<CategorySummary>,
        val budgetCompliance: List<CategorySummary>,
        val goalProgress: List<GoalEntity>,
        val recommendations: List<MonthlyRecommendation>,
        val narrative: String,
        val narrativeState: NarrativeState,
        val currency: String
    ) : MonthlyReportUiState()

    data class Error(val message: String) : MonthlyReportUiState()
}

enum class NarrativeState { LOADING, DONE, FALLBACK, ERROR }
```

### 4.3 ViewModel Methods

```kotlin
@HiltViewModel
class MonthlyReportViewModel @Inject constructor(
    private val txRepo: TransactionRepository,
    private val budgetRepo: BudgetRepository,
    private val goalRepo: GoalRepository,
    private val reportRepo: MonthlyReportRepository,
    private val prefs: UserPreferences
) : ViewModel() {

    private val _month = MutableStateFlow(currentMonth())
    private val _year  = MutableStateFlow(currentYear())
    val uiState: StateFlow<MonthlyReportUiState> = ...

    fun selectMonth(month: Int, year: Int) { ... }
    fun generateReport() { ... }    // calls AI; updates narrative
    fun regenerateReport() { ... }  // forces fresh AI call even if cached
    fun applyBudgetFromRecommendation(category: TransactionCategory, amount: Double) { ... }
}
```

### 4.4 Gemini Integration

Reuse the `gemini-1.5-flash` model following the exact pattern in `AIBudgetRecommender`:

```kotlin
private val generativeModel by lazy {
    GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.6f
            topK = 40
            topP = 0.95f
            maxOutputTokens = 600
        }
    )
}
```

Prompt strategy — send only aggregated KPIs (never raw transaction titles):

```
"You are a personal finance advisor writing a monthly summary.

DATA FOR [Month Year]:
- Total income: $X
- Total expenses: $Y
- Net balance: $Z
- Savings rate: W%
- Prior month savings rate: V%
- Top 3 expense categories: [Food $A, Groceries $B, Shopping $C]
- Budgets: X categories under budget, Y over budget
- Goals: X active goals, Y% average progress

Write a warm, encouraging 3-paragraph summary in [language name]:
Paragraph 1: Overall performance for the month.
Paragraph 2: What the user did well (savings, budget compliance, or goal progress).
Paragraph 3: One specific, actionable improvement tip.
Keep the total response under 120 words."
```

Retry logic: 2 attempts with 2-second delay between, matching `AIBudgetRecommender.generateBudgetRecommendations()`.

### 4.5 Date Range Utilities

Reuse existing utility functions already in the project:

```kotlin
// Already exist in utils package:
fun currentMonth(): Int
fun currentYear(): Int
fun monthStartMillis(month: Int, year: Int): Long
fun monthEndMillis(month: Int, year: Int): Long
```

Prior month calculation:

```kotlin
fun priorMonth(month: Int, year: Int): Pair<Int, Int> =
    if (month == 1) Pair(12, year - 1) else Pair(month - 1, year)
```

### 4.6 Navigation

Add to `NavRoutes`:

```kotlin
const val MONTHLY_REPORT = "monthly_report"
```

Add composable destination in `AppNavGraph.kt`:

```kotlin
composable(NavRoutes.MONTHLY_REPORT) {
    MonthlyReportScreen(
        onBack = { navController.popBackStack() },
        onNavigateToBudget = {
            navController.navigate(NavRoutes.BUDGET) {
                popUpTo(NavRoutes.MONTHLY_REPORT) { inclusive = true }
            }
        },
        onNavigateToGoals = {
            navController.navigate(NavRoutes.GOALS) {
                popUpTo(NavRoutes.MONTHLY_REPORT) { inclusive = true }
            }
        }
    )
}
```

Entry points:
- Dashboard → new banner card (replaces the existing AI Insights banner)
- Dashboard → Quick Action "Report" button (the `Str.REPORT` string already exists)

---


## 5. Data Layer Changes

### 5.1 New Entities

No new entities are strictly required if the narrative is regenerated on every visit. However, for performance and offline use, one new entity is recommended:

```kotlin
// data/model/MonthlyReportEntity.kt

@Entity(tableName = "monthly_reports")
data class MonthlyReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val month: Int,                // 1–12
    val year: Int,
    val narrative: String,         // AI-generated or template fallback
    val kpiJson: String,           // JSON snapshot: income, expenses, savingsRate, topCategories
    val healthScore: Int,          // 0–100 at time of generation
    val isFallback: Boolean = false, // true when generated offline
    val generatedAt: Long = System.currentTimeMillis()
)
```

### 5.2 New DAO

```kotlin
// data/database/MonthlyReportDao.kt

@Dao
interface MonthlyReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: MonthlyReportEntity): Long

    @Query("SELECT * FROM monthly_reports WHERE month = :month AND year = :year LIMIT 1")
    suspend fun getForMonth(month: Int, year: Int): MonthlyReportEntity?

    @Query("SELECT * FROM monthly_reports ORDER BY year DESC, month DESC")
    fun getAllFlow(): Flow<List<MonthlyReportEntity>>

    @Query("DELETE FROM monthly_reports WHERE year < :year OR (year = :year AND month < :month)")
    suspend fun deleteOlderThan(month: Int, year: Int)
}
```

### 5.3 New Repository

```kotlin
// data/repository/MonthlyReportRepository.kt

@Singleton
class MonthlyReportRepository @Inject constructor(
    private val dao: MonthlyReportDao
) {
    suspend fun getReportForMonth(month: Int, year: Int): MonthlyReportEntity? =
        dao.getForMonth(month, year)

    suspend fun saveReport(report: MonthlyReportEntity) = dao.insert(report)

    fun allReportsFlow(): Flow<List<MonthlyReportEntity>> = dao.getAllFlow()

    suspend fun pruneOld(keepMonths: Int = 12) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -keepMonths)
        val month = cal.get(Calendar.MONTH) + 1
        val year  = cal.get(Calendar.YEAR)
        dao.deleteOlderThan(month, year)
    }
}
```

### 5.4 Database Migration (v5 → v6)

```kotlin
// In di/AppModule.kt — new migration

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `monthly_reports` (
                `id`          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `month`       INTEGER NOT NULL,
                `year`        INTEGER NOT NULL,
                `narrative`   TEXT NOT NULL,
                `kpiJson`     TEXT NOT NULL,
                `healthScore` INTEGER NOT NULL,
                `isFallback`  INTEGER NOT NULL DEFAULT 0,
                `generatedAt` INTEGER NOT NULL
            )
        """.trimIndent())
    }
}
```

Register the migration in `AppModule.provideDatabase()` alongside existing migrations.

Add entity to `TrackFinzDatabase`:
```kotlin
@Database(
    entities = [
        UserEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        BudgetHistoryEntity::class,
        GoalEntity::class,
        GoalHistoryEntity::class,
        ChatMessageEntity::class,
        BillReminderEntity::class,
        MonthlyReportEntity::class   // NEW
    ],
    version = 6,   // bumped from 5
    exportSchema = false
)
```

Add DAO provider in `AppModule`:
```kotlin
@Provides fun provideMonthlyReportDao(db: TrackFinzDatabase): MonthlyReportDao = db.monthlyReportDao()
```

---


## 6. UI Design Specification

### 6.1 Entry Points on Dashboard

**Option A — Replace the existing AI Insights banner** with a "Monthly Report" banner:

```
┌─────────────────────────────────────────────────────────┐
│  [gradient: Deep Purple → Indigo]                       │
│  📊  Monthly Report                          [→]        │
│  July 2026 · Health Score: 74  [GOOD badge]             │
│  3 recommendations ready                                 │
└─────────────────────────────────────────────────────────┘
```

Gradient colors: `Color(0xFF6A1B9A)` → `Color(0xFF283593)` (purple-to-indigo — distinct from existing AI blue/teal banners).

**Option B — Add a Quick Action card** for "Report" (the `Str.REPORT` key and `Icons.Default.BarChart` icon already exist in the project).

Both options keep the existing AI Assistant banner untouched.

### 6.2 MonthlyReportScreen Layout

```
┌─────────────────────────────────────────────────────────┐
│  ←  Monthly Report                  [Month picker] [⋮]  │
│     TopAppBar, containerColor = Color(0xFF6A1B9A)       │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  LazyColumn content:                                    │
│                                                         │
│  1. Health Score Card (gradient header)                 │
│  2. KPI Summary Card (income / expenses / net)          │
│  3. Savings Rate Card                                   │
│  4. Month Comparison Card                               │
│  5. Top Categories Section                              │
│  6. Budget Performance Section                          │
│  7. Goal Progress Section                               │
│  8. AI Narrative Card                                   │
│  9. Recommendations Section                             │
│  10. Footer info note                                   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 6.3 Section Designs

#### Health Score Card (gradient header)

```
┌──────────────────────────────────────────────────────┐
│  [Brush.horizontalGradient(Purple → Indigo)]         │
│                                                      │
│  📊  July 2026 Financial Report                      │
│      "Your monthly AI-powered analysis"              │
│                                                      │
│          ┌──────────────────────┐                   │
│          │  HEALTH SCORE        │                   │
│          │        74            │                   │
│          │    ████████░░        │                   │
│          │   [GOOD badge]       │                   │
│          └──────────────────────┘                   │
│                                                      │
│  Savings 40% · Budget 35% · Goals 25%  [?]          │
└──────────────────────────────────────────────────────┘
```

- Score displayed as large number (MaterialTheme `headlineLarge`, ExtraBold, white).
- Progress bar: `LinearProgressIndicator` (fraction = score/100), color by label.
- Badge: `Surface` with `RoundedCornerShape(50%)`, color: EXCELLENT=Emerald, GOOD=Teal, FAIR=Amber, NEEDS_IMPROVEMENT=ExpenseRed.
- Tapping `[?]` shows a `BottomSheet` explaining score components.

#### KPI Summary Card

```
┌──────────────────────────────────────────────────────┐
│  Income vs Expenses                                  │
│                                                      │
│  ┌───────────────┐  ┌───────────────┐               │
│  │  ↑ Income     │  │  ↓ Expenses   │               │
│  │  $2,400       │  │  $1,680       │               │
│  │  +5% MoM ▲   │  │  -8% MoM ▼   │               │
│  └───────────────┘  └───────────────┘               │
│                                                      │
│  Net Balance: +$720                                  │
└──────────────────────────────────────────────────────┘
```

- Income chip: `IncomeGreen` accent; Expense chip: `ExpenseRed` accent.
- MoM delta: green if improved (income up / expense down), red otherwise.
- Uses `MaterialTheme.colorScheme.surface` background, `elevation = 2.dp`.

#### Savings Rate Card

```
┌──────────────────────────────────────────────────────┐
│  💰 Savings Rate                                     │
│                                                      │
│         30%                                          │
│    ████████████░░░░░░  (target: 20%)                │
│                                                      │
│    [  GOOD  ]   Prior month: 22%  ▲ +8%             │
└──────────────────────────────────────────────────────┘
```

- `LinearProgressIndicator` showing current savings rate vs 20% (full = 100%).
- Label badge same as health score badge color logic.
- Prior month comparison with delta.

#### Top Categories Section

```
Top Spending Categories                         (July 2026)

  1.  🍔  Food          $420   ▲ 35%   ████████░░  25%
      [Over budget by $120]  ⚠️

  2.  🛒  Groceries     $280   ▼ 5%    ██████░░░░  17%
      [Under budget]  ✅

  3.  🛍  Shopping      $190   ▲ 12%   ████░░░░░░  11%
      [No budget set]

  4.  💡  Bills         $180   ─        ████░░░░░░  11%
      [Under budget]  ✅

  5.  🎬  Entertainment $120   ▼ 15%   ██░░░░░░░░   7%
      [Under budget]  ✅
```

Each row is a `Card` with:
- Category emoji + label + amount (bold) + MoM delta chip.
- `LinearProgressIndicator` (fraction = categoryAmount / totalExpenses).
- Percentage text aligned end.
- Budget status chip: "Over budget" (red), "Under budget" (green), "No budget" (gray).

#### Month Comparison Card

```
┌──────────────────────────────────────────────────────┐
│  📅 Month Comparison                                 │
│                                                      │
│              July 2026    June 2026                  │
│  Income       $2,400       $2,280    ▲ +5%           │
│  Expenses     $1,680       $1,824    ▼ -8%           │
│  Net Balance    $720         $456    ▲ +58%           │
│  Savings Rate    30%          20%   ▲ +10pp           │
└──────────────────────────────────────────────────────┘
```

Simple table layout using `Row` + `Column`. Positive deltas green, negative red.

#### AI Narrative Card

```
┌──────────────────────────────────────────────────────┐
│  🤖 AI Financial Analysis                    [Share] │
│                                                      │
│  "July was a strong month for your finances.         │
│  Your savings rate of 30% is well above the          │
│  recommended 20% target. You successfully kept       │
│                                                      │
│  [Show more ▼ / Show less ▲]                        │
│                                                      │
│  ──────────────────────────────────────              │
│  ℹ Powered by Google Gemini                          │
└──────────────────────────────────────────────────────┘
```

- Background: `MaterialTheme.colorScheme.surfaceVariant`.
- Collapsible text (expand/collapse button).
- Share icon calls `android.content.Intent.ACTION_SEND` with narrative text.
- When `isFallback = true`, show: "Generated offline · Reconnect for a personalized summary".
- Loading state: `CircularProgressIndicator` + "Generating your analysis…" text (same as `AIInsightsScreen`).

#### Recommendations Section

```
💡 Recommendations

  ┌─────────────────────────────────────────────────┐
  │  ⚠️  Food Spending Up 35%                    [✕] │
  │  Food spending this month ($420) is 35% higher  │
  │  than last month ($312). Review your dining      │
  │  habits to get back on track.                   │
  │  [ View in Analytics ]                           │
  └─────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────┐
  │  💡  No Budget for Shopping               [✕]   │
  │  You spend ~$190/month on Shopping but have     │
  │  no budget. Setting one helps you stay on       │
  │  track each month.                              │
  │  [ Set Budget: $190 ]                            │
  └─────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────┐
  │  ✨  Excellent Savings Rate               [✕]   │
  │  Your 30% savings rate is outstanding. Keep     │
  │  it up and you'll hit your financial goals      │
  │  ahead of schedule!                             │
  └─────────────────────────────────────────────────┘
```

Each card:
- Border color by type: TREND_ALERT/OVER_BUDGET = red, MISSING_BUDGET = Amber, POSITIVE_HABIT = Emerald, others = Teal.
- Dismiss `[✕]` removes the card with `AnimatedVisibility` exit.
- Action button = `OutlinedButton` (if navigation) or `Button` with `Teal500` (if inline action like "Set Budget").

#### Footer

```
ℹ Analysis covers July 1–31, 2026  ·  Powered by AI
```

### 6.4 Month Selector

Use an existing `DatePickerDialog`-style month selector matching the pattern in `BudgetScreen.kt`.
Display: `"< July 2026 >"` in the TopAppBar actions area, or as an overflow menu item.

### 6.5 Empty State (< 5 transactions)

```
        📊

  Not enough data yet

  Add at least 5 transactions in July 2026
  to generate your financial report.

  [ Add Transaction ]
```

### 6.6 Loading State

```
        ⬤ ⬤ ⬤   (CircularProgressIndicator)

  Analyzing your finances for July 2026...
```

Same pattern as `AIInsightsScreen` and `AIBudgetRecommendationsScreen`.

### 6.7 Error State

```
  ┌──────────────────────────────────────────────┐
  │  ⛔ Failed to generate report                │
  │  Check your internet connection and retry.   │
  │  [ Try Again ]                               │
  └──────────────────────────────────────────────┘
```

Same pattern as `errorContainer` card used in `AIBudgetRecommendationsScreen`.

---


## 7. File Changes Required

### 7.1 New Files

| File path | Purpose |
|---|---|
| `data/model/MonthlyReportEntity.kt` | Room entity for persisted monthly reports |
| `data/database/MonthlyReportDao.kt` | DAO for monthly report persistence |
| `data/repository/MonthlyReportRepository.kt` | Repository wrapping the new DAO |
| `utils/FinancialHealthScore.kt` | Offline health score computation |
| `utils/MonthlyReportGenerator.kt` | KPI analysis + Gemini narrative call + fallback |
| `utils/MonthlyRecommendationEngine.kt` | Rule-based recommendation logic (7 rules) |
| `ui/screens/report/MonthlyReportScreen.kt` | Main screen composable (LazyColumn + all sections) |
| `ui/screens/report/MonthlyReportViewModel.kt` | ViewModel with sealed UiState |
| `ui/screens/report/MonthlyReportUiState.kt` | Data classes: MonthlyKpi, CategorySummary, MonthlyRecommendation, etc. |
| `ui/components/HealthScoreBadge.kt` | Reusable health score pill composable |
| `ui/components/MonthlyReportEntryCard.kt` | Dashboard entry banner card |

### 7.2 Modified Files

| File | Change required |
|---|---|
| `data/database/TrackFinzDatabase.kt` | Add `MonthlyReportEntity` to `entities`; add `monthlyReportDao()`; bump version to 6 |
| `di/AppModule.kt` | Add `MIGRATION_5_6`; add `@Provides fun provideMonthlyReportDao()`; add `MonthlyReportRepository` binding |
| `navigation/NavRoutes.kt` | Add `const val MONTHLY_REPORT = "monthly_report"` |
| `navigation/AppNavGraph.kt` | Add `composable(NavRoutes.MONTHLY_REPORT)` destination |
| `ui/screens/dashboard/DashboardScreen.kt` | Replace AI Insights banner (or add as additional quick action) with `MonthlyReportEntryCard` |
| `i18n/AppStrings.kt` | Add new `Str` constants (see §7.3) |
| `i18n/Translations.kt` | Add English translations; add Khmer translations; add remaining 9 languages |

**No changes needed to:**
- `ChatRepository`, `ChatViewModel`, `ChatMessageEntity` (not touched)
- `AIFinancialAssistant` (reused as-is for narrative generation)
- `TransactionRepository`, `BudgetRepository`, `GoalRepository` (reused as-is)
- `BudgetViewModel.applyRecommendedBudgets()` (called directly from MonthlyReportViewModel)
- Existing AI screens (`AIInsightsScreen`, `AIBudgetRecommendationsScreen`, `AIAssistantScreen`) — left intact

### 7.3 New i18n Keys

Add these constants to `Str` object in `AppStrings.kt`:

```kotlin
// Monthly Report Screen
const val MONTHLY_REPORT_TITLE        = "monthly_report_title"
const val MONTHLY_REPORT_SUBTITLE     = "monthly_report_subtitle"
const val MONTHLY_REPORT_ENTRY_TITLE  = "monthly_report_entry_title"
const val MONTHLY_REPORT_ENTRY_DESC   = "monthly_report_entry_desc"
const val GENERATING_REPORT           = "generating_report"
const val REGENERATE_REPORT           = "regenerate_report"
const val REPORT_ERROR                = "report_error"
const val REPORT_NOT_ENOUGH_DATA      = "report_not_enough_data"

// Health Score
const val HEALTH_SCORE                = "health_score"
const val HEALTH_SCORE_EXCELLENT      = "health_score_excellent"
const val HEALTH_SCORE_GOOD           = "health_score_good"
const val HEALTH_SCORE_FAIR           = "health_score_fair"
const val HEALTH_SCORE_NEEDS_WORK     = "health_score_needs_work"
const val SCORE_BREAKDOWN_SAVINGS     = "score_breakdown_savings"
const val SCORE_BREAKDOWN_BUDGET      = "score_breakdown_budget"
const val SCORE_BREAKDOWN_GOALS       = "score_breakdown_goals"

// KPI Section
const val INCOME_VS_EXPENSES_TITLE    = "income_vs_expenses_title"
const val NET_BALANCE_LABEL           = "net_balance_label"
const val MONTH_COMPARISON_TITLE      = "month_comparison_title"
const val PRIOR_MONTH_LABEL           = "prior_month_label"
const val SAVINGS_RATE_LABEL          = "savings_rate_label"
const val SAVINGS_TARGET_LABEL        = "savings_target_label"

// Categories Section
const val TOP_CATEGORIES_TITLE        = "top_categories_title"
const val NO_BUDGET_SET               = "no_budget_set"
const val OVER_BUDGET_BY              = "over_budget_by"       // "Over by %s"
const val UNDER_BUDGET_LABEL          = "under_budget_label"
const val BUDGET_COMPLIANCE_TITLE     = "budget_compliance_title"
const val CATEGORIES_UNDER_BUDGET     = "categories_under_budget"   // "%d under budget"
const val CATEGORIES_OVER_BUDGET      = "categories_over_budget"    // "%d over budget"

// Goal Progress
const val GOAL_PROGRESS_TITLE         = "goal_progress_title"
const val GOAL_AT_RISK_LABEL          = "goal_at_risk_label"
const val GOAL_ON_TRACK_LABEL         = "goal_on_track_label"

// AI Narrative
const val AI_NARRATIVE_TITLE          = "ai_narrative_title"
const val SHARE_REPORT                = "share_report"
const val OFFLINE_NARRATIVE_NOTE      = "offline_narrative_note"
const val POWERED_BY_GEMINI           = "powered_by_gemini"

// Recommendations
const val RECOMMENDATIONS_TITLE       = "recommendations_title"
const val NO_RECOMMENDATIONS_YET      = "no_recommendations_yet"
const val NO_RECOMMENDATIONS_DESC     = "no_recommendations_desc"
const val SET_BUDGET_ACTION           = "set_budget_action"      // "Set Budget: %s"
const val VIEW_IN_ANALYTICS_ACTION    = "view_in_analytics_action"
const val GO_TO_GOALS_ACTION          = "go_to_goals_action"
const val REC_TREND_ALERT_TITLE       = "rec_trend_alert_title"  // "%s up %d%%"
const val REC_OVER_BUDGET_TITLE       = "rec_over_budget_title"
const val REC_MISSING_BUDGET_TITLE    = "rec_missing_budget_title"
const val REC_LOW_SAVINGS_TITLE       = "rec_low_savings_title"
const val REC_GOAL_AT_RISK_TITLE      = "rec_goal_at_risk_title"
const val REC_POSITIVE_HABIT_TITLE    = "rec_positive_habit_title"
const val REC_ENGAGEMENT_TITLE        = "rec_engagement_title"
```

Add English translations for all keys above to `Translations.kt` in the `en` map.  
Add Khmer translations in the `kh` map.  
Remaining 9 languages (`fr`, `vi`, `lo`, `zh`, `ja`, `ko`, `my`, `ms`, `id`) use English fallback until translated.

---


## 8. Development Plan

Effort scale: XS < 2h · S = 2–4h · M = 4–8h · L = 1–2d

### Phase 1 — Data Layer (Day 1)

| Task | Effort | Notes |
|---|---|---|
| Create `MonthlyReportEntity` | XS | Simple data class with 8 fields |
| Create `MonthlyReportDao` | S | 4 queries |
| Create `MonthlyReportRepository` | S | Thin wrapper |
| Write `MIGRATION_5_6` | XS | Single `CREATE TABLE` |
| Register entity + DAO in `TrackFinzDatabase`; bump to v6 | XS | |
| Add DAO provider in `AppModule` | XS | |

**Checkpoint:** App builds, migrates cleanly. No UI changes yet.

### Phase 2 — Core Logic Utilities (Day 2–3)

| Task | Effort | Notes |
|---|---|---|
| Implement `FinancialHealthScore.compute()` | S | Pure function, easily testable |
| Implement `MonthlyReportGenerator` — KPI analysis offline (no Gemini) | M | Use existing `spentInCategory`, `totalIncomeInRange` etc. |
| Add Gemini narrative call to `MonthlyReportGenerator` | S | Copy pattern from `AIBudgetRecommender` |
| Implement offline fallback narrative template | S | String interpolation using KPI values |
| Implement `MonthlyRecommendationEngine` — all 7 rules | L | Complex logic, test each rule |
| Implement `priorMonth()` date utility function | XS | Edge case: January → December prior year |

**Checkpoint:** Logic utilities return correct results on sample data. No network calls needed for KPIs.

### Phase 3 — ViewModel (Day 4)

| Task | Effort | Notes |
|---|---|---|
| Define sealed `MonthlyReportUiState` + data classes | S | `MonthlyKpi`, `CategorySummary`, `MonthlyRecommendation` |
| Create `MonthlyReportViewModel` with Hilt injection | M | Wire 4 repositories + UserPreferences |
| Implement `loadData(month, year)` method | M | Combines all repo calls, computes score + recs |
| Implement `generateReport()` — calls `MonthlyReportGenerator` | S | Updates `narrativeState` in UiState |
| Implement `applyBudgetFromRecommendation()` | S | Delegates to `budgetRepo.upsert()` |
| Implement `selectMonth(month, year)` | S | Re-triggers data load |

**Checkpoint:** ViewModel logs correct UiState in Logcat on a test device with seed data.

### Phase 4 — Navigation + Entry Card (Day 5)

| Task | Effort | Notes |
|---|---|---|
| Add `NavRoutes.MONTHLY_REPORT` | XS | |
| Add `composable()` destination in `AppNavGraph` | S | 3 callback parameters |
| Create `MonthlyReportEntryCard` composable | S | Purple gradient, health score, entry CTA |
| Create `HealthScoreBadge` composable | XS | Reusable colored pill |
| Update `DashboardScreen` to show entry card | S | Replace or add to existing quick actions |

**Checkpoint:** Tapping the Dashboard card navigates to a blank Companion screen. Back button works.

### Phase 5 — UI: Report Screen (Day 6–9)

| Task | Effort | Notes |
|---|---|---|
| `MonthlyReportScreen` scaffold + `TopAppBar` + month selector | M | |
| Health Score Card section | M | Gradient + score + progress bar + badge |
| KPI Summary Card section | M | Income/expense chips + net balance |
| Savings Rate Card section | S | Progress bar + label badge + prior month |
| Month Comparison Card section | S | Table-style Row layout |
| Top Categories Section | L | 5 category rows + budget progress + MoM delta chips |
| Budget Compliance Section | S | Summary count cards (under/over) |
| Goal Progress Section | M | Goal cards with progress rings (reuse pattern from GoalsScreen) |
| AI Narrative Card section | M | Collapsible text + share button + loading/fallback states |
| Recommendations Section | L | 7 card types + dismiss animation + inline budget apply |
| Empty state | S | |
| Loading state | S | Reuse `CircularProgressIndicator` pattern |
| Error state | S | Reuse `errorContainer` card pattern |
| Footer note | XS | |

**Checkpoint:** All sections render correctly with seed data. Month picker changes the data.

### Phase 6 — i18n (Day 10)

| Task | Effort | Notes |
|---|---|---|
| Add `Str` constants to `AppStrings.kt` | S | ~35 new keys |
| Add English translations to `Translations.kt` | S | |
| Add Khmer translations | M | |
| Add remaining 9 languages (copy English, mark for translation) | S | Fallback keeps app from crashing |

**Checkpoint:** All new text visible in English and Khmer. No hardcoded strings in composables.

### Phase 7 — Polish & QA (Day 11–12)

| Task | Effort | Notes |
|---|---|---|
| Dark mode pass — all new cards and colors | S | |
| Empty state verification (< 5 transactions) | XS | |
| Offline test — disable wifi, verify fallback narrative appears | S | |
| Migration test — fresh install with existing DB v5 data | S | |
| Month picker edge cases (January → previous year) | S | |
| Screen with many recommendations (> 6) — verify max 6 shown | XS | |
| Budget apply test — tap "Set Budget", verify budget created in BudgetScreen | S | |
| Share report test — verify share sheet opens with correct text | S | |
| Manual QA for all 11 languages (text overflow, wrapping) | M | |

**Checkpoint:** Feature is production-ready.

### Summary Timeline

| Day | Milestone |
|---|---|
| 1 | Data layer complete — app migrates cleanly |
| 2–3 | Logic utilities + KPI analysis working |
| 4 | ViewModel with correct state transitions |
| 5 | Navigation + Dashboard entry card |
| 6–9 | All UI sections implemented |
| 10 | Full i18n (EN + KH) |
| 11–12 | Polish, dark mode, offline, QA |
| **Total** | **~12 working days** |

---


## 9. Testing Checklist

### 9.1 Data Layer Tests

- [ ] `MonthlyReportDao.insert()` persists a report correctly
- [ ] `MonthlyReportDao.getForMonth()` returns the correct report or null
- [ ] `MonthlyReportDao.getAllFlow()` returns reports sorted newest first
- [ ] `MonthlyReportDao.deleteOlderThan()` removes only old records
- [ ] `MIGRATION_5_6` executes without SQL errors on a device running DB v5
- [ ] App does not crash on first launch after migration from v5 to v6
- [ ] Existing transaction, budget, goal, and bill data is fully intact after migration

### 9.2 FinancialHealthScore Tests

- [ ] Score = 50 when no transactions, no budgets, no goals (neutral default)
- [ ] Score = 100 when savings rate ≥ 20%, all categories under budget, all goals 100% funded
- [ ] Score = 0 when savings rate = 0%, all categories over budget, all goals 0% funded
- [ ] `label(80..100)` → EXCELLENT
- [ ] `label(60..79)` → GOOD
- [ ] `label(40..59)` → FAIR
- [ ] `label(0..39)` → NEEDS_IMPROVEMENT
- [ ] Score clamped to 0–100 (no negative or > 100 values)

### 9.3 MonthlyReportGenerator Tests

- [ ] Income = sum of all `INCOME` transactions in the month (verified with 5 sample transactions)
- [ ] Expenses = sum of all `EXPENSE` transactions in the month
- [ ] Savings rate computed correctly: `(income - expenses) / income * 100`
- [ ] Savings rate = 0 when income = 0 (no division by zero crash)
- [ ] Top categories ranked by descending amount
- [ ] Prior month date range is correct for January (should be December of prior year)
- [ ] MoM delta = 0 when prior month has no transactions (no crash)
- [ ] Gemini narrative is called once per report generation
- [ ] When Gemini throws an exception, fallback narrative is returned and `isFallback = true`
- [ ] Fallback narrative contains income, expenses, and top category values
- [ ] Generated report is persisted in Room after successful generation
- [ ] On re-open in the same month, the cached report is loaded (Gemini is NOT called again)
- [ ] "Regenerate" forces a fresh Gemini call even if a cached report exists

### 9.4 MonthlyRecommendationEngine Tests

- [ ] R01 TREND_ALERT fires when category spending is ≥ 20% above prior month
- [ ] R01 does NOT fire when category spending is < 20% above prior month
- [ ] R01 does NOT fire when prior month has no data for that category
- [ ] R02 OVER_BUDGET fires when actual spending > budget limit for a category
- [ ] R03 MISSING_BUDGET fires when category has ≥ 3 transactions and no budget
- [ ] R03 does NOT fire when a budget already exists for that category
- [ ] R04 LOW_SAVINGS fires when savings rate < 10%
- [ ] R05 GOAL_AT_RISK fires when deadline within 60 days AND savedAmount < 50% of target
- [ ] R05 does NOT fire when goal has no deadline
- [ ] R06 POSITIVE_HABIT fires when savings rate ≥ 25%
- [ ] R07 ENGAGEMENT fires when no transactions in last 7 days
- [ ] Maximum 6 recommendations returned (excess rules are dropped in priority order)
- [ ] Rules evaluated in correct priority order: R02 → R05 → R01 → R03 → R04 → R07 → R06

### 9.5 ViewModel Tests

- [ ] Initial state is `MonthlyReportUiState.Loading`
- [ ] State transitions to `Ready` after successful data load
- [ ] `selectedMonth` and `selectedYear` default to current month/year
- [ ] `selectMonth()` triggers a fresh data load
- [ ] `narrativeState` transitions: LOADING → DONE on Gemini success
- [ ] `narrativeState` transitions: LOADING → FALLBACK on Gemini failure
- [ ] `applyBudgetFromRecommendation()` calls `budgetRepo.upsert()` with correct category and amount
- [ ] `UserPreferences.currency` is reflected in all amount display strings

### 9.6 UI / Integration Tests

- [ ] **Happy path:** Screen loads with real data and shows all 10 sections
- [ ] **Empty state:** < 5 transactions → empty state illustration shown, no report sections rendered
- [ ] **Loading state:** `CircularProgressIndicator` visible while `narrativeState == LOADING`
- [ ] **Error state:** Error card with retry button shown when both Gemini and data load fail
- [ ] **Month picker:** Changing month to prior month loads prior month data correctly
- [ ] **January edge case:** Selecting January 2026 computes prior month as December 2025
- [ ] **Category row:** "Over budget" badge shown on correct categories
- [ ] **Category row:** "No budget set" chip shown for categories with no budget
- [ ] **Recommendation dismiss:** Tapping ✕ on a recommendation removes it from the list with animation
- [ ] **Set Budget action:** Tapping "Set Budget: $190" creates the budget and updates the BudgetScreen
- [ ] **Share report:** Share button opens Android share sheet with narrative text + KPI summary
- [ ] **Dashboard entry card:** Tapping the card navigates to MonthlyReportScreen
- [ ] **Back navigation:** Back button from MonthlyReportScreen returns to Dashboard

### 9.7 Dark Mode Tests

- [ ] Health Score gradient card renders correctly in dark mode
- [ ] KPI chips (`IncomeGreen`, `ExpenseRed`) remain readable against `Navy800` surface
- [ ] Health Score badge colors readable in dark mode
- [ ] AI Narrative card uses `surfaceVariant` correctly in both themes
- [ ] Recommendation card borders visible in dark mode

### 9.8 Offline / Network Tests

- [ ] With wifi disabled: KPI sections, health score, and recommendations all render correctly
- [ ] With wifi disabled: AI narrative fallback text is shown
- [ ] With wifi disabled: "Generated offline" info note is visible below the narrative
- [ ] With wifi disabled: No crash or unhandled exception
- [ ] Reconnecting to wifi and tapping "Regenerate" triggers a fresh Gemini call

### 9.9 i18n Tests

- [ ] All new text strings are accessed via `s(Str.KEY)` — no hardcoded English strings in composables
- [ ] English: all new text displays correctly with no `???` fallback keys
- [ ] Khmer: all new text displays correctly with Khmer script
- [ ] French, Vietnamese, other languages: English fallback used without any crash
- [ ] Long translated strings (Khmer, Japanese) do not overflow or clip in cards
- [ ] Month names in the month picker use `translatedFullMonths(language)` correctly

### 9.10 Performance Tests

- [ ] Screen opens in < 1 second when a cached report exists for the current month
- [ ] `LazyColumn` scrolls smoothly (no jank) with all 10 sections loaded
- [ ] No redundant `Flow` re-emissions causing unnecessary recomposition
- [ ] Room queries do not block the main thread (all called from `viewModelScope.launch`)

---

*This specification was authored after a thorough analysis of the TrackFinz v1.1.2 source code in July 2026.*  
*All architectural decisions are grounded in the existing patterns observed in the codebase.*  
*No existing screens, routes, repositories, or entities were modified in this spec — only additive changes.*
