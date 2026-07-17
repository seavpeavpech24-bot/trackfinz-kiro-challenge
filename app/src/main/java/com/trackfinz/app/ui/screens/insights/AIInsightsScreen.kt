package com.trackfinz.app.ui.screens.insights

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trackfinz.app.ui.theme.*
import com.trackfinz.app.utils.AIInsight
import com.trackfinz.app.utils.AIInsightsGenerator
import com.trackfinz.app.utils.InsightType
import com.trackfinz.app.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIInsightsScreen(
    onBack: () -> Unit,
    vm: TransactionViewModel = hiltViewModel()
) {
    val transactions by vm.allTransactions.collectAsState()
    var insights by remember { mutableStateOf<List<AIInsight>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val s = com.trackfinz.app.i18n.LocalStrings.current

    // Generate insights on first load
    LaunchedEffect(Unit) {
        isLoading = true
        error = null
        try {
            insights = AIInsightsGenerator.generateInsights(transactions)
        } catch (e: Exception) {
            error = s(com.trackfinz.app.i18n.Str.FAILED_GENERATE_INSIGHTS)
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s(com.trackfinz.app.i18n.Str.AI_SPENDING_INSIGHTS)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = s(com.trackfinz.app.i18n.Str.BACK))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                error = null
                                try {
                                    insights = AIInsightsGenerator.generateInsights(transactions)
                                } catch (e: Exception) {
                                    error = s(com.trackfinz.app.i18n.Str.FAILED_GENERATE_INSIGHTS)
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Teal500, Color(0xFF00897B))
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    s(com.trackfinz.app.i18n.Str.YOUR_FINANCIAL_COACH),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                s(com.trackfinz.app.i18n.Str.AI_INSIGHTS_SUBTITLE),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // Loading state
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Teal500)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                s(com.trackfinz.app.i18n.Str.ANALYZING_SPENDING),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Error state
            error?.let { errorMsg ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                errorMsg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Insights list
            items(insights) { insight ->
                InsightCard(insight)
            }

            // Empty state
            if (!isLoading && insights.isEmpty() && error == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                s(com.trackfinz.app.i18n.Str.NO_INSIGHTS_YET),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                s(com.trackfinz.app.i18n.Str.ADD_MORE_TRANSACTIONS),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }

            // Footer info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Teal500,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            s(com.trackfinz.app.i18n.Str.INSIGHTS_POWERED_BY_AI),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InsightCard(insight: AIInsight) {
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    
    val backgroundColor = when (insight.type) {
        InsightType.SPENDING_INCREASE -> if (isDarkTheme) Color(0xFF5C1C1C) else Color(0xFFFFEBEE)
        InsightType.SPENDING_DECREASE -> if (isDarkTheme) Color(0xFF1B3A1B) else Color(0xFFE8F5E9)
        InsightType.SAVING_TIP -> if (isDarkTheme) Color(0xFF3D3A1A) else Color(0xFFFFF9C4)
        InsightType.CATEGORY_ALERT -> if (isDarkTheme) Color(0xFF3D2A1A) else Color(0xFFFFF3E0)
        InsightType.POSITIVE_HABIT -> if (isDarkTheme) Color(0xFF1A2D3D) else Color(0xFFE1F5FE)
        InsightType.WARNING -> if (isDarkTheme) Color(0xFF3D1A2D) else Color(0xFFFCE4EC)
    }

    val borderColor = when (insight.type) {
        InsightType.SPENDING_INCREASE -> if (isDarkTheme) Color(0xFFEF9A9A) else Color(0xFFEF5350)
        InsightType.SPENDING_DECREASE -> if (isDarkTheme) Color(0xFFA5D6A7) else Color(0xFF66BB6A)
        InsightType.SAVING_TIP -> if (isDarkTheme) Color(0xFFFFF59D) else Color(0xFFFDD835)
        InsightType.CATEGORY_ALERT -> if (isDarkTheme) Color(0xFFFFCC80) else Color(0xFFFF9800)
        InsightType.POSITIVE_HABIT -> if (isDarkTheme) Color(0xFF81D4FA) else Color(0xFF29B6F6)
        InsightType.WARNING -> if (isDarkTheme) Color(0xFFF48FB1) else Color(0xFFE91E63)
    }
    
    val textColor = if (isDarkTheme) Color.White else Color.Black.copy(alpha = 0.87f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(borderColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    insight.icon,
                    fontSize = 24.sp
                )
            }

            Spacer(Modifier.width(16.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    insight.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = borderColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    insight.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}
