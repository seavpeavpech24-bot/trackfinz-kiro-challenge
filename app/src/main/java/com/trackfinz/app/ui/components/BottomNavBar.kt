package com.trackfinz.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trackfinz.app.i18n.LocalStrings
import com.trackfinz.app.i18n.Str
import com.trackfinz.app.navigation.NavRoutes

data class BottomNavItem(val route: String, val icon: ImageVector, val labelKey: String)

val bottomNavItems = listOf(
    BottomNavItem(NavRoutes.DASHBOARD,      Icons.Default.Home,          Str.HOME),
    BottomNavItem(NavRoutes.TRANSACTIONS,   Icons.Default.Receipt,       Str.TXNS_NAV),
    BottomNavItem(NavRoutes.BUDGET,         Icons.Default.PieChart,      Str.BUDGET),
    BottomNavItem(NavRoutes.GOALS,          Icons.Default.Flag,          Str.GOALS),
    BottomNavItem(NavRoutes.BILL_REMINDERS, Icons.Default.Notifications, Str.BILLS_NAV),
    BottomNavItem(NavRoutes.ANALYTICS,      Icons.Default.BarChart,      Str.ANALYTICS)
)

@Composable
fun TrackFinzBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val s = LocalStrings.current

    NavigationBar(
        modifier = Modifier.clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        tonalElevation = 8.dp
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            val label = s(item.labelKey)
            val iconColor by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primary
                              else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "nav_color_${item.route}"
            )

            NavigationBarItem(
                selected = selected,
                onClick = { if (!selected) onNavigate(item.route) },
                icon = {
                    Box(contentAlignment = Alignment.Center) {
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            )
                        }
                        Icon(
                            imageVector = item.icon,
                            contentDescription = label,
                            tint = iconColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = iconColor
                    )
                }
            )
        }
    }
}
