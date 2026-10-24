package io.devdeck.logcat

public object LogcatNoise {

    public val PLATFORM_TAGS: Set<String> = setOf(
        "perf_hint",
        "ashmem",
        "EGL_emulation",
        "eglCodecCommon",
        "HostConnection",
        "goldfish-address-space",
        "Gralloc4",
        "libEGL",
        "nativeloader",
        "ziparchive",
        "ProfileInstaller",
        "CompatibilityChangeReporter",
        "ImeTracker",
        "InsetsController",
    )

    public val PLATFORM_TAG_PREFIXES: Set<String> = setOf("VRI[")

    public val PLATFORM_MESSAGE_PREFIXES: Set<String> = setOf("setRequestedFrameRate ")
}
