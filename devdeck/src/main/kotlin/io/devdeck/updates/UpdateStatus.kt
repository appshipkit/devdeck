package io.devdeck.updates

public sealed interface UpdateStatus {
    public data object Idle : UpdateStatus

    public data object Checking : UpdateStatus

    public data object UpToDate : UpdateStatus

    public data class Available(val update: AvailableUpdate) : UpdateStatus

    public data class Installing(val update: AvailableUpdate, val progress: Float?) : UpdateStatus

    public data class Failed(val message: String) : UpdateStatus
}
