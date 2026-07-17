package com.trackfinz.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.trackfinz.app.ui.screens.analytics.AnalyticsScreen
import com.trackfinz.app.ui.screens.assistant.AIAssistantScreen
import com.trackfinz.app.ui.screens.auth.LoginScreen
import com.trackfinz.app.ui.screens.auth.PinLockScreen
import com.trackfinz.app.ui.screens.auth.PinSetupScreen
import com.trackfinz.app.ui.screens.auth.RegisterScreen
import com.trackfinz.app.ui.screens.bills.BillRemindersScreen
import com.trackfinz.app.ui.screens.budget.AIBudgetRecommendationsScreen
import com.trackfinz.app.ui.screens.budget.BudgetScreen
import com.trackfinz.app.ui.screens.dashboard.DashboardScreen
import com.trackfinz.app.ui.screens.goals.GoalsScreen
import com.trackfinz.app.ui.screens.insights.AIInsightsScreen
import com.trackfinz.app.ui.screens.onboarding.OnboardingScreen
import com.trackfinz.app.ui.screens.profile.ProfileScreen
import com.trackfinz.app.ui.screens.receipt.ReceiptScannerScreen
import com.trackfinz.app.ui.screens.splash.SplashScreen
import com.trackfinz.app.ui.screens.report.AIReportScreen
import com.trackfinz.app.ui.screens.transactions.AddTransactionScreen
import com.trackfinz.app.ui.screens.transactions.TransactionsScreen

@Composable
fun AppNavGraph(navController: NavHostController, startDestination: String) {

    // Shared bottom-nav handler for all main screens
    val onNavigate: (String) -> Unit = { route ->
        if (route in NavRoutes.mainRoutes) {
            navController.navigate(route) {
                popUpTo(NavRoutes.DASHBOARD) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        } else {
            navController.navigate(route)
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(NavRoutes.SPLASH) {
            SplashScreen(onFinished = { dest ->
                navController.navigate(dest) { popUpTo(NavRoutes.SPLASH) { inclusive = true } }
            })
        }

        composable(NavRoutes.ONBOARDING) {
            OnboardingScreen(onFinished = {
                navController.navigate(NavRoutes.REGISTER) {
                    popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                }
            })
        }

        composable(NavRoutes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavRoutes.DASHBOARD) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                },
                onRegister = { navController.navigate(NavRoutes.REGISTER) }
            )
        }

        composable(NavRoutes.REGISTER) {
            RegisterScreen(
                onRegistered = { name, email ->
                    navController.navigate(NavRoutes.pinSetup(name, email)) {
                        popUpTo(NavRoutes.REGISTER) { inclusive = true }
                    }
                },
                onLogin = { navController.navigate(NavRoutes.LOGIN) }
            )
        }

        composable(
            route = NavRoutes.PIN_SETUP,
            arguments = listOf(
                navArgument("name")  { type = NavType.StringType; defaultValue = "User" },
                navArgument("email") { type = NavType.StringType; defaultValue = "" }
            )
        ) { back ->
            val name  = back.arguments?.getString("name")  ?: "User"
            val email = back.arguments?.getString("email") ?: ""
            PinSetupScreen(
                userName  = name,
                userEmail = email,
                onPinSet  = {
                    navController.navigate(NavRoutes.DASHBOARD) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable(NavRoutes.PIN_LOCK) {
            PinLockScreen(onUnlocked = {
                navController.navigate(NavRoutes.DASHBOARD) {
                    popUpTo(NavRoutes.PIN_LOCK) { inclusive = true }
                }
            })
        }

        // ── Main screens (all have bottom nav) ────────────────────────────────

        composable(NavRoutes.DASHBOARD) {
            DashboardScreen(
                onNavigate = onNavigate,
                onAddTransaction = { navController.navigate(NavRoutes.addTransaction()) }
            )
        }

        composable(NavRoutes.TRANSACTIONS) {
            TransactionsScreen(
                onNavigate = onNavigate,
                onAdd = { type -> navController.navigate(NavRoutes.addTransaction(type = type)) },
                onEdit = { id -> navController.navigate(NavRoutes.addTransaction(id = id)) }
            )
        }

        composable(NavRoutes.BUDGET) {
            BudgetScreen(onNavigate = onNavigate)
        }

        composable(NavRoutes.GOALS) {
            GoalsScreen(onNavigate = onNavigate)
        }

        composable(NavRoutes.ANALYTICS) {
            AnalyticsScreen(onNavigate = onNavigate)
        }

        composable(NavRoutes.BILL_REMINDERS) {
            BillRemindersScreen(onNavigate = onNavigate)
        }

        // ── Sub-screens ───────────────────────────────────────────────────────

        composable(
            route = NavRoutes.ADD_TRANSACTION,
            arguments = listOf(
                navArgument("id")   { type = NavType.IntType; defaultValue = -1 },
                navArgument("type") { type = NavType.StringType; defaultValue = "EXPENSE" }
            )
        ) { back ->
            val id   = back.arguments?.getInt("id") ?: -1
            val type = back.arguments?.getString("type") ?: "EXPENSE"
            AddTransactionScreen(
                transactionId = if (id == -1) null else id,
                initialType = type,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(NavRoutes.LOGIN) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable(NavRoutes.RECEIPT_SCANNER) {
            ReceiptScannerScreen(
                onBack = { navController.popBackStack() },
                onReceiptSaved = {
                    navController.navigate(NavRoutes.TRANSACTIONS) {
                        popUpTo(NavRoutes.DASHBOARD) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(NavRoutes.AI_INSIGHTS) {
            AIInsightsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.AI_BUDGET_RECOMMENDATIONS) {
            AIBudgetRecommendationsScreen(
                onBack = { navController.popBackStack() },
                onApplyBudgets = {
                    navController.navigate(NavRoutes.BUDGET) {
                        popUpTo(NavRoutes.AI_BUDGET_RECOMMENDATIONS) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(NavRoutes.AI_ASSISTANT) {
            AIAssistantScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.AI_MONTHLY_REPORT) {
            AIReportScreen(
                onBack = { navController.popBackStack() },
                onNavigateToBudget = {
                    navController.navigate(NavRoutes.BUDGET) {
                        popUpTo(NavRoutes.AI_MONTHLY_REPORT) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToGoals = {
                    navController.navigate(NavRoutes.GOALS) {
                        popUpTo(NavRoutes.AI_MONTHLY_REPORT) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToAnalytics = {
                    navController.navigate(NavRoutes.ANALYTICS) {
                        popUpTo(NavRoutes.AI_MONTHLY_REPORT) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToTransactions = {
                    navController.navigate(NavRoutes.TRANSACTIONS) {
                        popUpTo(NavRoutes.AI_MONTHLY_REPORT) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
