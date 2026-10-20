# Skenario Demo FakeNews AI

Referensi jadwal demo mentor: 17 Oktober 2026, 13:00 WITA.

## Persiapan

- Android Emulator sudah aktif.
- Aplikasi FakeNews AI terpasang.
- Kedua model ONNX tersedia secara lokal.
- Siapkan dua contoh berita untuk pengujian.

## Alur Demonstrasi

### 1. Beranda

Buka aplikasi FakeNews AI.
Perkenalkan tujuan aplikasi dan fitur utamanya.

### 2. Deteksi Indonesia

Buka halaman Deteksi.

Judul:
Pemerintah Mengumumkan Program Pembangunan Infrastruktur Baru

Isi:
Pemerintah Indonesia mengumumkan rencana pembangunan
infrastruktur baru untuk meningkatkan akses transportasi
masyarakat. Menteri terkait menyampaikan bahwa program
tersebut akan dilaksanakan secara bertahap dengan
melibatkan pemerintah daerah dan lembaga terkait.

Tekan Analisis Berita.

Hasil pada pengujian sebelumnya: FAKE NEWS.

### 3. Deteksi Inggris

Judul:
Government Announces New Education Development Program

Isi:
The government announced a new education development
program to improve learning facilities in rural areas.
Officials stated that the initiative will provide
additional educational resources, teacher training,
and technology support for public schools.

Tekan Analisis Berita.

Hasil pada pengujian sebelumnya: UNCERTAIN.

### 4. Riwayat

Buka halaman Riwayat.
Tunjukkan judul, isi, waktu, dan label hasil analisis.
Tutup dan buka kembali aplikasi untuk menunjukkan
bahwa riwayat tetap tersimpan.

### 5. Mode Offline

Aktifkan Airplane Mode.
Lakukan analisis berita kembali.
Tunjukkan bahwa model ONNX dapat bekerja tanpa internet.

### 6. Validasi Input

Coba analisis dengan judul atau isi berita kosong.
Tunjukkan pesan validasi tanpa crash.

## Penutup

Jelaskan bahwa aplikasi menggunakan:
- Kotlin dan Android Studio.
- TF-IDF dan Linear SVM.
- ONNX Runtime untuk inferensi offline.
- SharedPreferences untuk penyimpanan riwayat.

Disclaimer:
Hasil AI adalah prediksi berdasarkan pola teks,
bukan verifikasi fakta secara langsung.

Catatan:
Skenario ini disusun sebagai panduan demo berdasarkan
kebutuhan mentor dan hasil pengujian manual.
