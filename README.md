# VYORA

Pemutar musik Android **mode tamu (tanpa perlu login akun)** berbasis YouTube Music & musik lokal — dibalut desain **Obsidian Royal Violet Liquid Glass** & **Editorial**.

## Fitur Unggulan VYORA v1.1

- **100% Mode Tamu (Tanpa Login)** — Langsung cari, putar, unduh offline, dan nikmati radio tanpa perlu masuk akun Google/YouTube.
- **Discord Rich Presence Bawaan ("Listening to VYORA")** — Tampilkan kartu *Listening to VYORA* lengkap dengan sampul lagu, judul, artis, dan *progress bar* durasi berjalan *real-time* langsung di profil Discord tanpa aplikasi tambahan.
- **Unduhan Offline & Auto-Cache Pintar** — Simpan lagu favorit agar bisa diputar kapan pun tanpa sinyal/kuota, ditambah `SimpleCache` otomatis hingga 2 GB.
- **Mode Lagu ⇄ Video Klip (MV)** — Saklar instan di layar Pemutar untuk berpindah mulus antara sampul album dan video klip langsung di bingkai kaca VYORA.
- **Karaoke Per-Kata, Terjemahan ID & Romaji** — Lirik multi-sumber (*BetterLyrics TTML* → *LRCLIB* → *KuGou* + cache offline), lengkap dengan sorotan karaoke per-kata, terjemahan otomatis Bahasa Indonesia, dan romanisasi Korea/Jepang (*Romaji*).
- **Studio Audio & Equalizer VYORA** — Preset *Slowed + Reverb*, *Deep Slowed*, *Nightcore*, kecepatan & *pitch* manual, Equalizer 5-band dengan profil *TWS Harman AutoEQ* & *VYORA Signature*, *Bass Boost*, *3D Spasial Virtualizer*, dan normalisasi volume.
- **Statistik, Streak Harian & Kartu Story VYORA Recap** — Pantau menit dengar, *streak* harian, Top Lagu, Top Artis, dan ekspor kartu *Story* PNG `1080×1920` sekali ketuk.
- **Studio Poster Lagu & Lirik** — Buat kartu foto *Cutout AI* maupun poster lirik editorial siap bagikan ke Instagram/WhatsApp.
- **Widget Layar Utama Liquid Glass** — Kontrol pemutaran langsung dari *Home Screen* HP.
- **Impor Playlist Lintas Platform & Musik Lokal** — Impor playlist publik dari Spotify, Apple Music, atau YouTube Music, serta putar MP3/FLAC di perangkat.
- **Cek Pembaruan Otomatis** — Periksa dan unduh rilis APK terbaru langsung dari menu *Pengaturan → Tentang*.

## Build dari Source

Prasyarat: **JDK 17** dan **Android SDK 35**.

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

APK hasil build berada di `app/build/outputs/apk/debug/app-debug.apk`. Rilis bertanda tangan dipublikasikan otomatis lewat GitHub Actions pada setiap push ke branch `main`.

## Privasi & Catatan

- Semua data (lagu disukai, playlist, unduhan offline, statistik putar, cache lirik, pengaturan audio) disimpan **100% lokal di perangkat** dan dapat diekspor lewat menu Backup JSON di Pustaka.
- Proyek independen; tidak berafiliasi dengan YouTube, Google, Spotify, atau Apple Music.
