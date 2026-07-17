package com.trackfinz.app.navigation

import android.net.Uri

object NavRoutes {
    const val SPLASH       = "splash"
    const val ONBOARDING   = "onboarding"
    const val LOGIN        = "login"
    const val REGISTER     = "register"
    const val PIN_SETUP    = "pin_setup?name={name}&email={email}"

    fun pinSetup(name: String, email: String) =
        "pin_setup?name=${Uri.encode(name)}&email=${Uri.encode(email)}"
    const val PIN_LOCK     = "pin_lock"
    const val DASHBOARD    = "dashboard"
    const val TRANSACTIONS = "transactions"
    const val ADD_TRANSACTION = "add_transaction?id={id}&type={type}"
    const val BUDGET       = "budget"
    const val GOALS        = "goals"
    const val ANALYTICS    = "analytics"
    const val PROFILE      = "profile"
    const val RECEIPT_SCANNER = "receipt_scanner"
    const val AI_INSIGHTS  = "ai_insights"
    const val AI_BUDGET_RECOMMENDATIONS = "ai_budget_recommendations"
    const val AI_ASSISTANT = "ai_assistant"
    const val AI_MONTHLY_REPORT = "ai_monthly_report"
    const val BILL_REMINDERS = "bill_reminders"

    // Main bottom-nav destinations
    val mainRoutes = setOf(DASHBOARD, TRANSACTIONS, BUDGET, GOALS, ANALYTICS, BILL_REMINDERS)

    fun addTransaction(id: Int? = null, type: String = "EXPENSE") =
        "add_transaction?id=${id ?: -1}&type=$type"
}
