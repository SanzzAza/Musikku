# 🎵 Musikku — Aplikasi Musik Android (Kotlin + Jetpack Compose)

Aplikasi streaming musik ala Spotify / YouTube Music.

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
| Sumber data | [Deezer API](https://developers.deezer.com/api) — gratis, **tanpa API key** |

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

## ⚠️ Catatan Penting
- Deezer API gratis hanya menyediakan **preview 30 detik** per lagu. Ini batasan legal —
  lagu penuh butuh lisensi (Spotify/YT Music membayar label rekaman).
- URL preview Deezer kedaluwarsa ± 1 jam, jadi player memakai URI `deezer://track/{id}`
  yang di-resolve ke URL baru tepat sebelum diputar (lihat `PlaybackService.resolve`).
- Kalau mau lagu full: ganti sumber ke **Jamendo API** (musik indie berlisensi Creative Commons,
  lagu penuh, gratis dengan client_id) atau server/musik milik sendiri.

## 🚀 Build & Rilis Otomatis (GitHub Actions)
Setiap **push ke branch `main`** (atau klik *Run workflow* di tab **Actions**) akan:
1. Build APK release yang sudah ditandatangani
2. Membuat **GitHub Release** baru `v1.0.<nomor build>` berisi file APK

Download APK terbaru: **Releases → Latest**.

Secrets yang dipakai (Settings → Secrets and variables → Actions):
`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
