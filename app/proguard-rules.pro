# ---------------------------------------------------------------------------
# Musikku — keep rules untuk R8 (Fase 1)
# ---------------------------------------------------------------------------

# Atribut yang dibutuhkan Gson untuk membaca signature generik (TypeToken)
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# --- Gson -------------------------------------------------------------------
# TypeToken: Gson membaca tipe generik lewat anotasi signature.
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Kelas model yang diserialisasi/dideserialisasi Gson.
# Kalau nama field diobfuscate, data pengguna (favorit, playlist, snapshot
# pemutaran, backup JSON) akan hilang tanpa error — jadi harus di-keep.
-keep class com.example.musikku.data.ytmusic.** { *; }
-keep class com.example.musikku.data.local.** { *; }
-keep class com.example.musikku.player.PlaybackSnapshot$Snapshot { *; }
-keepclassmembers class com.example.musikku.player.PlaybackSnapshot$Snapshot { <fields>; }

# Enum disimpan sebagai String di SharedPreferences (UiStyle, AppFontStyle),
# jadi nama konstantanya tidak boleh berubah.
-keepclassmembers enum com.example.musikku.data.local.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- NewPipeExtractor -------------------------------------------------------
# Sejak v0.27.x extractor memakai Rhino (mesin JavaScript) untuk memecahkan
# signature YouTube. Kalau kelas Rhino dipangkas/diobfuscate, lagu akan gagal
# diputar secara diam-diam — jadi seluruh paketnya di-keep.
-keep class org.mozilla.javascript.** { *; }
-keep class javax.script.** { *; }
-keep class org.schabi.newpipe.extractor.** { *; }
-keep interface org.schabi.newpipe.extractor.** { *; }

# Kelas opsional/desktop yang tidak ada di Android.
-dontwarn org.mozilla.javascript.**
-dontwarn javax.script.**
-dontwarn java.beans.**
-dontwarn org.jsoup.**
-dontwarn org.slf4j.**
-dontwarn com.google.re2j.**
-dontwarn org.apache.commons.**
-dontwarn org.conscrypt.**
-dontwarn javax.annotation.**

# --- OkHttp / Retrofit (dipakai StreamResolver) -----------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# --- Kotlin metadata --------------------------------------------------------
-keep class kotlin.Metadata { *; }
