package com.trackfinz.app.data.repository

import com.trackfinz.app.data.database.UserDao
import com.trackfinz.app.data.datastore.UserPreferences
import com.trackfinz.app.data.model.UserEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val dao: UserDao,
    val prefs: UserPreferences
) {
    val userFlow = dao.getUserFlow()

    suspend fun register(user: UserEntity): Long {
        val id = dao.insert(user)
        prefs.setLoggedIn(true)
        prefs.setOnboarded(true)
        return id
    }

    suspend fun getUser() = dao.getUser()

    suspend fun update(user: UserEntity) = dao.update(user)

    suspend fun logout() = prefs.setLoggedIn(false)

    /** Simple PIN check — in production use bcrypt/argon2 */
    suspend fun verifyPin(pin: String): Boolean {
        val user = dao.getUser() ?: return false
        return user.pin == pin
    }
}
