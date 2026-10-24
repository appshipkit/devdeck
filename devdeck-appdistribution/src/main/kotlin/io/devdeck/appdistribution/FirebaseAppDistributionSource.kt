package io.devdeck.appdistribution

import com.google.android.gms.tasks.Task
import com.google.firebase.appdistribution.FirebaseAppDistribution
import io.devdeck.updates.AvailableUpdate
import io.devdeck.updates.UpdateSource
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

public class FirebaseAppDistributionSource(
    private val distribution: () -> FirebaseAppDistribution = { FirebaseAppDistribution.getInstance() },
) : UpdateSource {

    override val name: String = "Firebase App Distribution"

    override val requiresSignIn: Boolean = true

    override val browseBuildsUrl: String = TESTER_PAGE_URL

    override fun isSignedIn(): Boolean = runCatching { distribution().isTesterSignedIn }.getOrDefault(false)

    override suspend fun signIn() {
        distribution().signInTester().await()
    }

    override fun signOut() {
        distribution().signOutTester()
    }

    override suspend fun checkForUpdate(): AvailableUpdate? = distribution().checkForNewRelease().await()?.let { release ->
        AvailableUpdate(
            versionName = release.displayVersion,
            versionCode = release.versionCode,
            releaseNotes = release.releaseNotes?.takeIf { it.isNotBlank() },
        )
    }

    override suspend fun install(update: AvailableUpdate, onProgress: (Float?) -> Unit) {
        distribution().updateApp()
            .addOnProgressListener { progress ->
                val total = progress.apkFileTotalBytes
                onProgress(if (total > 0) progress.apkBytesDownloaded.toFloat() / total else null)
            }
            .await()
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        when {
            task.isCanceled -> continuation.cancel(CancellationException("Task was cancelled"))
            task.isSuccessful -> continuation.resume(task.result)
            else -> continuation.resumeWithException(task.exception ?: IllegalStateException("Task failed"))
        }
    }
}

private const val TESTER_PAGE_URL = "https://appdistribution.firebase.google.com/testerapps"
