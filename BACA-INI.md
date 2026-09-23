# Virgo Komputer POS — Proyek Android

Proyek Android Studio siap-build yang membungkus aplikasi kasir Virgo Komputer
menjadi aplikasi Android asli (.apk). Sudah dilengkapi:

- 🖨️ **Cetak struk ke printer Bluetooth thermal 58mm** (juga mendukung 80mm)
- ⚠️ **Peringatan stok menipis** otomatis di halaman Kasir, Produk & Laporan
- 📷 **Pindai barcode pakai kamera** (langsung di halaman, dengan cadangan pemindai native jika perlu)
- 💵 Diskon per transaksi, metode Tunai/Non-Tunai, cetak ulang struk dari Riwayat
- 💾 Cadangkan & pulihkan data (semua data tersimpan lokal di HP, tanpa internet/login)

Catatan jujur: file .apk **tidak bisa dibuat langsung di sistem saya** karena
di sini tidak tersedia Android SDK / Gradle untuk Android. Proyek ini sudah
lengkap dan sudah diuji strukturnya — proses build di komputer Anda hanya
perlu beberapa klik di Android Studio.

## Cara membuat file .apk (± 10–15 menit, gratis)

1. Install **Android Studio**: https://developer.android.com/studio
2. Buka Android Studio → **Open** → pilih folder proyek ini (`virgo-pos-android`)
3. Tunggu "Gradle Sync" selesai (perlu internet sekali saat ini — Android
   Studio & Gradle akan mengunduh beberapa komponen, termasuk pustaka
   pemindai barcode ZXing). Jika muncul pesan tentang "Gradle wrapper",
   pilih **Use default Gradle wrapper**.
4. Klik menu **Build → Build App Bundle(s) / APK(s) → Build APK(s)**
5. Klik notifikasi **"locate"** di pojok kanan bawah setelah build selesai.
   File APK ada di: `app/build/outputs/apk/debug/app-debug.apk`
6. Salin file itu ke HP Android (kabel USB / Drive / kirim ke diri sendiri),
   lalu buka untuk menginstal. Aktifkan **"Instal dari sumber tidak dikenal"**
   sekali saja saat diminta.

## Izin yang akan diminta saat pertama kali dibuka

Saat aplikasi pertama kali dijalankan, akan muncul dialog izin:
- **Kamera** — wajib diizinkan agar fitur pindai barcode berfungsi
- **Perangkat di sekitar / Bluetooth** (HP dengan Android 12 ke atas) — wajib
  diizinkan agar aplikasi bisa melihat & terhubung ke printer Bluetooth

Jika tidak sengaja ditolak, izin bisa diaktifkan lagi lewat:
**Pengaturan HP → Aplikasi → Virgo Komputer → Izin**

## Menyiapkan printer Bluetooth 58mm

1. Nyalakan printer, lalu pasangkan (pairing) lebih dulu lewat menu
   **Pengaturan Bluetooth bawaan HP** (di luar aplikasi) — cari nama printer,
   biasanya kode PIN pairing default `0000` atau `1234`.
2. Buka aplikasi Virgo Komputer → ketuk ikon ⚙️ **Pengaturan** di pojok kanan atas
3. Gulir ke bagian **Printer Bluetooth 58mm** → printer yang sudah dipasangkan
   akan muncul di daftar → ketuk untuk memilihnya
4. Ketuk **"Cetak Struk Percobaan"** untuk memastikan koneksi berhasil
5. Atur juga lebar kertas (58mm/80mm), info toko, dan catatan kaki struk di
   bagian atas menu Pengaturan yang sama

Setelah printer dipilih, tombol **"🖨️ Cetak Struk"** akan muncul otomatis di
setiap struk transaksi, dan bisa juga dipakai untuk mencetak ulang struk lama
dari tab **Riwayat**. Aktifkan **"Cetak otomatis"** di Pengaturan bila ingin
struk langsung tercetak setiap transaksi selesai tanpa perlu menekan tombol.

> Catatan teknis: printer terhubung lewat profil Bluetooth klasik (SPP) dan
> perintah ESC/POS standar — cocok untuk hampir semua printer thermal 58mm
> generik yang beredar di pasaran (merk seperti EPPOS, Xprinter/POS58,
> Zjiang, Goojprt, dll).

## Cara kerja pindai barcode

Aplikasi mencoba tiga cara secara berurutan, otomatis:
1. Kamera langsung di dalam halaman (cepat, tanpa membuka layar lain)
2. Jika perangkat tidak mendukung cara di atas → membuka pemindai kamera
   bawaan aplikasi (native, offline, pakai pustaka ZXing)
3. Jika kamera tidak tersedia sama sekali → bisa ketik barcode secara manual

Untuk memanfaatkannya, isi kolom **Barcode** (opsional) saat menambah/mengubah
produk di tab Produk — cukup ketuk tombol **"Pindai"** di formulir untuk
mengisinya otomatis dari hasil scan.

## Struktur proyek

- `app/src/main/assets/www/index.html` — seluruh aplikasi kasir (tampilan,
  logika, printer, scanner, penyimpanan data). File yang sama dengan versi
  web yang sudah dipublikasikan sebelumnya.
- `app/src/main/java/.../MainActivity.kt` — pembungkus Android:
  - menampilkan `index.html` lewat domain virtual aman (`https://appassets.androidplatform.net`)
    agar kamera bisa diakses,
  - jembatan `AndroidPOS` ke JavaScript untuk daftar printer, kirim cetak
    (ESC/POS lewat Bluetooth SPP), dan pemindai barcode native cadangan.
- `app/build.gradle` — dependensi: AndroidX WebKit (WebViewAssetLoader) dan
  ZXing Android Embedded (pemindai barcode offline).

## Mengubah data produk setelah jadi APK

Cara termudah: tetap gunakan menu **Produk** di dalam aplikasi itu sendiri —
tambah/ubah/hapus produk, atur harga, stok, dan barcode langsung dari sana.
Data tersimpan otomatis di HP (baik versi web maupun versi APK memakai
mekanisme penyimpanan yang sama).

Untuk mengubah daftar produk **bawaan** sebelum di-build, edit bagian
`seedProducts()` di `app/src/main/assets/www/index.html`.

## Mengganti ikon aplikasi (opsional)

Proyek ini belum menyertakan ikon kustom (memakai ikon bawaan Android).
Untuk memasang logo toko sendiri: klik kanan folder `app/src/main/res` di
Android Studio → **New → Image Asset**, lalu unggah logo Anda.
