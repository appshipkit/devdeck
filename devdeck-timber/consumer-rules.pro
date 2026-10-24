# Timber is a compileOnly dependency: the app brings its own. Don't fail R8 in apps that
# add devdeck-timber without Timber on the classpath.
-dontwarn timber.log.**
