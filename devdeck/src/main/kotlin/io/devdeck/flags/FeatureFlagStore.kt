package io.devdeck.flags

public interface FeatureFlagStore {
    public operator fun <T : Any> get(flag: FeatureFlag<T>): T

    public operator fun <T : Any> set(flag: FeatureFlag<T>, value: T)

    public fun isOverridden(flag: FeatureFlag<*>): Boolean

    public fun reset(flag: FeatureFlag<*>)
}
