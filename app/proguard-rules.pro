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

# Cast / DLNA serialization
-keepclassmembers class com.atharok.btremote.domain.entities.settings.CastSettings, com.atharok.btremote.domain.entities.settings.CastLink, com.atharok.btremote.domain.entities.settings.CastDevice, com.atharok.btremote.domain.entity.dlna.DlnaDevice {
    <init>(...);
    *** *;
}
-keep class com.atharok.btremote.domain.entities.settings.CastSettings$* { *; }
-keep class com.atharok.btremote.domain.entities.settings.CastLink$* { *; }
-keep class com.atharok.btremote.domain.entities.settings.CastDevice$* { *; }
-keep class com.atharok.btremote.domain.entity.dlna.DlnaDevice$* { *; }