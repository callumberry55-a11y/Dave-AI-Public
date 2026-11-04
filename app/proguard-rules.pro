# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles directive in build.gradle.

# Keep Room entities and DAOs
-keep class com.local.daveai.** { *; }

# Keep Glance ActionCallbacks for reflection instantiation
-keep class * implements androidx.glance.appwidget.action.ActionCallback { public <init>(); }

# Keep Gemini Client and kotlinx.serialization
-keep class com.google.ai.client.generativeai.** { *; }
-keep class kotlinx.serialization.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep BuildConfig and SecurityManager
-keep class com.local.daveai.BuildConfig { *; }
-keep class com.local.daveai.SecurityManager { *; }
