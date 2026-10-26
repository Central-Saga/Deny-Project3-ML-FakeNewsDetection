# Dataset

Folder ini adalah tempat menaruh dataset sebelum menjalankan src/train.py.

## English (ISOT Dataset)

Download dari: https://onlineacademiccommunity.uvic.ca/isot/2022/11/27/fake-news-detection-datasets/

Letakkan di folder ini:
- data/Fake.csv  — artikel berita FAKE (23.481 baris)
- data/True.csv  — artikel berita REAL (21.417 baris)

Kolom yang dibutuhkan: 	itle, 	ext

## Indonesian Political

Dataset berita politik Indonesia.

Letakkan di folder ini:
- data/id_political.csv

Kolom yang dibutuhkan: 	ext, label (0=REAL, 1=FAKE)

---

File dataset tidak di-commit ke repo (terlalu besar, dikecualikan oleh .gitignore).
