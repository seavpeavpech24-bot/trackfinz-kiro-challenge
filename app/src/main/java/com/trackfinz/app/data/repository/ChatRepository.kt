package com.trackfinz.app.data.repository

import com.trackfinz.app.data.database.ChatMessageDao
import com.trackfinz.app.data.model.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatMessageDao: ChatMessageDao
) {
    fun getAllMessages(): Flow<List<ChatMessageEntity>> = chatMessageDao.getAllFlow()

    suspend fun insertMessage(message: ChatMessageEntity): Long {
        return chatMessageDao.insert(message)
    }

    suspend fun deleteAllMessages() {
        chatMessageDao.deleteAll()
    }

    suspend fun deleteMessage(message: ChatMessageEntity) {
        chatMessageDao.delete(message)
    }
}
