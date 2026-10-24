package io.devdeck.timber

import android.os.Process
import io.devdeck.console.ConsoleLevel
import io.devdeck.console.ConsoleLine
import timber.log.Timber

public class DevDeckTimberTree(private val defaultTag: String = "Timber") : Timber.DebugTree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        TimberLogs.buffer.append(
            ConsoleLine(
                level = ConsoleLevel.fromPriority(priority),
                tag = tag ?: defaultTag,
                message = message,
                threadId = Process.myTid(),
            ),
        )
    }
}
