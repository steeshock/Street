package com.steeshock.android.streetworkout.services.auth

interface IAuthService {
    /**
     * Check if current user is authorized
     */
    suspend fun isUserAuthorized(): Boolean

    /**
     * Get current authorized user email
     */
    suspend fun getUserEmail(): String?

    /**
     * Get current authorized user name
     */
    suspend fun getUsername(): String?

    /**
     * Sign up new user (registration)
     */
    suspend fun signUp(
        userCredentials: UserCredentials,
        onSuccess: (String?) -> Unit,
        onError: (Exception) -> Unit,
    )

    /**
     * Sign in existing user (authorization)
     */
    suspend fun signIn(
        userCredentials: UserCredentials,
        onSuccess: (String?) -> Unit,
        onError: (Exception) -> Unit,
    )

    /**
     * Logout current authorized user
     */
    suspend fun logout()
}