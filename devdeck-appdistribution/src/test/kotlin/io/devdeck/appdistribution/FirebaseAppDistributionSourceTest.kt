package io.devdeck.appdistribution

import android.app.Activity
import android.net.Uri
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.appdistribution.AppDistributionRelease
import com.google.firebase.appdistribution.BinaryType
import com.google.firebase.appdistribution.FirebaseAppDistribution
import com.google.firebase.appdistribution.InterruptionLevel
import com.google.firebase.appdistribution.OnProgressListener
import com.google.firebase.appdistribution.UpdateProgress
import com.google.firebase.appdistribution.UpdateStatus
import com.google.firebase.appdistribution.UpdateTask
import io.devdeck.updates.AvailableUpdate
import java.io.IOException
import java.util.concurrent.Executor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FirebaseAppDistributionSourceTest {

    private val distribution = FakeDistribution()
    private val source = FirebaseAppDistributionSource { distribution }

    @Test
    fun `new release becomes an available update`() = runBlocking {
        distribution.release = FakeRelease(displayVersion = "1.4.0", versionCode = 412, releaseNotes = "Fixes")

        assertEquals(AvailableUpdate("1.4.0", 412, "Fixes"), source.checkForUpdate())
    }

    @Test
    fun `blank release notes are dropped`() = runBlocking {
        distribution.release = FakeRelease(displayVersion = "1.4.0", versionCode = 412, releaseNotes = "  ")

        assertNull(source.checkForUpdate()?.releaseNotes)
    }

    @Test
    fun `no new release means no update`() = runBlocking {
        distribution.release = null

        assertNull(source.checkForUpdate())
    }

    @Test
    fun `failed checks are rethrown`() = runBlocking {
        distribution.checkError = IOException("Tester not signed in")

        try {
            source.checkForUpdate()
            fail("Expected the check to fail")
        } catch (e: IOException) {
            assertEquals("Tester not signed in", e.message)
        }
    }

    @Test
    fun `sign in state comes from Firebase`() = runBlocking {
        assertFalse(source.isSignedIn())

        source.signIn()
        assertTrue(source.isSignedIn())

        source.signOut()
        assertFalse(source.isSignedIn())
    }

    @Test
    fun `missing Firebase setup reads as signed out`() {
        val unavailable = FirebaseAppDistributionSource { throw IllegalStateException("FirebaseApp is not initialized") }

        assertFalse(unavailable.isSignedIn())
    }

    @Test
    fun `install reports download progress`() = runBlocking {
        distribution.progress = listOf(0L to 200L, 50L to 200L, 200L to 200L, 10L to 0L)
        val reported = mutableListOf<Float?>()

        source.install(AvailableUpdate("1.4.0")) { reported += it }

        assertEquals(listOf(0f, 0.25f, 1f, null), reported)
    }

    @Test
    fun `describes itself as a sign-in source`() {
        assertEquals("Firebase App Distribution", source.name)
        assertTrue(source.requiresSignIn)
        assertEquals("https://appdistribution.firebase.google.com/testerapps", source.browseBuildsUrl)
    }
}

private class FakeRelease(private val displayVersion: String, private val versionCode: Long, private val releaseNotes: String?) :
    AppDistributionRelease {
    override fun getDisplayVersion(): String = displayVersion
    override fun getVersionCode(): Long = versionCode
    override fun getReleaseNotes(): String? = releaseNotes
    override fun getBinaryType(): BinaryType = BinaryType.APK
}

private class FakeDistribution : FirebaseAppDistribution {
    var release: AppDistributionRelease? = null
    var checkError: Exception? = null
    var progress: List<Pair<Long, Long>> = emptyList()
    private var signedIn = false

    override fun isTesterSignedIn(): Boolean = signedIn

    override fun signInTester(): Task<Void> {
        signedIn = true
        return CompletedTask(null)
    }

    override fun signOutTester() {
        signedIn = false
    }

    override fun checkForNewRelease(): Task<AppDistributionRelease> = CompletedTask(release, checkError)

    override fun updateApp(): UpdateTask = CompletedUpdateTask(progress)

    override fun updateIfNewReleaseAvailable(): UpdateTask = CompletedUpdateTask(progress)

    override fun startFeedback(infoTextResourceId: Int) = Unit
    override fun startFeedback(infoText: CharSequence) = Unit
    override fun startFeedback(infoTextResourceId: Int, screenshotUri: Uri?) = Unit
    override fun startFeedback(infoText: CharSequence, screenshotUri: Uri?) = Unit
    override fun showFeedbackNotification(infoTextResourceId: Int, interruptionLevel: InterruptionLevel) = Unit
    override fun showFeedbackNotification(infoText: CharSequence, interruptionLevel: InterruptionLevel) = Unit
    override fun cancelFeedbackNotification() = Unit
}

/** A task that has already finished, so listeners run immediately on the calling thread. */
private class CompletedTask<T>(private val value: T?, private val error: Exception? = null) : Task<T>() {
    override fun addOnCompleteListener(listener: OnCompleteListener<T>): Task<T> = apply { listener.onComplete(this) }
    override fun addOnSuccessListener(listener: OnSuccessListener<in T>): Task<T> = unsupported()
    override fun addOnSuccessListener(activity: Activity, listener: OnSuccessListener<in T>): Task<T> = unsupported()
    override fun addOnSuccessListener(executor: Executor, listener: OnSuccessListener<in T>): Task<T> = unsupported()
    override fun addOnFailureListener(listener: OnFailureListener): Task<T> = unsupported()
    override fun addOnFailureListener(activity: Activity, listener: OnFailureListener): Task<T> = unsupported()
    override fun addOnFailureListener(executor: Executor, listener: OnFailureListener): Task<T> = unsupported()
    override fun getException(): Exception? = error

    @Suppress("UNCHECKED_CAST")
    override fun getResult(): T = value as T
    override fun <X : Throwable> getResult(exceptionType: Class<X>): T = result
    override fun isCanceled(): Boolean = false
    override fun isComplete(): Boolean = true
    override fun isSuccessful(): Boolean = error == null
}

private class CompletedUpdateTask(private val progress: List<Pair<Long, Long>>) : UpdateTask() {
    private val task = CompletedTask<Void>(null)

    override fun addOnProgressListener(listener: OnProgressListener): UpdateTask = apply {
        progress.forEach { (downloaded, total) -> listener.onProgressUpdate(FakeProgress(downloaded, total)) }
    }

    override fun addOnProgressListener(executor: Executor?, listener: OnProgressListener): UpdateTask = addOnProgressListener(listener)
    override fun addOnCompleteListener(listener: OnCompleteListener<Void>): Task<Void> = apply { listener.onComplete(this) }
    override fun addOnSuccessListener(listener: OnSuccessListener<in Void>): Task<Void> = unsupported()
    override fun addOnSuccessListener(activity: Activity, listener: OnSuccessListener<in Void>): Task<Void> = unsupported()
    override fun addOnSuccessListener(executor: Executor, listener: OnSuccessListener<in Void>): Task<Void> = unsupported()
    override fun addOnFailureListener(listener: OnFailureListener): Task<Void> = unsupported()
    override fun addOnFailureListener(activity: Activity, listener: OnFailureListener): Task<Void> = unsupported()
    override fun addOnFailureListener(executor: Executor, listener: OnFailureListener): Task<Void> = unsupported()
    override fun getException(): Exception? = null
    override fun getResult(): Void? = task.result
    override fun <X : Throwable> getResult(exceptionType: Class<X>): Void? = task.result
    override fun isCanceled(): Boolean = false
    override fun isComplete(): Boolean = true
    override fun isSuccessful(): Boolean = true
}

private class FakeProgress(private val downloaded: Long, private val total: Long) : UpdateProgress {
    override fun getApkBytesDownloaded(): Long = downloaded
    override fun getApkFileTotalBytes(): Long = total
    override fun getUpdateStatus(): UpdateStatus = UpdateStatus.DOWNLOADING
}

private fun unsupported(): Nothing = throw UnsupportedOperationException("Not used by FirebaseAppDistributionSource")
