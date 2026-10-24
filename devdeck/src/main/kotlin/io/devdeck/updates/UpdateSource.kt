package io.devdeck.updates

import androidx.compose.runtime.Immutable

@Immutable
public data class AvailableUpdate(val versionName: String, val versionCode: Long? = null, val releaseNotes: String? = null) {
    val label: String get() = if (versionCode != null) "v$versionName ($versionCode)" else "v$versionName"
}

public interface UpdateSource {
    public val name: String

    public val requiresSignIn: Boolean get() = false

    public fun isSignedIn(): Boolean = true

    public suspend fun signIn() {}

    public fun signOut() {}

    public suspend fun checkForUpdate(): AvailableUpdate?

    public suspend fun availableBuilds(): List<AvailableUpdate> = emptyList()

    public val browseBuildsUrl: String? get() = null

    public suspend fun install(update: AvailableUpdate, onProgress: (Float?) -> Unit)
}
