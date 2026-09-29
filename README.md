# 🎵 Musikku — Aplikasi Musik Android (Kotlin + Jetpack Compose)

Aplikasi streaming musik ala Spotify / YouTube Music — **lagu diputar full** (bukan preview).

## ✨ Fitur
- **Katalog lengkap YouTube Music** — artis Indonesia & internasional (Last Child, Tulus, Dewa 19, dll)
- **Beranda**: Trending di Indonesia, tangga lagu, artis teratas, rilisan baru, playlist pilihan
- **Cari**: lagu, artis, album, dan playlist — dengan saran kata kunci saat mengetik & riwayat pencarian
- **Halaman Artis** ala YT Music: lagu teratas, album, single & EP, video, playlist, artis serupa
- **Album & Playlist**: daftar lagu lengkap, putar / acak
- **Radio otomatis**: putar satu lagu → antrian diisi lagu serupa (seperti YT Music)
- **Lagu full** (bukan preview), kualitas audio tertinggi yang tersedia
- **Lirik tersinkron** ala Spotify (sumber: [LRCLIB](https://lrclib.net))
- **Putar di background** + notifikasi media & kontrol lockscreen
- **Koleksi**: lagu yang disukai tersimpan di perangkat

## 🧱 Tech Stack
| Bagian | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Arsitektur | MVVM (ViewModel + StateFlow) |
| Navigasi | Navigation Compose |
| Katalog musik | YouTube Music InnerTube API (`data/ytmusic/YTMusic.kt`) |
| Audio stream | [NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor) |
| Player | AndroidX Media3 (ExoPlayer + MediaSessionService) |
| Gambar | Coil |
| Lirik | LRCLIB |

## 📁 Struktur
```
app/src/main/java/com/example/musikku/
├── AppModule.kt                 # Service locator
├── data/
│   ├── ytmusic/
│   │   ├── YTMusic.kt           # Beranda, cari, artis, album/playlist, radio
│   │   ├── InnerTubeParser.kt   # Parser respons YouTube Music
│   │   ├── StreamResolver.kt    # URL audio full (NewPipe)
│   │   └── Models.kt            # SongItem, ArtistItem, AlbumItem, Section
│   ├── lyrics/LyricsRepository.kt
│   └── local/FavoritesStore.kt
├── player/                      # PlaybackService + PlayerViewModel
└── ui/                          # home, search, artist, collection, library, player, lyrics
```

## ▶️ Cara Menjalankan
1. Buka folder `Musikku` di **Android Studio** (Koala / Ladybug atau lebih baru).
2. Tunggu Gradle sync selesai (JDK 17).
3. Jalankan di emulator / HP (Android 7.0+ / API 24).

## 🎧 Cara Kerja
1. Semua data musik (beranda, pencarian, artis, album, playlist, radio) diambil langsung dari
   **YouTube Music** lewat API internal yang sama dengan music.youtube.com — tanpa login / API key.
2. Saat lagu diputar, `StreamResolver` mengambil URL audio kualitas tertinggi dari videoId lagu itu.
3. Lagu berikutnya di antrian di-*prefetch* supaya perpindahan lagu mulus.

## ⚠️ Catatan Penting
- Mengambil audio dari YouTube melanggar ToS YouTube → aplikasi **tidak bisa masuk Play Store**;
  distribusikan lewat GitHub Releases saja (sama seperti NewPipe / ViMusic / InnerTune).
- Kalau suatu saat lagu tiba-tiba tidak bisa diputar, biasanya karena YouTube mengubah sesuatu →
  update versi `NewPipeExtractor` di `app/build.gradle.kts` ke [rilis terbaru](https://github.com/TeamNewPipe/NewPipeExtractor/releases), lalu push.

## 🚀 Build & Rilis Otomatis (GitHub Actions)
Setiap **push ke branch `main`** (atau klik *Run workflow* di tab **Actions**) akan:
1. Build APK release yang sudah ditandatangani
2. Membuat **GitHub Release** baru `v1.0.<nomor build>` berisi file APK

Download APK terbaru: **Releases → Latest**.

Secrets yang dipakai (Settings → Secrets and variables → Actions):
`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
