# Proguard Rules untuk Jimpitan Digital
# Berguna untuk mencegah error saat proses shrink/minify (pengurangan ukuran aplikasi)

# Moshi & Retrofit
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keep class * extends com.squareup.moshi.JsonAdapter { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }

# Supabase DTOs & Models
-keep class com.example.data.remote.** { *; }
-keep class com.example.data.local.entity.** { *; }

# Room Database
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# iText PDF
-keep class com.itextpdf.** { *; }

# QRCode
-keep class io.github.g0dkar.qrcode.** { *; }

# Debugging information (opsional, disarankan agar error log tetap terbaca)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Ignore warnings for optional dependencies from iText and OkHttp
-dontwarn com.fasterxml.jackson.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn org.slf4j.**
