# Issue #3 - End-to-End Functional Test

## Tujuan

Memastikan fitur utama Fake News Detection berjalan
menggunakan input pengguna dan model Machine Learning asli,
tanpa mock atau hasil prediksi yang di-hardcode.

GitHub Issue: #3
Pengujian: Manual pada Android Emulator
Tanggal: 20 Oktober 2026

## Alur Utama

1. Pengguna membuka halaman Deteksi.
2. Pengguna memasukkan judul dan isi berita.
3. Aplikasi mendeteksi bahasa secara otomatis.
4. Teks diproses menggunakan TF-IDF.
5. ONNX Runtime menjalankan model Linear SVM.
6. Aplikasi menampilkan hasil klasifikasi.
7. Hasil disimpan melalui LocalHistoryManager.
8. Pengguna membuka halaman Riwayat.
9. Data tetap tersedia setelah aplikasi dibuka ulang.

## Model yang Digunakan

- English v1.3: fake_news_model_en_v13.onnx
- Indonesian Political v3: fake_news_model_id_political_v3.onnx

Kedua model menjalankan inferensi secara offline.

## Hasil Pengujian Manual

| Skenario | Hasil | Status |
|---|---|---|
| Input judul dan isi berita | Dapat digunakan | PASS |
| Deteksi berita Indonesia | FAKE NEWS | PASS |
| Deteksi berita Inggris | UNCERTAIN | PASS |
| Inferensi menggunakan ONNX | Berhasil | PASS |
| Hasil klasifikasi ditampilkan | Berhasil | PASS |
| Riwayat prediksi tersimpan | Berhasil | PASS |
| Riwayat ditampilkan kembali | Berhasil | PASS |
| Riwayat bertahan setelah aplikasi dibuka ulang | Berhasil | PASS |
| Navigasi aplikasi | Berjalan tanpa crash | PASS |

Catatan: Pengujian dilakukan secara manual.
Hasil FAKE NEWS dan UNCERTAIN adalah keluaran model
pada contoh pengujian, bukan verifikasi kebenaran berita.

## Penyimpanan Data

Riwayat disimpan secara lokal menggunakan SharedPreferences
dalam format JSON.

Informasi yang disimpan:

- ID
- Judul berita
- Isi berita
- Label prediksi
- Skor prediksi
- Waktu analisis

Riwayat dibatasi hingga maksimal 50 entri.

## Komponen Utama

- MainActivity.kt
- FakeNewsClassifier.kt
- OnnxSvmScorer.kt
- LocalHistoryManager.kt
- HistoryActivity.kt

## Definition of Done

Satu alur end-to-end telah diuji:

Input berita -> Proses ONNX -> Hasil klasifikasi
-> Penyimpanan lokal -> Tampilan riwayat

Hasil pengujian manual menunjukkan bahwa fitur inti
berjalan menggunakan input dan keluaran model asli.

## Kesimpulan

Alur utama aplikasi berhasil dijalankan pada Android Emulator.

Pengujian ini membuktikan fungsionalitas dasar,
bukan akurasi faktual seluruh prediksi atau
kelulusan pengujian otomatis.
