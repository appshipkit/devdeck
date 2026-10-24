# OkHttp is a compileOnly dependency: the app brings its own. Don't fail R8 in apps that
# add devdeck-okhttp without using OkHttp directly.
-dontwarn okhttp3.**
-dontwarn okio.**
