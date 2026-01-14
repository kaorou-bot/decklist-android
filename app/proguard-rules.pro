-keep class com.mtgo.decklistmanager.data.local.entity.** { *; }
-keep class com.mtgo.decklistmanager.domain.model.** { *; }

# Keep Room entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Gson models
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep Retrofit models
-keepattributes Signature
-keepattributes Exceptions
-keepattributes SourceFile
-keepattributes LineNumberTable
-keep class com.mtgo.decklistmanager.data.remote.api.** { *; }
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
