# Preserve source file and line numbers for readable stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve Kotlin metadata and annotations used by Firestore & Credential Manager
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class kotlin.Metadata { *; }

# Keep app Firestore models & @Keep classes
-keepclassmembers class com.example.data.model.** { *; }
-keep class com.example.data.model.** { *; }
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers @androidx.annotation.Keep class * { *; }

# Firebase Firestore
-keepclassmembers class * {
    @com.google.firebase.firestore.* <fields>;
    @com.google.firebase.firestore.* <methods>;
}
-dontwarn com.google.firebase.firestore.**
-dontwarn io.grpc.**

# Firebase Auth & Cloud Functions
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.firebase.functions.** { *; }
-dontwarn com.google.firebase.auth.**
-dontwarn com.google.firebase.functions.**

# Jetpack Credential Manager & Google Identity
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn androidx.credentials.**
-dontwarn com.google.android.libraries.identity.googleid.**
