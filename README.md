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

### Build Instructions

1. Clone repository.
2. Masuk ke folder `android-app`.
3. Konfigurasi Android SDK melalui `ANDROID_HOME` atau
   `local.properties` (file ini tidak disimpan di Git).
4. Jalankan:

   Windows:
   `.\gradlew.bat --no-daemon clean assembleDebug`

5. APK debug tersedia di:
   `android-app/app/build/outputs/apk/debug/app-debug.apk`

### Verification

Fresh clone build berhasil pada Windows 11.
Build: SUCCESSFUL (33 detik).
APK: berhasil dibuat.
Git working tree: clean.