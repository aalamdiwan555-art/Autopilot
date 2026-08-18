# Start.io (StartApp) SDK ProGuard Rules
-keep class com.startapp.** { *; }
-keep interface com.startapp.** { *; }
-keep enum com.startapp.** { *; }
-dontwarn com.startapp.**

# General AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# Parcelable
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
