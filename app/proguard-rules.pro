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

# Preserve line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep Room entities, Retrofit/Moshi models, and AI structured output models
-keep class com.example.core.database.** { *; }
-keep class com.example.core.network.** { *; }
-keep class com.example.core.repository.** { *; }
-keep class com.example.core.ai.** { *; }
-keep class com.example.core.ml.** { *; }

# ML Kit GenAI Prompt API & AICore
-dontwarn com.google.mlkit.genai.**
-keep class com.google.mlkit.genai.** { *; }
