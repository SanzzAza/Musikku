# Catatan update Musikku

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
