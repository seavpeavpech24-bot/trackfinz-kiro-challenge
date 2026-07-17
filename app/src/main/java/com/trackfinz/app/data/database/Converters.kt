package com.trackfinz.app.data.database

import androidx.room.TypeConverter
import com.trackfinz.app.data.model.*

class Converters {
    @TypeConverter fun fromType(v: TransactionType): String = v.name
    @TypeConverter fun toType(v: String): TransactionType = TransactionType.valueOf(v)

    @TypeConverter fun fromCategory(v: TransactionCategory): String = v.name
    @TypeConverter fun toCategory(v: String): TransactionCategory =
        runCatching { TransactionCategory.valueOf(v) }.getOrDefault(TransactionCategory.OTHER)

    @TypeConverter fun fromBudgetAction(v: BudgetHistoryAction): String = v.name
    @TypeConverter fun toBudgetAction(v: String): BudgetHistoryAction = BudgetHistoryAction.valueOf(v)

    @TypeConverter fun fromGoalAction(v: GoalFundAction): String = v.name
    @TypeConverter fun toGoalAction(v: String): GoalFundAction = GoalFundAction.valueOf(v)

    @TypeConverter fun fromBillFrequency(v: BillFrequency): String = v.name
    @TypeConverter fun toBillFrequency(v: String): BillFrequency =
        runCatching { BillFrequency.valueOf(v) }.getOrDefault(BillFrequency.MONTHLY)
}
