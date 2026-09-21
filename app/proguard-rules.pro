# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ---- Tomatoes Clock Specific Rules ----

-keepclassmembers enum com.hxlxz.tomatoclock.* {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public int getValue();
}
-keepclassmembers class com.hxlxz.tomatoclock.SavedTimerState {
    *;
}

# [Security & Performance] Strip all Log.d and Log.v in Release build
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
