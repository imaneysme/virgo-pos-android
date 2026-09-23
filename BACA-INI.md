# Virgo Komputer POS — Proyek Android

Folder ini adalah proyek Android Studio siap-build yang membungkus aplikasi
kasir Virgo Komputer (file HTML) menjadi aplikasi Android asli (.apk), tanpa
memerlukan internet untuk berjalan (kecuali untuk memuat font, yang otomatis
memakai font cadangan jika sedang offline).

Catatan jujur: file .apk **tidak bisa dibuat langsung di sistem saya** karena
di sini tidak tersedia Android SDK / Gradle untuk Android. Tapi proyek ini
sudah lengkap — proses build di komputer Anda hanya perlu 2–3 klik.

## Cara membuat file .apk (± 10–15 menit, gratis)

1. Install **Android Studio** (jika belum ada): https://developer.android.com/studio
2. Buka Android Studio → **Open** → pilih folder proyek ini (`virgo-pos-android`)
3. Tunggu proses "Gradle Sync" selesai (Android Studio otomatis mengunduh
   komponen yang dibutuhkan saat sync pertama kali — pastikan komputer
   terhubung internet saat langkah ini saja). Jika muncul pesan tentang
   "Gradle wrapper", klik **OK / Use default Gradle wrapper** — ini normal
   untuk proyek yang belum menyertakan file wrapper biner.
4. Setelah sync selesai, klik menu **Build → Build App Bundle(s) / APK(s) → Build APK(s)**
5. Setelah selesai, klik notifikasi **"locate"** yang muncul di pojok kanan
   bawah — file APK ada di:
   `app/build/outputs/apk/debug/app-debug.apk`
6. Salin file `app-debug.apk` itu ke HP Android (lewat kabel USB, Google Drive,
   atau WhatsApp ke diri sendiri), lalu buka file-nya di HP untuk menginstal.
   Android mungkin meminta izin "Instal dari sumber tidak dikenal" — aktifkan
   sekali saja untuk file ini.

## Struktur proyek

- `app/src/main/assets/www/index.html` — seluruh aplikasi kasir (tampilan,
  logika, penyimpanan data). Ini file yang sama dengan versi web yang sudah
  dipublikasikan sebelumnya.
- `app/src/main/java/.../MainActivity.kt` — pembungkus Android: menampilkan
  `index.html` dalam layar penuh tanpa address bar, dan mengaktifkan
  penyimpanan data lokal (localStorage) agar produk & transaksi tersimpan
  di HP.
- `app/build.gradle`, `build.gradle`, `settings.gradle` — konfigurasi build
  standar Android (nama paket: `com.virgokomputer.pos`).

## Mengubah data produk setelah jadi APK

Cara termudah adalah tetap menggunakan versi web (link yang sudah dikirim
sebelumnya) untuk mengedit produk melalui menu "Produk" di aplikasi itu
sendiri — datanya tersimpan otomatis di HP, baik di versi web maupun versi
APK, karena keduanya memakai file HTML yang sama.

Jika ingin mengubah daftar produk bawaan (sebelum di-build), edit langsung
bagian `seedProducts()` di dalam `app/src/main/assets/www/index.html`.

## Mengganti ikon aplikasi (opsional)

Proyek ini belum menyertakan ikon kustom (memakai ikon bawaan Android) agar
tetap ringan dan langsung bisa di-build. Untuk memasang logo toko sendiri:
klik kanan folder `app/src/main/res` di Android Studio → **New → Image Asset**,
lalu unggah logo Anda.
