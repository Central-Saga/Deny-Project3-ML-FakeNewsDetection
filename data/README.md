# Dataset & Sumber Data

Folder ini adalah tempat menaruh dataset sebelum menjalankan `src/train.py`.

File dataset tidak di-commit ke repository Git karena ukurannya yang besar dan dikecualikan oleh `.gitignore`.

---

## 1. English (ISOT Fake News Dataset)

- **Sumber & Laboratorium**: Information Security and Object Technology (ISOT) Research Lab, University of Victoria, Canada.
- **Sitasi / Paper**: Ahmed H, Traore I, Saad S. *"Detection of Online Fake News Using N-Gram Analysis and Machine Learning Techniques."* In: Intelligent, Secure, and Dependable Systems in Distributed and Cloud Environments. Springer (2017).
- **URL Resmi**: [ISOT Fake News Dataset](https://onlineacademiccommunity.uvic.ca/isot/2022/11/27/fake-news-detection-datasets/)
- **Lisensi**: Riset Akademis / Educational & Research Non-Commercial Use.

### Distribusi Data
| File | Label | Sumber Asal | Jumlah Baris Awal | Setelah Cleaning |
|---|---|---|---|---|
| `data/Fake.csv` | `0` (FAKE) | Artikel hoaks/berita palsu diverifikasi PolitiFact & Wikipedia (2016–2017) | 23.481 | 23.447 |
| `data/True.csv` | `1` (REAL) | Artikel berita resmi dari Reuters.com (2016–2017) | 21.417 | 21.233 |
| **Total** | | | **44.898** | **44.680** |

- **Split Evaluasi**: 80% Training (35.744 artikel) dan 20% Testing (8.936 artikel / 8.980 evaluasi model produksi).
- **Kolom yang dibutuhkan**: `title`, `text`

### Catatan Penting Mengenai Karakteristik Dataset (Publisher Leakage)
Seluruh berita `REAL` pada dataset ISOT bersumber dari satu penerbit (*Reuters*) dengan format dan dateline khas (mis. `WASHINGTON (Reuters) -`), sementara berita `FAKE` berasal dari beragam situs tidak resmi. Model Linear SVM dapat memanfaatkan gaya editorial ini (*publisher bias*) untuk pemisahan kelas, sehingga akurasi tinggi pada test set ISOT (98,92%) tidak secara langsung mencerminkan kemampuan mendeteksi hoaks baru di luar gaya penerbitan tersebut.

---

## 2. Indonesian Political Dataset

- **Sumber**: Kompilasi artikel berita politik berbahasa Indonesia dari arsip verifikasi cek fakta (TurnBackHoax / MAFINDO) untuk kelas hoaks dan portal berita nasional kredibel (Antara, Kompas, Detik) untuk kelas faktual.
- **Cakupan / Domain**: `"scope": "political_news"` (khusus ranah politik Indonesia).
- **Lisensi**: Open Research Data untuk pemrosesan bahasa alami (NLP).

### Distribusi Data
| Subset | Kelas REAL (0) | Kelas FAKE (1) | Total |
|---|---|---|---|
| Training (80%) | 2.400 (50%) | 2.400 (50%) | 4.800 |
| Testing (20%) | 600 (50%) | 600 (50%) | 1.200 |
| **Total** | **3.000 (50%)** | **3.000 (50%)** | **6.000** |

- **Kolom yang dibutuhkan**: `text`, `label` (`0` = REAL, `1` = FAKE).
- **Catatan Domain**: Model ini ditujukan khusus untuk wacana politik. Berita non-politik (kesehatan, olahraga, kuliner, gaya hidup) berada di luar domain latih (*out-of-distribution*).

---

## File Paritas Ter-commit
Untuk keperluan uji paritas otomatis tanpa mengharuskan pengunduhan dataset berukuran besar di lingkungan CI:
- `data/parity_fixtures.json` (dan salinannya di `src/parity_fixtures.json`): berisi 20 sampel uji EN dan 10 sampel uji ID beserta skor paritas terverifikasi.

