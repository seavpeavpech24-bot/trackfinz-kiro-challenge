package com.trackfinz.app.ui.screens.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trackfinz.app.i18n.LocalStrings
import com.trackfinz.app.i18n.Str
import com.trackfinz.app.data.model.TransactionEntity
import com.trackfinz.app.data.model.ChatMessageEntity
import com.trackfinz.app.viewmodel.ChatViewModel
import com.trackfinz.app.ui.theme.*
import com.trackfinz.app.utils.AIFinancialAssistant
import com.trackfinz.app.utils.ChatMessage
import com.trackfinz.app.viewmodel.SettingsViewModel
import com.trackfinz.app.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssistantScreen(
    onBack: () -> Unit,
    txVm: TransactionViewModel = hiltViewModel(),
    settingsVm: SettingsViewModel = hiltViewModel(),
    chatVm: ChatViewModel = hiltViewModel()
) {
    val transactions by txVm.allTransactions.collectAsState()
    val currency by settingsVm.currency.collectAsState()
    val savedMessages by chatVm.allMessages.collectAsState()
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val s = LocalStrings.current

    // Load saved messages on first launch
    LaunchedEffect(savedMessages) {
        if (messages.isEmpty() && savedMessages.isNotEmpty()) {
            messages = savedMessages.map { 
                ChatMessage(it.text, it.isUser, it.timestamp) 
            }
        }
    }

    // Scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s(Str.AI_FINANCIAL_ASSISTANT)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = s(Str.BACK))
                    }
                },
                actions = {
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = {
                            chatVm.clearHistory()
                            messages = emptyList()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear History", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E88E5)
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(s(Str.TYPE_YOUR_QUESTION)) },
                            shape = RoundedCornerShape(24.dp),
                            enabled = !isLoading,
                            maxLines = 3
                        )
                        Spacer(Modifier.width(8.dp))
                        FloatingActionButton(
                            onClick = {
                                if (inputText.isNotBlank() && !isLoading) {
                                    val question = inputText.trim()
                                    inputText = ""
                                    
                                    // Add user message
                                    val userMessage = ChatMessage(question, isUser = true)
                                    messages = messages + userMessage
                                    
                                    // Save to database
                                    chatVm.insertMessage(question, isUser = true)
                                    
                                    // Get AI response
                                    scope.launch {
                                        isLoading = true
                                        val response = AIFinancialAssistant.askQuestion(
                                            question, transactions, currency
                                        )
                                        val aiMessage = ChatMessage(response, isUser = false)
                                        messages = messages + aiMessage
                                        
                                        // Save AI response to database
                                        chatVm.insertMessage(response, isUser = false)
                                        
                                        isLoading = false
                                    }
                                }
                            },
                            containerColor = Color(0xFF1E88E5),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                if (isLoading) Icons.Default.HourglassEmpty else Icons.Default.Send,
                                contentDescription = "Send",
                                tint = Color.White
                            )
                        }
                    }
                    // Add spacer for navigation bar
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    ) { padding ->
        val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(if (isDarkTheme) Color.Black else Color(0xFFF5F5F5)),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Welcome message
            if (messages.isEmpty()) {
                item {
                    WelcomeCard(
                        transactions = transactions,
                        onQuestionClick = { question ->
                            inputText = question
                        },
                        s = s
                    )
                }
            }

            // Chat messages
            items(messages) { message ->
                ChatBubble(message, currency)
            }

            // Loading indicator
            if (isLoading) {
                item {
                    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            modifier = Modifier.widthIn(max = 280.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant else Color.White
                            ),
                            shape = RoundedCornerShape(
                                topStart = 20.dp,
                                topEnd = 20.dp,
                                bottomEnd = 20.dp,
                                bottomStart = 4.dp
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF1E88E5)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    s(Str.ANALYZING_DATA),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeCard(
    transactions: List<TransactionEntity>,
    onQuestionClick: (String) -> Unit,
    s: (String) -> String
) {
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    
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
                        listOf(Color(0xFF1E88E5), Color(0xFF1976D2))
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        s(Str.YOUR_AI_ADVISOR),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    s(Str.ASK_ANYTHING),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    // Suggested questions
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant else Color.White
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                s(Str.EXAMPLE_QUESTIONS),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E88E5)
            )
            Spacer(Modifier.height(12.dp))

            val suggestedQuestions = AIFinancialAssistant.getSuggestedQuestions(transactions)
            suggestedQuestions.forEach { question ->
                SuggestedQuestionChip(question, onQuestionClick)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun SuggestedQuestionChip(question: String, onClick: (String) -> Unit) {
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    
    OutlinedCard(
        onClick = { onClick(question) },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surface else Color(0xFFF5F5F5)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isDarkTheme) MaterialTheme.colorScheme.outline else Color(0xFFE0E0E0)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = null,
                tint = Color(0xFF1E88E5),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                question,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, currency: String) {
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            Icon(
                Icons.Default.SmartToy,
                contentDescription = null,
                tint = Color(0xFF1E88E5),
                modifier = Modifier
                    .size(32.dp)
                    .padding(top = 4.dp)
            )
            Spacer(Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (message.isUser) {
                        Color(0xFF1E88E5)
                    } else {
                        if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant else Color.White
                    }
                ),
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomEnd = if (message.isUser) 4.dp else 20.dp,
                    bottomStart = if (message.isUser) 20.dp else 4.dp
                )
            ) {
                Text(
                    message.text,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isUser) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                dateFormat.format(Date(message.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        if (message.isUser) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(32.dp)
                    .padding(top = 4.dp)
            )
        }
    }
}
