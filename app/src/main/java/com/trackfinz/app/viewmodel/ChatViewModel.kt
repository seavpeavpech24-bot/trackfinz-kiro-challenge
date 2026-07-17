package com.trackfinz.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackfinz.app.data.model.ChatMessageEntity
import com.trackfinz.app.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    val allMessages = chatRepository.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertMessage(text: String, isUser: Boolean) {
        viewModelScope.launch {
            chatRepository.insertMessage(
                ChatMessageEntity(
                    text = text,
                    isUser = isUser,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            chatRepository.deleteAllMessages()
        }
    }
}
