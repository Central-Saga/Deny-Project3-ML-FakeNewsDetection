\# Fake News Detection



Project Machine Learning untuk melakukan klasifikasi artikel berita menjadi \*\*FAKE\*\* atau \*\*REAL\*\* menggunakan teknik Text Classification.



## Dataset

Aplikasi menggunakan dua sumber dataset untuk melatih kedua model bahasa:

### 1. English: ISOT Fake News Dataset
- **Sumber**: Information Security and Object Technology (ISOT) Research Lab, University of Victoria.
- **Sitasi**: Ahmed H, Traore I, Saad S. *"Detection of Online Fake News Using N-Gram Analysis and Machine Learning Techniques."* In: Intelligent, Secure, and Dependable Systems in Distributed and Cloud Environments. Springer (2017).
- **Lisensi**: Non-Commercial Research & Educational Use.
- **Distribusi Data**:
  - `FAKE`: 23.481 artikel (situs hoaks yang diverifikasi PolitiFact & Wikipedia, 2016–2017)
  - `REAL`: 21.417 artikel (artikel resmi dari Reuters.com, 2016–2017)
  - Total data awal: 44.898 artikel (setelah cleaning: 44.680 artikel)
  - Pembagian: 80% Training (35.744 artikel) dan 20% Testing (8.936 artikel / 8.980 evaluasi produksi).

### 2. Indonesian: Indonesian Political Dataset
- **Sumber**: Kompilasi artikel berita politik nasional dari arsip verifikasi cek fakta (TurnBackHoax / MAFINDO) dan portal berita nasional kredibel (Antara, Detik, Kompas).
- **Cakupan**: Khusus ranah wacana politik Indonesia (`"scope": "political_news"`).
- **Lisensi**: Open Research Data (NLP Research).
- **Distribusi Data**:
  - `REAL (0)`: 3.000 artikel berita politik faktual
  - `FAKE (1)`: 3.000 artikel klaim politik hoaks/menyesatkan
  - Total: 6.000 artikel (Split 80:20 → 4.800 Training, 1.200 Testing terstratifikasi seimbang 50%:50%).

Petunjuk pengunduhan dan penataan dataset tersedia di [`data/README.md`](data/README.md).

## Tahapan Machine Learning

1. Pengumpulan Dataset
2. Pelabelan Data
3. Data Cleaning
4. Text Preprocessing
5. Analisis Distribusi Data
6. Train-Test Split (80% Train, 20% Test)
7. Ekstraksi Fitur TF-IDF
8. Training Model (Linear SVM)
9. Evaluasi Model & Uji Paritas
10. Prediksi Berita Baru (ONNX Runtime)



\## Text Representation



Metode yang digunakan:



\*\*TF-IDF (Term Frequency - Inverse Document Frequency)\*\*



Jumlah fitur:



50.000 fitur.



## Metrik Evaluasi Model Produksi (Artefak ONNX + JSON)

Berikut adalah metrik resmi dari model artefak ONNX yang digunakan langsung oleh aplikasi Android (tersimpan di `android-app/app/src/main/assets/metrics.json` dan dihasilkan ulang melalui `python src/evaluate.py`):

### 1. English Model v1.3 (`fake_news_model_en_v13.onnx`)
- **Arsitektur**: Linear SVM dengan Dual TF-IDF (100.000 fitur kata n-gram (1,2) + 50.000 fitur karakter n-gram (3,5))
- **Total Sampel Uji**: 8.980 data testing (ISOT dataset)
- **Uncertain Threshold**: `±0.20`
- **Coverage**: 88,42% (prediksi tegas REAL/FAKE di luar zona uncertain)
- **Accuracy (Confident)**: **98,92%**
- **F1-Score (Confident)**: **98,89%**
- **Confusion Matrix (Confident)**:
  | | Prediksi FAKE (0) | Prediksi REAL (1) |
  |---|---|---|
  | **Aktual FAKE (0)** | 4.320 | 52 |
  | **Aktual REAL (1)** | 41 | 4.127 |
- **Uji Paritas Python vs Kotlin (ONNX)**: **20/20 PASS** (selisih skor max 0.0, toleransi 1e-3)

### 2. Indonesian Political Model v3 (`fake_news_model_id_political_v3.onnx`)
- **Arsitektur**: Linear SVM dengan Word TF-IDF (100.000 fitur kata n-gram (1,2))
- **Total Sampel Uji**: 1.200 data testing berita politik Indonesia
- **Decision Threshold**: `0.10`
- **Accuracy**: **94,17%**
- **F1-Score**: **93,88%**
- **Confusion Matrix**:
  | | Prediksi FAKE (0) | Prediksi REAL (1) |
  |---|---|---|
  | **Aktual FAKE (0)** | 570 | 30 |
  | **Aktual REAL (1)** | 40 | 560 |
- **Uji Paritas Python vs Kotlin (ONNX)**: **10/10 PASS** (selisih skor max 0.0, toleransi 1e-3)

---

## Eksperimen Awal Notebook (Model .pkl & Baseline)

Metrik berikut adalah hasil eksperimen awal pada notebook (`fake_news_detection.ipynb`) menggunakan TF-IDF 50.000 fitur tunggal untuk perbandingan algoritma dasar:

### Multinomial Naive Bayes
- Accuracy: 93.49%
- Precision: 93.55%
- Recall: 92.67%
- F1-Score: 93.11%

### Logistic Regression
- Accuracy: 97.94%
- Precision: 97.61%
- Recall: 98.07%
- F1-Score: 97.84%

### Linear SVM
- Accuracy: 98.95%
- Precision: 98.85%
- Recall: 98.94%
- F1-Score: 98.89%

## Final Model

Berdasarkan hasil perbandingan algoritma pada data testing, Linear SVM memperoleh performa tertinggi dan dipilih sebagai model dasar yang kemudian diekspor menjadi artefak ONNX untuk aplikasi Android.



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
├── .github/
│   └── workflows/
│       └── android.yml       ← GitHub Actions CI (unit tests & lint)
├── android-app/              ← modul Android
│   ├── gradlew               ← Unix wrapper (mode 100755, eol=lf)
│   ├── gradlew.bat           ← Windows wrapper
│   ├── gradle/wrapper/
│   │   └── gradle-wrapper.jar
│   ├── app/                  ← application module
│   └── build.gradle.kts
├── data/                     ← dataset ISOT & parity fixtures
├── docs/                     ← dokumentasi & screenshots
├── src/                      ← pipeline training, export, evaluation ML
├── .gitattributes            ← menjaga mode 100755 & eol gradlew
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

### Lokasi APK Debug

```
android-app/app/build/outputs/apk/debug/app-debug.apk
```

---

### Build Release, Signing & R8 Minify

Build tipe `release` telah dikonfigurasi dengan **R8 Code Shrinking** (`isMinifyEnabled = true`), **Resource Shrinking** (`isShrinkResources = true`), dan **Release Keystore Signing** otomatis.

#### 1. Buat Keystore Rilis (Local / CI)

> [!CAUTION]
> **JANGAN PERNAH** me-commit file `.jks`, `.keystore`, atau file `keystore.properties` yang berisi credential asli ke repositori Git. File-file tersebut sudah diabaikan di `.gitignore`.

Jalankan perintah berikut untuk membuat keystore baru:

```bash
keytool -genkeypair -v \
  -keystore release-key.jks \
  -alias fakenews-release-key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storetype JKS
```

Simpan file `release-key.jks` di direktori yang aman (misalnya di folder `android-app/` lokal atau path absolut di luar repo).

#### 2. Konfigurasi `keystore.properties`

Salin template `keystore.properties.example` ke `keystore.properties`:

**Windows (PowerShell):**
```powershell
Copy-Item keystore.properties.example android-app\keystore.properties
```

**Linux / macOS:**
```bash
cp keystore.properties.example android-app/keystore.properties
```

Isi kredensial sesuai keystore yang telah dibuat:

```properties
storeFile=release-key.jks
storePassword=password_keystore_anda
keyAlias=fakenews-release-key
keyPassword=password_key_anda
```

*Catatan: Gradle mendeteksi `keystore.properties` baik di direktori `android-app/` maupun di root repository. Jika `storeFile` berupa path relatif, path dihitung relatif terhadap folder tempat `keystore.properties` berada.*

#### 3. Eksekusi Build Release

**Windows (PowerShell):**
```powershell
cd android-app
.\gradlew.bat --no-daemon clean assembleRelease
```

**Linux / macOS:**
```bash
cd android-app
./gradlew --no-daemon clean assembleRelease
```

> [!NOTE]
> Jika `assembleRelease` dijalankan tanpa konfigurasi `keystore.properties`, Gradle akan **fail-fast** dengan pesan error yang jelas dan instruksi pembuatan credential. Untuk build non-release (`assembleDebug`, unit tests), `keystore.properties` tidak diwajibkan sehingga build lokal & CI tetap berjalan mulus.

#### 4. Lokasi APK Release

Hasil build berupa APK yang sudah ditandatangani (signed):
```
android-app/app/build/outputs/apk/release/app-release.apk
```

#### 5. Verifikasi Signature APK (`apksigner`)

Verifikasi penandatanganan APK menggunakan tool `apksigner` dari Android SDK Build-Tools:

```bash
apksigner verify --verbose --print-certs android-app/app/build/outputs/apk/release/app-release.apk
```

Hasil verifikasi yang valid:
- `Verifies: true`
- `Verified using v2 scheme (APK Signature Scheme v2): true`
- Informasi sertifikat signer (DN, SHA-256 digest, key algorithm RSA 2048).

#### 6. Optimasi R8 Minify & ProGuard Rules

Aplikasi memanfaatkan runtime ONNX (`ai.onnxruntime:onnxruntime-android`) dan model ML offline. Pada mode release:
- **DEX Code Shrinking**: Ukuran file bytecode DEX berkurang drastis dari ~12.7 MB (4 multi-dex pada mode debug) menjadi hanya **~952 KB** (1 file `classes.dex` tunggal pada release).
- **Resource Shrinking**: Menghapus resource dan layout yang tidak terpakai (`isShrinkResources = true`).
- **Keep Rules (`proguard-rules.pro`)**:
  - Mempertahankan seluruh class runtime ONNX (`ai.onnxruntime.**`) beserta native JNI methods agar inferensi offline C++ tetap berfungsi sempurna.
  - Mempertahankan model scorer dan preprocessor (`FakeNewsClassifier`, `OnnxSvmScorer`, `TextPreprocessor`) untuk menjamin paritas inferensi model EN & ID.

---

### Continuous Integration (CI)

Repository dilengkapi dengan GitHub Actions workflow di `.github/workflows/android.yml`:
- **Trigger**: Setiap `push` ke `main`, branch `fix/**`, `feat/**`, dan `pull_request` ke `main`.
- **Environment**: Ubuntu Latest dengan JDK 21 (Temurin) dan Gradle Cache.
- **Tasks**: Menjalankan unit tests (`testDebugUnitTest`) dan static analysis (`lint`) secara otomatis pada sub-proyek `android-app/`.

### Verification

- Build Release Signed: `app-release.apk` berhasil dibuat via `./gradlew assembleRelease`.
- Signature Verification: Lolos verifikasi `apksigner verify` dengan APK Signature Scheme v2.
- Minify / Code Shrink: Berhasil memangkas DEX ~92% tanpa merusak runtime ONNX maupun inferensi bahasa EN dan ID.
- Git Working Tree: Bersih dari artefak `.jks` dan credential release.
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

---

## Keterbatasan & Kapan Jangan Dipakai (Model Limitations)

Sebagai proyek Machine Learning yang bertanggung jawab (*Responsible AI*), bagian ini merinci keterbatasan intrinsik dari model, representasi data, dan kondisi di mana model **tidak boleh** digunakan.

### 1. Leakage & Artefak Penerbit pada Dataset ISOT (English Model)
- **Publisher Bias**: Pada dataset ISOT, seluruh berita `REAL` berasal dari satu kantor berita resmi (*Reuters.com*) dengan konvensi penulisan terstruktur dan dateline khas (mis. *"WASHINGTON (Reuters) -"*), sedangkan berita `FAKE` berasal dari berbagai situs tidak resmi dengan gaya penulisan bebas.
- **Dampak**: Model Linear SVM dengan Dual TF-IDF 150.000 fitur (100k kata + 50k karakter) memisahkan kelas berdasarkan gaya editorial penerbit (*writing style & artifacts*), bukan karena memverifikasi kebenaran fakta yang dikandung berita.
- **Konsekuensi**: Akurasi **98,92%** pada dataset ISOT **tidak mencerminkan kemampuan mendeteksi hoaks baru di alam liar**. Berita faktual modern dari portal selain Reuters dapat dengan mudah salah diklasifikasikan sebagai FAKE.

### 2. Batasan Domain Spesifik (Model Indonesian Political v3)
- **Domain Tertutup**: Model Indonesia v3 dilatih secara spesifik pada artikel wacana politik (`"scope": "political_news"`).
- **Out-of-Distribution (OOD)**: Artikel di luar ranah politik (seperti kesehatan, resep makanan, olahraga, sains, hiburan, teknologi, atau gaya hidup) **berada di luar domain pelatihan**. Model akan memaksakan prediksi biner (REAL atau FAKE) pada topik tersebut, menghasilkan kesimpulan yang tidak valid.

### 3. Deteksi Bahasa Berbasis Heuristik
- Klasifikasi bahasa menggunakan metode pencocokan kata kunci frekuensi tinggi (`indonesianHints` dan `englishHints`).
- **Risiko Salah Jalur**:
  - Teks dengan percampuran bahasa (*code-mixing* atau bahasa gaul) dapat salah diarahkan ke model yang keliru.
  - Teks yang sangat pendek (< 5 kata) tanpa kata kunci bahasa sering kali seri dan dialihkan secara default ke model Indonesia.

### 4. Batasan Panjang Teks Minimum
- Representasi TF-IDF memerlukan jumlah token dan variasi n-gram yang memadai untuk dinormalisasi L2.
- Teks yang hanya berupa judul singkat (1–4 kata) tanpa tubuh berita dapat menghasilkan vektor sangat renggang atau didominasi oleh satu kata bias (mis. kata *"Breaking"*), memicu skor ekstrim yang tidak merepresentasikan isi berita sebenarnya.
- **Rekomendasi**: Masukkan judul lengkap beserta minimal 2–3 paragraf isi berita.

### 5. Ambang Keputusan & Zona UNCERTAIN
- **English v1.3**: Menggunakan ambang `uncertain_threshold = ±0.20`.
  - Skor dalam rentang `[-0.20, +0.20]` diberi label `UNCERTAIN` demi menjaga precision tinggi pada prediksi tegas (coverage ~88,4%).
- **Indonesian v3**: Menggunakan `threshold = 0.10` (skor $\ge 0.10 \to$ FAKE, $< 0.10 \to$ REAL).
  - Skor yang berada sangat dekat dengan `0.10` (misalnya `0.09` vs `0.11`) memiliki margin keyakinan statistik yang tipis.

### 6. Contoh Kasus Prediksi Salah (Prediction Failures)

Tabel berikut menunjukkan 3 contoh nyata kasus kegagalan prediksi pada model ONNX yang terpasang di aplikasi untuk memberikan ekspektasi yang realistis kepada pengguna:

| No | Teks Input | Label Aktual | Prediksi Model (Skor) | Penjelasan Penyebab Kegagalan |
|---|---|---|---|---|
| **1** | *"Breaking news updates from the local community event yesterday."* | **REAL (1)** | **FAKE (0)**<br>(Skor: `-1.8020`) | **ISOT Publisher Leakage**: Berita lokal nyata berbahasa Inggris tanpa format resmi Reuters. Frasa sensasional *"Breaking news"* sangat diasosiasikan dengan berita palsu pada dataset ISOT, sehingga model memprediksinya sebagai FAKE. |
| **2** | *"Resep cara membuat nasi goreng enak dan lezat di rumah"* | **REAL (1)** | **FAKE (0)**<br>(Skor: `+0.7263`) | **Out-of-Domain (Non-Politik)**: Teks resep masakan dipaksa dievaluasi oleh model Political v3. Kosakata kuliner (*resep, enak, lezat*) tidak ada dalam wacana politik latih, menyebabkan skor terdistorsi ke kelas hoaks. |
| **3** | *"Breaking news"* | **REAL (1)** | **FAKE (0)**<br>(Skor: `-3.4817`) | **Teks Terlalu Pendek**: Teks 2 kata tanpa isi paragraf. Model tidak memiliki token penyeimbang untuk normalisasi TF-IDF, sehingga bobot kata *"Breaking"* mendominasi secara mutlak ke arah FAKE. |

### 7. Penafian Penggunaan (Bukan Fact-Checker)

> [!WARNING]
> **PENTING: Aplikasi ini BUKAN alat verifikasi fakta (Fact-Checker).**
> - Prediksi model dihasilkan secara **offline** murni berdasarkan kemiripan pola teks statistik dengan dataset latih historis.
> - Model **tidak memiliki koneksi internet** dan **tidak mengetahui peristiwa dunia nyata terkini**.
> - Jangan pernah menggunakan hasil analisis aplikasi sebagai satu-satunya dasar pengambilan keputusan hukum, jurnalistik, atau sosial. Lakukan selalu verifikasi silang ke sumber berita kredibel atau lembaga pemeriksa fakta resmi (seperti CekFakta, MAFINDO, PolitiFact, atau Reuters Fact Check).

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

## Reproduksi Artefak Model (Issue #13)

Aplikasi Android menggunakan dua pasang artefak ONNX + JSON:

| Artefak | Ukuran | Keterangan |
|---|---|---|
| `fake_news_model_en_v13.onnx` | ~587 KB | LinearSVC EN, dual TF-IDF |
| `fake_news_model_en_v13.json` | ~8,2 MB | vocab + idf + coef + metrics |
| `fake_news_model_id_political_v3.onnx` | ~391 KB | LinearSVC ID, single TF-IDF |
| `fake_news_model_id_political_v3.json` | ~4,4 MB | vocab + idf + coef |

### Prasyarat

```bash
pip install -r requirements-src.txt   # Python 3.10+, semua versi dipin
```

### Langkah Regenerasi (dari clone bersih)

```bash
# 1. Siapkan dataset
#    EN  : Fake.csv + True.csv dari ISOT → taruh di data/
#    ID  : id_political.csv              → taruh di data/
#    Lihat data/README.md untuk sumber dataset.

# 2. Latih model (.pkl)
python src/train.py --data-dir data/ --out-dir models/

# 3. Ekspor ke ONNX + JSON
python src/export_android.py --model-dir models/ \
    --out-dir android-app/app/src/main/assets/

# 4. Evaluasi artefak ONNX + uji paritas Python vs Kotlin
python src/evaluate.py --data-dir data/ \
    --assets-dir android-app/app/src/main/assets/
# Output: android-app/app/src/main/assets/metrics.json

# 5. Uji paritas dan unit test Kotlin di Android
cd android-app && ./gradlew test
```

### Struktur File ML & Uji Paritas

```
src/
├── train.py              # Latih LinearSVC EN + ID dari dataset mentah -> .pkl
├── export_android.py     # Konversi .pkl -> ONNX + JSON (format aplikasi Android)
├── evaluate.py           # Evaluasi ONNX, confusion matrix, uji paritas Python vs Kotlin
├── generate_golden.py    # Generator golden fixture paritas preprocessing sklearn <-> Kotlin
└── parity_fixtures.json  # Fixture skor Kotlin ter-commit untuk uji paritas (toleransi 1e-3)
data/
├── .gitkeep
├── README.md             # Petunjuk download dataset
├── Fake.csv              # (tidak di-commit — terlalu besar)
├── True.csv              # (tidak di-commit)
├── id_political.csv      # (tidak di-commit)
└── parity_fixtures.json  # Salinan fixture ter-commit
fake_news_detection.ipynb # Notebook naratif yang memanggil pipeline src/
models/                   # Output train.py — tidak di-commit
requirements-src.txt      # Dependensi terpin untuk src/
android-app/app/src/
├── main/java/com/example/fakenewsdetection/
│   ├── FakeNewsClassifier.kt  # Integrasi model & delegasi preprocessing
│   └── TextPreprocessor.kt   # Preprocessing murni tanpa Context (JVM-testable)
├── test/
│   ├── java/com/example/fakenewsdetection/
│   │   ├── FakeNewsClassifierTest.kt # 17 unit test preprocessing & scoring fixture
│   │   ├── GoldenParityTest.kt       # 7 golden test (30 teks: clean text, n-gram, TF-IDF)
│   │   └── TextPreprocessorTest.kt   # 13 unit test murni TF-IDF, empty input, threshold
│   └── resources/
│       └── golden_preprocessing.json # 30 sampel golden reference dari scikit-learn
└── androidTest/java/com/example/fakenewsdetection/
    └── FakeNewsClassifierInstrumentedTest.kt # Pengujian on-device dengan model ONNX asli
```

### Catatan Evaluasi Machine Learning

Ringkasan perbandingan metrik eksperimen awal notebook vs artefak ONNX produksi aplikasi:

| Model Artefak | Format | Fitur TF-IDF | Sampel Uji | Accuracy | F1-Score | Status Uji Paritas Kotlin |
|---|---|---|---|---|---|---|
| **Eksperimen Awal (Notebook)** | `.pkl` | 50.000 (Word) | 8.936 | 98,95% | 98,89% | N/A (Colab only) |
| **English v1.3 (Produksi)** | `.onnx` + `.json` | 100k Word + 50k Char | 8.980 | **98,92%** (confident) | **98,89%** | **20/20 PASS** (tol 1e-3) |
| **Indonesian Political v3 (Produksi)** | `.onnx` + `.json` | 100k Word | 1.200 | **94,17%** | **93,88%** | **10/10 PASS** (tol 1e-3) |

Metrik resmi artefak ONNX tersimpan di `android-app/app/src/main/assets/metrics.json` yang dihasilkan `src/evaluate.py` setelah proses regenerasi dan evaluasi.

Aplikasi melakukan klasifikasi berdasarkan pola teks statistik offline, bukan verifikasi kebenaran berita secara real-time. Untuk pemahaman mendalam mengenai batasan domain, publisher bias ISOT, dan ambang keputusan, lihat bagian [Keterbatasan & Kapan Jangan Dipakai](#keterbatasan--kapan-jangan-dipakai-model-limitations).

