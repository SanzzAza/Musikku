# Catatan update Musikku

## Fase 4 — UI: token desain, mode tema, keadaan kosong, gerak

### Mode tema (baru)
- **Otomatis / Terang / Gelap / Hitam Pekat** — dipilih di Pengaturan → **MODE TEMA**.
- **Hitam Pekat** memakai latar `#000000` untuk layar OLED (hemat baterai).
- **Terang** memakai kertas hangat (`#FAF7F2`), bukan putih dingin, supaya tetap terasa satu keluarga dengan obsidian hangat.
- Perpindahan tema dianimasikan (420 ms) dan ikon status bar otomatis ikut terang/gelap — sebelumnya selalu dipaksa putih sehingga tak terlihat di latar terang.

### Token desain (baru: `ui/theme/Tokens.kt`)
- Sebelumnya warna adalah `val` tetap yang hanya cocok untuk mode gelap, sehingga mode terang mustahil dibuat. Sekarang semua warna berasal dari `WarmPalette` (Gelap / Hitam Pekat / Terang).
- Nama token lama (`WarmInk`, `WarmSurface`, `WarmMuted`, `WarmFaint`, `WarmHairline`, …) **tetap sama**, jadi 400+ titik pemakaian di seluruh layar ikut berubah tanpa ditulis ulang satu per satu.
- Token disembunyikan di balik snapshot state (`WarmTheme`) supaya tetap bisa dibaca dari `remember { }`, `Modifier.composed`, lambda `onClick`, dan fungsi biasa — konteks yang tidak boleh membaca getter `@Composable`. Perubahan tema tetap memicu gambar ulang.
- Skala baru: `Space` (jarak), `Radius` (sudut), `Motion` (durasi 160/240/420 ms), `ContentMaxWidth`.
- `warmWash(alpha)` menggantikan `Color.White.copy(alpha = …)` di layar: di mode terang otomatis jadi hitam tipis, jadi kartu & chip tidak "hilang".

### Keadaan kosong & kerangka muat (baru: `ui/components/Placeholders.kt`)
- Satu komponen `EmptyState` (ikon dalam lingkaran, judul, pesan, aksi opsional) dipakai di **Cari** (tanpa hasil & belum ada riwayat) dan **Playlist kosong** — sebelumnya tiap layar menulis pesannya sendiri tanpa ikon.
- `SkeletonRowList` menampilkan kerangka baris berkilau saat pencarian pertama, menggantikan layar kosong sesaat.

### Gerak
- Perpindahan halaman di `NavHost` kini memakai fade + geser halus (240 ms masuk, 160 ms keluar) dengan durasi dari token `Motion`.
- Bottom sheet dan permukaan kaca ikut menyesuaikan mode tema (tint & fallback putih susu di mode terang).

### Test
- `ThemeTokensTest` (5 test): pemetaan mode tema, latar hitam murni untuk OLED, dan **kontras teks terhadap latar di semua mode** — kalau ada yang memasang teks terang di latar terang, CI gagal sebelum dirilis.

### Checklist uji manual Fase 4
1. Pengaturan → **MODE TEMA** → **Terang**: semua layar terbaca, kartu & chip tetap kelihatan, ikon status bar jadi gelap.
2. Pilih **Hitam Pekat**: latar benar-benar hitam, teks tetap kontras.
3. Pilih **Otomatis**, lalu ganti tema sistem ke terang/gelap — aplikasi mengikuti.
4. Cari sesuatu → kerangka baris muncul sekejap; cari kata ngawur → keadaan kosong berikon.
5. Buka playlist kosong → keadaan kosong dengan tombol "Cari lagu".
6. Pindah antar tab/halaman → transisi halus, tidak tersendat.

## Fase 3 — Fitur cepat: musik di perangkat & impor playlist

### Musik di perangkat (baru)
- Entri **“Musik di perangkat”** di tab Pustaka. Sekali ketuk, aplikasi memindai file audio di HP lewat **MediaStore** (butuh izin `READ_MEDIA_AUDIO` di Android 13+, `READ_EXTERNAL_STORAGE` di versi lama — diminta hanya saat fitur ini dibuka).
- Daftar lagu langsung bisa **Putar semua** atau **Acak**, dan masuk ke antrean pemutar yang sama dengan lagu YouTube Music (lanjut otomatis, kontrol notifikasi, tombol headset).
- Durasi di bawah 30 detik (nada dering/notifikasi) disaring supaya daftar tidak penuh sampah. Artis `<unknown>` ditampilkan sebagai “Artis tidak dikenal”.
- **Perbaikan teknis penting:** `PlaybackService` sebelumnya hanya memakai `DefaultHttpDataSource`, jadi file lokal tidak akan pernah bisa dibuka. Sekarang dibungkus `DefaultDataSource` sehingga `content://` (file HP) dan `file://` ikut tertangani, sementara lagu YouTube tetap lewat `ResolvingDataSource` seperti biasa.
- `SongItem` mendapat field `localUri`; `toMediaItem()` memakai URI file langsung untuk lagu lokal, dan `toSong()` membacanya kembali dari `MediaMetadata` supaya judul/artis tidak hilang saat layar ditutup.

### Impor playlist dari link (baru)
- Entri **“Impor playlist dari link”** di Pustaka. Tempel link (atau teks share lengkap dari aplikasi lain — URL-nya diambil otomatis).
- **Spotify**: membaca halaman embed publik (`open.spotify.com/embed/playlist/...` dan `/album/...`), mengambil `__NEXT_DATA__` → `props.pageProps.state.data.entity.trackList`. Tidak butuh API key atau login.
- **Apple Music**: membaca `serialized-server-data` di halaman playlist publik, mengambil nama playlist dari header dan lagu dari section `trackLockup` (artis dari `artistName`, cadangan `subtitleLinks`).
- **YouTube Music**: link `music.youtube.com/playlist?list=...` dibaca langsung lewat API internal (paling akurat, tanpa pencarian).
- Tiap lagu Spotify/Apple dicarikan padanannya di YouTube Music (maksimal 150 lagu, jeda 120 ms per pencarian agar tidak membanjiri server), lalu disimpan sebagai playlist lokal. Progres ditampilkan (“Mencari lagu… 12 dari 50”) dan hasilnya melaporkan berapa lagu yang berhasil ditemukan.
- Parser bersifat murni (teks → model) sehingga bisa diuji. `PlaylistImportTest` (10 test) memakai contoh HTML asli yang dipendekkan; kalau Spotify/Apple mengubah struktur halaman, CI yang gagal lebih dulu, bukan pengguna.

### Checklist uji manual Fase 3
1. Pustaka → **Musik di perangkat** → izinkan akses audio. Daftar lagu muncul; ketuk satu lagu, lalu “Putar semua” dan “Acak”.
2. Kunci layar saat lagu lokal berbunyi — kontrol notifikasi muncul dan judul/artis benar.
3. Pustaka → **Impor playlist dari link** → tempel link playlist Spotify publik → tunggu progres → “Buka playlist” → lagu tersimpan.
4. Ulangi dengan link Apple Music, lalu dengan link `music.youtube.com/playlist?list=…` (harus tanpa proses pencarian, langsung jadi).
5. Tempel link asal-asalan (`https://example.com/foo`) — harus muncul pesan “Link tidak dikenali”, bukan crash.

## Fase 2 — Fitur pemutaran & lirik

### Kualitas audio (baru)
- Tiga pilihan di Pengaturan → **Pemutaran**: **Tinggi** (bitrate tertinggi yang tersedia), **Otomatis** (sekitar 160 kbps, seimbang), **Hemat** (bitrate terendah).
- Pilihan ini benar-benar dipakai saat resolusi stream: `StreamResolver.audioUrl()` sekarang menerima kualitas dan cache-nya dipisah per kualitas (URL 256 kbps tidak dipakai ulang di mode hemat).
- **Hemat data di seluler**: kalau aktif dan perangkat memakai jaringan berbayar (bukan Wi-Fi), kualitas otomatis turun ke Hemat. Wi-Fi tetap memakai pilihan pengguna. Butuh izin baru `ACCESS_NETWORK_STATE`.
- Penyebab: sebelumnya kualitas tidak bisa dipilih sama sekali — aplikasi selalu mengambil bitrate tertinggi.

### Geser timing lirik (baru)
- Di layar lirik sekarang ada kontrol **− / +** (langkah 0,25 detik, maksimal ±5 detik) plus tombol **Reset**. Muncul hanya ketika liriknya tersinkron.
- Geseran disimpan **per lagu** (`LyricsOffsetStore`), jadi tidak mengubah lagu lain.
- Ketuk-baris untuk seek ikut dikoreksi dengan geseran yang sama, jadi seek tetap jatuh di baris yang benar.
- Aturan batas dan langkah ada di `LyricsOffset` (fungsi murni, ada unit test).

### Lewati bagian hening (baru)
- Sakelar di Pengaturan → Pemutaran; memakai `ExoPlayer.skipSilenceEnabled`.
- Perubahan berlaku langsung tanpa menutup aplikasi, karena `PlaybackService` sekarang mengamati preferensi.

### Test
- `LyricsOffsetTest` (5 test) + `AudioStreamSelectorTest` (8 test) dijalankan otomatis di CI.

### Checklist uji manual Fase 2
1. Pengaturan → Pemutaran: ganti kualitas ke Hemat, putar lagu baru, pastikan tetap berbunyi.
2. Aktifkan "Hemat data di seluler", matikan Wi-Fi, putar lagu baru — kualitas harus turun; nyalakan Wi-Fi, putar lagu lain — kualitas kembali.
3. Buka lirik lagu yang tersinkron, tekan + beberapa kali; sorotan lirik harus bergeser. Pindah lagu lalu kembali — geseran harus tetap tersimpan. Tekan Reset.
4. Nyalakan "Lewati bagian hening", putar lagu dengan intro hening panjang.

## Fase 1 — Fondasi (build, mesin stream, ikon)

### Platform & build
- `compileSdk`/`targetSdk` 34 → **35** (Android 15), AGP 8.5.2 → **8.7.3**.
- **R8/minify dinyalakan** untuk build release (`isMinifyEnabled = true`). Resource shrinking tetap mati sampai ada waktu menguji perilaku runtime.
- `proguard-rules.pro` diperluas: keep untuk model Gson (favorit, playlist, snapshot, backup), enum yang disimpan sebagai String, dan seluruh paket NewPipeExtractor + Rhino — tanpa ini data pengguna bisa hilang atau pemutaran gagal tanpa pesan error.
- Workflow `check.yml` sekarang juga menjalankan `assembleRelease` di branch, jadi masalah keep-rule R8 ketahuan **sebelum** push ke main.

### Mesin stream
- `NewPipeExtractor` v0.26.5 → **v0.27.1** (rilis 10 Nov 2026).
- `Media3` 1.4.1 → **1.9.4**.
- Pemilihan stream dipindah ke `AudioStreamSelector` (fungsi murni, ada unit test): semua kandidat audio-only dipertimbangkan, bukan hanya `PROGRESSIVE_HTTP`, dengan prioritas bitrate tertinggi lalu codec Opus/WebM.
- Resolusi URL sekarang **retry 2x** dengan pembuangan cache di antaranya; error asli ikut disertakan di pesan supaya penyebabnya kelihatan.
- Disiapkan `AudioQuality` (HIGH/NORMAL/LOW) di lapisan data — UI-nya menyusul di Fase 2.

### Ikon
- Adaptive icon sebenarnya (`mipmap-anydpi-v26`) + lapisan **monochrome** untuk themed icon Android 13+, plus PNG legasi semua densitas (mdpi–xxxhdpi) untuk Android 7–8.
- Ikon lama memakai hijau Spotify (#1ED760) padahal aksen aplikasi terakota (#E06D47) — sekarang diseragamkan ke terakota dengan latar not krem (#F5F1EC).

### Checklist uji manual Fase 1
1. Install APK rilis baru di atas versi lama (keystore sama) — pastikan data favorit/playlist/snapshot masih ada (uji keep-rule R8).
2. Putar 5 lagu berbeda; perhatikan tidak ada "Audio tidak tersedia" baru.
3. Buat playlist, tambah lagu, tutup paksa, buka lagi — pastikan playlist utuh.
4. Ekspor backup JSON, impor lagi.
5. Lihat ikon di launcher: di Android 13+ coba aktifkan "Themed icons" dan pastikan ikon ikut tema.


## Perubahan
- Layar pemutar: tombol Antrean untuk melihat daftar, memilih lagu, mengubah urutan dan menghapus lagu lain. Bila shuffle aktif urutan putar dapat berbeda dari urutan daftar.
- Sleep timer 15/30/45/60 menit, dapat dibatalkan atau diganti, berjalan di layanan pemutar ketika layar Activity ditutup. Timer dibatalkan jika layanan atau proses dihentikan; penjadwalan Android dapat tertunda saat perangkat tidur.
- Tombol coba ulang ketika pemutaran gagal; URL audio yang di-cache untuk lagu tersebut dibuang sebelum mencoba lagi.
- Pembatalan permintaan radio diteruskan dengan benar.
- Workflow pemeriksaan build debug dan unit test berjalan untuk branch dan pull request, tanpa menerbitkan APK.
- Workflow GitHub Actions menolak rilis ketika secret penandatanganan hilang, bukan diam-diam memakai debug key.

## Build dan rilis
Buka proyek di Android Studio dengan JDK 17 dan Android SDK sesuai `compileSdk`. Build debug dengan `./gradlew :app:assembleDebug`. Sebelum push ke main, siapkan empat secrets yang tertera di README. Gunakan keystore yang **sama** dengan APK terpasang; APK dengan tanda tangan berbeda tidak dapat meng-update instalasi lama secara langsung. Jangan hapus instalasi lama sebelum menyelamatkan data lokal.

## Checklist uji manual (belum dijalankan)
1. Putar playlist dan buka Antrean; pilih lagu, pindahkan ke atas/bawah, hapus lagu lain, coba dengan shuffle aktif.
2. Atur sleep timer, kembali dari layar player atau minimalkan aplikasi, pastikan musik dijeda saat waktu habis. Uji ubah dan batalkan timer.
3. Putuskan koneksi atau gunakan lagu yang gagal; pulihkan koneksi dan tekan Coba putar lagi.
4. Pilih lagu lain saat radio masih dimuat; pastikan antrean lama tidak masuk ke lagu baru.
5. Periksa build debug dan build release di CI, termasuk percobaan tanpa secrets dan pembaruan APK di perangkat memakai keystore yang sama.

## Update koleksi dan pemutaran
- Ekspor atau impor backup JSON di layar Koleksi; impor menggabungkan data, berdasarkan ID playlist dan ID lagu, tanpa menimpa nama lokal. File dibatasi 5 MB dan divalidasi sebelum koleksi diubah. Backup hanya metadata, tidak termasuk audio.
- Layanan pemutar menyimpan antrean (maksimum 500 lagu sekitar lagu aktif), lagu dan posisi terakhir setiap lima detik saat memainkan lagu dan pada perubahan penting. Saat aplikasi dibuka kembali, antrean dipulihkan dalam keadaan jeda. Posisi mungkin tertinggal beberapa detik bila proses dihentikan mendadak; urutan acak saat dipulihkan bisa berbeda.
- Menu titik tiga pada baris lagu memiliki Putar berikutnya dan Tambahkan ke antrean. Putar berikutnya akan mematikan acak agar urutannya sesuai pilihan.

Uji perangkat: ekspor, hapus/tambah data dan impor; uji file rusak dan terlalu besar; pilih lagu, jeda atau putar, tutup paksa lalu buka ulang dan lanjutkan; coba menambah lagu saat acak aktif dan saat antrean kosong. Backup JSON harus disimpan di tempat aman karena berisi daftar lagu pengguna.

## Refresh tampilan
- Beranda lebih terstruktur, menampilkan akses lagu aktif dan item yang dibuka lewat pencarian. Placeholder ditampilkan saat memuat rekomendasi.
- Player memakai tint yang disampel dari cover, dengan fallback warna saat cover tidak tersedia; tombol antrean dan timer tetap dapat dijangkau. Mini player dirapikan; favorit tetap tersedia di player penuh.
- Koleksi diberi filter dan menu opsi untuk backup/impor.
- Uji manual di layar kecil, font besar, Android di bawah/di atas 12, cover gagal dimuat, kondisi offline, pembacaan layar, player, mini player, antrean, timer, lirik, filter koleksi, serta backup/impor.
- GitHub Actions menyimpan APK debug sebagai artifact selama 7 hari. APK debug biasanya bertanda tangan berbeda dari APK rilis: gunakan perangkat atau profil terpisah untuk tes, jangan uninstall APK rilis jika data lokal belum dibackup.

## Update UI Liquid Glass & Editorial (gaya Auralys)
- **Dua tampilan**: **Liquid Glass** (default, permukaan kaca buram dengan blur backdrop nyata via Haze di Android 12+ dan fallback ringan di Android 7–11) dan **Editorial** (matte obsidian hangat dengan garis tipis tegas). Bisa diganti kapan saja lewat ikon Pengaturan di Beranda atau Pustaka.
- **Navbar lensa kaca**: navbar melayang dengan indikator lensa bercahaya yang bisa diketuk atau digeser (drag) secara horizontal antar tab, serta opsi melipat otomatis menjadi tombol lingkaran saat scroll dengan mini player menempel di sebelahnya.
- **Tema ikut cover**: palet warna aksen & kilau ambient latar otomatis beradaptasi dengan warna sampul lagu yang sedang diputar (fallback terakota hangat bila sampul netral atau opsi dimatikan).
- **Beranda baru**: chip filter suasana (Bersantai, Senang, Pesta, Perjalanan, Fokus, Galau), grid **Panggilan cepat** 3×3 dengan kartu dadu acak cepat & indikator halaman, serta **Pilihan cepat** dengan tombol Putar semua dan sheet Riwayat putar.
- **Pemutar & Lirik Editorial**: header "SEDANG DIPUTAR" dengan judul miring, bingkai ganda sampul album, tombol Play/Pause squircle terakota, seek bar modern, dock 4 aksi bawah (**LIRIK • ANTREAN • BAGIKAN • WAKTU TIDUR**), lirik tersinkron bergaya serif rata tengah, serta **Kartu Bagikan Lagu** (ekspor gambar PNG siap Story atau bagikan teks).
- **Gaya huruf & saklar iOS**: 4 pilihan gaya huruf (Modern, Editorial, Klasik Serif, Studio) dan saklar kaca pegas di Pengaturan Tampilan.

## Studio Poster & Kartu Foto Liquid Glass
- **Akses global**: buka **Studio Poster & Kartu** dari tombol **POSTER** di layar Pemutar/Lirik, atau dari menu titik tiga (`⋮`) di lagu mana pun (Beranda, Cari, Pustaka, Album, Playlist, Artis).
- **Mode Kartu Foto Liquid Glass (ala Auralys)**:
  - **Pilih foto sendiri dari Galeri** sebagai latar kanvas, atau pilih latar bawaan / **Transparan (Stiker PNG)** untuk ditempel ke Story & TikTok.
  - **Kartu Pemutar Kaca Frosted** lengkap dengan sampul album, label `~ Musikku`, judul + ikon hati, artis, seekbar + waktu berjalan/sisa, tombol kontrol (`⏮ ⏸ ⏭`), slider volume, refraksi blur backdrop asli, dan kilau sudut bintang (`✦`).
  - **Kolase 1, 3, atau 5 Kartu Melayang**: geser kartu langsung dengan jari, atur kemiringan & ukuran, atau ganti lagu tiap kartu dari Favorit, Riwayat, dan Pencarian YouTube Music.
  - **Efek Kedalaman "Subjek di Depan" (AI Subject Segmentation)**: otomatis memisahkan orang/subjek di foto sehingga kartu pemutar kaca tampak melayang di belakang subjek!
- **Mode Poster Lagu & Poster Lirik**: sampul berbingkai + strip 5 palet warna, pilih hingga 4 baris lirik, 4 gaya latar (Liquid Glass, Editorial, Obsidian, Cover Blur), 3 rasio resolusi tinggi (9:16, 4:5, 1:1), dan kustomisasi font/ketebalan/rata/warna teks.
- **Ekspor**: langsung **Simpan ke Galeri** (`Pictures/Musikku`), **Bagikan PNG** ke Instagram Story/TikTok/WhatsApp, atau bagikan teks.
