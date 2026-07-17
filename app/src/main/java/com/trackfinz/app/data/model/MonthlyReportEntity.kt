package com.trackfinz.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persists a generated monthly financial report so it loads instantly
 * on re-visits within the same month.
 */
@Entity(tableName = "monthly_reports")
data class MonthlyReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val month: Int,               // 1–12
    val year: Int,
    val narrative: String,        // AI-generated or offline fallback
    val isFallback: Boolean = false, // true when generated without Gemini
    val healthScore: Int,         // 0–100 at time of generation
    val totalIncome: Double,
    val totalExpenses: Double,
    val savingsRate: Double,
    val generatedAt: Long = System.currentTimeMillis()
)
