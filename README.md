# 🎵 Musikku — Aplikasi Musik Android (Kotlin + Jetpack Compose)

Aplikasi streaming musik ala Spotify / YouTube Music — **lagu diputar full** (bukan preview).

## ✨ Fitur
- **Beranda**: Lagu trending, artis populer, album populer (chart Deezer)
- **Cari**: cari **lagu, artis, dan album** (auto-search saat mengetik, dengan debounce) + kategori jelajah
- **Halaman Artis**: foto header, jumlah penggemar, lagu populer, diskografi, artis serupa
- **Halaman Album**: cover, daftar lagu, putar / acak
- **Pemutar**: mini player + pemutar layar penuh (seek bar, next/prev, shuffle, repeat, suka)
- **Putar di background** + notifikasi media & kontrol lockscreen (Media3 MediaSession)
- **Koleksi**: lagu yang disukai tersimpan di perangkat
- Pause otomatis saat headset dicabut, audio focus (berhenti saat ada telepon)

## 🧱 Tech Stack
| Bagian | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Arsitektur | MVVM (ViewModel + StateFlow) |
| Navigasi | Navigation Compose |
| Network | Retrofit + Gson |
| Gambar | Coil |
| Player | AndroidX Media3 (ExoPlayer + MediaSessionService) |
| Metadata (cari, chart, artis, album) | [Deezer API](https://developers.deezer.com/api) — gratis, tanpa API key |
| Audio full | YouTube Music via [NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor) |

## 📁 Struktur
```
app/src/main/java/com/example/musikku/
├── AppModule.kt              # Service locator (Retrofit, repository, favorit)
├── MainActivity.kt
├── data/
│   ├── model/Models.kt       # Track, Artist, Album
│   ├── remote/DeezerApi.kt   # Endpoint API
│   ├── local/FavoritesStore.kt
│   └── MusicRepository.kt
├── player/
│   ├── PlaybackService.kt    # ExoPlayer + MediaSession (background)
│   └── PlayerViewModel.kt    # Kontrol player dari UI
└── ui/
    ├── MusikkuApp.kt         # Scaffold, bottom nav, NavHost
    ├── home/  search/  artist/  album/  library/  player/
    ├── components/           # TrackRow, AlbumCard, dll
    └── theme/
```

## ▶️ Cara Menjalankan
1. Buka folder `Musikku` di **Android Studio** (Koala / Ladybug atau lebih baru).
2. Tunggu Gradle sync selesai (JDK 17).
3. Jalankan di emulator / HP (Android 7.0+ / API 24).

## 🎧 Cara Audio Full Bekerja
1. Pencarian, chart, info artis & album diambil dari **Deezer** (datanya rapi + cover HD).
2. Saat lagu diputar, `YouTubeAudioResolver` mencari lagu yang sama di **YouTube Music**
   (kategori *Songs*), dicocokkan lewat **judul, artis, dan durasi**, lalu mengambil stream audio
   kualitas tertinggi (Opus 160 kbps / M4A 128 kbps).
3. Lagu berikutnya di antrian di-*prefetch* supaya perpindahan lagu mulus.
4. Kalau YouTube gagal, otomatis fallback ke preview 30 detik Deezer.

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
