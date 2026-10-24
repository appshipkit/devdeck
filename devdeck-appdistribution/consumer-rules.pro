# firebase-appdistribution-api is a compileOnly dependency: the app brings its own. Don't fail
# R8 in apps that add devdeck-appdistribution without the Firebase SDK on the classpath.
-dontwarn com.google.firebase.appdistribution.**
-dontwarn com.google.android.gms.tasks.**
