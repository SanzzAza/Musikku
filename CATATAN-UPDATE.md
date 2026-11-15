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
