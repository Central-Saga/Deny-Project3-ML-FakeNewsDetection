\# Fake News Detection



Project Machine Learning untuk melakukan klasifikasi artikel berita menjadi \*\*FAKE\*\* atau \*\*REAL\*\* menggunakan teknik Text Classification.



\## Dataset



Dataset yang digunakan adalah \*\*Fake and Real News Dataset (ISOT)\*\*.



Jumlah data awal:

\- FAKE: 23.481 artikel

\- REAL: 21.417 artikel

\- Total: 44.898 artikel



Setelah proses cleaning, jumlah data yang digunakan adalah 44.680 artikel.



\## Tahapan Machine Learning



1\. Pengumpulan Dataset

2\. Pelabelan Data

3\. Data Cleaning

4\. Text Preprocessing

5\. Analisis Distribusi Data

6\. Train-Test Split

7\. TF-IDF

8\. Training Model

9\. Evaluasi Model

10\. Prediksi Berita Baru



\## Pembagian Dataset



\- Training: 80%

\- Testing: 20%



Jumlah:

\- Training: 35.744 data

\- Testing: 8.936 data



\## Text Representation



Metode yang digunakan:



\*\*TF-IDF (Term Frequency - Inverse Document Frequency)\*\*



Jumlah fitur:



50.000 fitur.



\## Model yang Dibandingkan



\### Multinomial Naive Bayes



\- Accuracy: 93.49%

\- Precision: 93.55%

\- Recall: 92.67%

\- F1-Score: 93.11%



\### Logistic Regression



\- Accuracy: 97.94%

\- Precision: 97.61%

\- Recall: 98.07%

\- F1-Score: 97.84%



\### Linear SVM



\- Accuracy: 98.95%

\- Precision: 98.85%

\- Recall: 98.94%

\- F1-Score: 98.89%



\## Final Model



Berdasarkan hasil evaluasi pada data testing, Linear SVM memperoleh performa tertinggi pada eksperimen ini.



\## Technology



\- Python

\- Google Colab

\- Pandas

\- NumPy

\- Scikit-learn

\- Matplotlib

\- TF-IDF

\- Git

\- GitHub



\## Notebook



Notebook utama:



`fake\_news\_detection.ipynb`



\## Label



\- `0` = FAKE

\- `1` = REAL

## Android Debug Build

### Requirements

- JDK 21 atau JDK yang kompatibel dengan konfigurasi Gradle
- Android SDK (termasuk platform SDK 36.1)
- Gradle Wrapper 9.3.1 (sudah tersedia dalam repository)

### Struktur Modul

```
Deny-Project3-Final/          ← root repo
├── android-app/              ← modul Android
│   ├── gradlew               ← Unix wrapper (mode 100755, eol=lf)
│   ├── gradlew.bat           ← Windows wrapper
│   ├── gradle/wrapper/
│   │   └── gradle-wrapper.jar
│   ├── app/                  ← application module
│   └── build.gradle.kts
├── backend/
├── docs/
├── .gitattributes            ← menjaga mode & eol gradlew
└── README.md
```

### Build dari Root Repository (Cara yang Direkomendasikan)

**Windows (PowerShell / CMD):**

```powershell
git clone https://github.com/Central-Saga/Deny-Project3-ML-FakeNewsDetection.git
cd Deny-Project3-ML-FakeNewsDetection\android-app
.\gradlew.bat --no-daemon clean assembleDebug
```

**Linux / macOS:**

```bash
git clone https://github.com/Central-Saga/Deny-Project3-ML-FakeNewsDetection.git
cd Deny-Project3-ML-FakeNewsDetection/android-app
# gradlew sudah executable (mode 100755) — tidak perlu chmod manual
./gradlew --no-daemon clean assembleDebug
```

> **Catatan:** File `android-app/gradlew` disimpan dengan mode `100755` di Git
> dan dijaga oleh `.gitattributes` (`eol=lf`). Setelah fresh clone di
> Linux/macOS, file langsung bisa dieksekusi tanpa `chmod +x`.

### Konfigurasi Android SDK

Buat file `android-app/local.properties` (tidak di-commit ke Git):

```properties
sdk.dir=/path/to/android/sdk
```

Atau ekspor environment variable:

```bash
export ANDROID_HOME=/path/to/android/sdk
```

### Lokasi APK

```
android-app/app/build/outputs/apk/debug/app-debug.apk
```

### Verification

Fresh clone build berhasil pada Windows 11.
Build: SUCCESSFUL (33 detik).
APK: berhasil dibuat.
Git working tree: clean.
---

## Android Offline Machine Learning Inference

Aplikasi Android Fake News Detection menggunakan model Machine Learning
asli berbasis TF-IDF dan Linear Support Vector Machine (Linear SVM).

Inferensi model dijalankan secara offline menggunakan ONNX Runtime
Android, tanpa memerlukan koneksi internet atau API eksternal.

### Model yang Digunakan

Aplikasi mendukung dua model:

1. English v1.3
   - Model: fake_news_model_en_v13.onnx
   - TF-IDF Word: 100.000 fitur
   - TF-IDF Character: 50.000 fitur
   - Algoritma: Linear SVM
   - Mendukung hasil UNCERTAIN

2. Indonesian Political v3
   - Model: fake_news_model_id_political_v3.onnx
   - TF-IDF Word: 100.000 fitur
   - Algoritma: Linear SVM
   - Terutama ditujukan untuk berita politik Indonesia

Kedua model ONNX disimpan di:

`android-app/app/src/main/assets/`

### Arsitektur Inferensi

Alur prediksi aplikasi:

1. Pengguna memasukkan judul dan isi berita.
2. Aplikasi mendeteksi bahasa secara otomatis.
3. Teks diproses menggunakan preprocessing yang sesuai.
4. Aplikasi membentuk vektor TF-IDF menggunakan vocabulary dan IDF dari JSON.
5. ONNX Runtime menjalankan inferensi model Linear SVM.
6. Skor prediksi diubah menjadi label FAKE, REAL, atau UNCERTAIN sesuai aturan model.
7. Hasil ditampilkan dan disimpan dalam riwayat lokal.

### Implementasi Android

Bahasa pemrograman: Kotlin

Komponen utama:

- `FakeNewsClassifier.kt`: preprocessing, TF-IDF, deteksi bahasa, dan klasifikasi.
- `OnnxSvmScorer.kt`: pemanggilan model melalui ONNX Runtime.
- `MainActivity.kt`: input pengguna, prediksi, dan pengelolaan lifecycle model.

Pemuatan model dan inferensi dilakukan melalui background worker
agar tidak memblokir UI Android.

### Label Prediksi

Label yang digunakan aplikasi:

- `0` = FAKE NEWS
- `1` = REAL NEWS
- `-1` = UNCERTAIN

Model Indonesian Political v3 memiliki pemetaan label internal
yang berbeda, sehingga output dikonversi menjadi label aplikasi.

### Pengujian Integrasi

Pengujian pada Android Emulator:

- Android debug build berhasil.
- Model Indonesia berhasil melakukan prediksi.
- Model Inggris berhasil melakukan prediksi.
- Hasil pada dua contoh pengujian sesuai dengan implementasi JSON sebelumnya.
- Navigasi aplikasi berjalan tanpa crash pada pengujian akhir.

Catatan: hasil evaluasi pada bagian awal README berasal dari
eksperimen Machine Learning sebelumnya dan tidak otomatis
merepresentasikan akurasi kedua model ONNX yang digunakan aplikasi.

Prediksi model adalah klasifikasi berdasarkan pola teks,
bukan verifikasi fakta secara real-time.
---

## Fitur Aplikasi Android

FakeNews AI adalah aplikasi Android berbasis Kotlin untuk
klasifikasi teks berita menggunakan Machine Learning offline.

Fitur utama:

- Deteksi otomatis bahasa Indonesia dan Inggris.
- Prediksi menggunakan TF-IDF, Linear SVM, dan ONNX Runtime.
- Menampilkan hasil FAKE NEWS, REAL NEWS, atau UNCERTAIN.
- Menyimpan hasil analisis dalam riwayat lokal.
- Menampilkan kembali riwayat tanpa koneksi internet.
- Validasi input dan penanganan kesalahan.

## Screenshot Aplikasi

### Beranda
![Beranda FakeNews AI](docs/screenshots/01-home.png)

### Form Deteksi
![Form Deteksi Berita](docs/screenshots/02-detection.png)

### Hasil Analisis
![Hasil Analisis Berita](docs/screenshots/03-result.png)

### Riwayat
![Riwayat Hasil Analisis](docs/screenshots/04-history.png)

## Cara Menjalankan Aplikasi

1. Clone repository dari GitHub.
2. Buka folder android-app menggunakan Android Studio.
3. Pastikan Android SDK dan JDK sudah terkonfigurasi.
4. Lakukan Gradle Sync.
5. Jalankan aplikasi menggunakan emulator atau perangkat Android.
6. Masukkan judul dan isi berita pada halaman Deteksi.
7. Tekan Analisis Berita untuk menjalankan model ONNX.
8. Buka halaman Riwayat untuk melihat hasil yang tersimpan.

Perintah build dari folder android-app:

    .\gradlew.bat --no-daemon clean assembleDebug

Lokasi APK:

    android-app/app/build/outputs/apk/debug/app-debug.apk

## Skenario Demonstrasi

Panduan demonstrasi tersedia di:

[Demo Scenario](docs/demo-scenario.md)

## Status Backend

Direktori `backend/` (FastAPI + PostgreSQL) telah **dihapus dari repository**
pada ronde audit produksi (Issue #11, Oktober 2026).

**Alasan penghapusan:**

- Aplikasi Android menyimpan riwayat deteksi secara **lokal** menggunakan
  `LocalHistoryManager.kt` — tidak ada klien HTTP (Retrofit/OkHttp) dan
  tidak ada izin `INTERNET` di `AndroidManifest.xml`.
- README sendiri menyatakan aplikasi berjalan
  _"tanpa memerlukan koneksi internet atau API eksternal"_.
- Backend tidak pernah dipanggil oleh komponen mana pun → **dead code**.
- Seluruh endpoint tidak memiliki autentikasi, sehingga menimbulkan risiko
  keamanan (IDOR, data dump tanpa batas) bila tidak sengaja di-deploy.

**Arsitektur riwayat saat ini:**

```
Input Pengguna → ONNX Inference (offline) → LocalHistoryManager (SQLite lokal)
```

Tidak ada backend eksternal yang digunakan oleh aplikasi.

## Catatan Evaluasi Machine Learning

Metrik dataset ISOT pada bagian awal README merupakan hasil
eksperimen awal. Angka tersebut tidak otomatis menjadi hasil
evaluasi model English v1.3 dan Indonesian Political v3.

Aplikasi melakukan klasifikasi berdasarkan pola teks,
bukan verifikasi kebenaran berita secara real-time.

