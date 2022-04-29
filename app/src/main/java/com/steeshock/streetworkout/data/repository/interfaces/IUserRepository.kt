package com.steeshock.streetworkout.data.repository.interfaces

import com.steeshock.streetworkout.data.model.User

interface IUserRepository {
    /**
     * Fetch if exist user info or create new one in remote storage
     * After user created, add it to local storage (update user locally if exists)
     *
     * [userId] - uid of created Firebase User
     *
     * @return created User
     */
    suspend fun getOrCreateUser(userId: String, name: String, email: String): User?

    /**
     * Get list of favorite places by userId
     */
    suspend fun getUserFavorites(userId: String): List<String>
}