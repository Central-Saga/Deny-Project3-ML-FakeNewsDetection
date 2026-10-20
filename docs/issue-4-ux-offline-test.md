# Issue #4 - UX Permission, Offline, Loading and Error State

## Tujuan

Memastikan aplikasi Fake News Detection tetap berfungsi
tanpa internet serta menyediakan penanganan loading dan error.

## Audit Permission

AndroidManifest.xml tidak mendeklarasikan runtime
permission khusus pada kode aplikasi yang diaudit.

Fitur klasifikasi menggunakan ONNX Runtime secara lokal.
Tidak diperlukan permintaan permission tambahan untuk deteksi.

## Offline Testing

Pengujian manual dilakukan di Android Emulator.

- Airplane Mode diaktifkan.
- Aplikasi berhasil dibuka.
- Fitur Deteksi dapat digunakan.
- Model ONNX tetap menghasilkan prediksi.
- Tidak terjadi crash saat pengujian offline.

Status: PASS

## Loading State

Implementasi saat ini:

- Tombol analisis dinonaktifkan saat model dimuat.
- Teks "Memuat model..." ditampilkan.
- Teks "Menganalisis..." ditampilkan selama prediksi.
- Prediksi diproses melalui background worker.
- Tombol diaktifkan kembali setelah proses selesai.

## Error Handling

Kode aplikasi menyediakan:

- Validasi judul dan isi berita.
- Try-catch pada pemuatan model.
- Try-catch pada proses prediksi.
- Toast untuk menampilkan kesalahan.

Catatan:
Pengujian khusus kegagalan pemuatan model dan
kegagalan inferensi belum dilakukan.

## Kesimpulan

Pengujian offline berhasil tanpa crash.

Tidak ada perubahan kode produksi pada Issue #4.
Pengujian error khusus dan pemeriksaan permission
hasil merge manifest masih perlu dilakukan untuk
memastikan seluruh skenario terpenuhi.

## Hasil Audit Permission APK

Pemeriksaan menggunakan Android dumpsys package:

- INTERNET: granted
- ACCESS_NETWORK_STATE: granted
- DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION: granted
- Runtime permissions: tidak ada

Semua permission tersebut termasuk izin normal atau
internal aplikasi yang tidak membutuhkan dialog runtime.

Pengujian penolakan runtime permission:
NOT APPLICABLE karena tidak ada runtime permission
yang perlu diminta untuk fitur inti.


## Pengujian Validasi Input

Pengujian manual pada Android Emulator:

- Judul berita kosong: PASS
- Isi berita kosong: PASS
- Pesan validasi muncul dengan benar.
- Aplikasi tidak crash.
- Pengguna dapat memperbaiki input.

Status: PASS
