# generate_golden.py — golden fixture paritas preprocessing Python <-> Kotlin (Issue #17)
#
# Penggunaan:
#   python src/generate_golden.py
#
# Output:
#   android-app/app/src/test/resources/golden_preprocessing.json
#
# Referensi yang dipakai:
#   - Cleaning  : clean_english_text / clean_indonesian_text di src/evaluate.py
#                 (satu-satunya implementasi cleaning Python di repo).
#   - Fitur     : analyzer ASLI scikit-learn (TfidfVectorizer.build_analyzer),
#                 BUKAN salinan manual logika Kotlin. Dengan begitu golden test
#                 mengukur paritas Kotlin terhadap perilaku sklearn yang dipakai
#                 saat model dilatih (word 1-2 gram, char_wb 3-5 gram).
#   - TF-IDF    : sublinear_tf (1 + ln tf) * idf, normalisasi L2, memakai
#                 vocabulary + idf dari artefak JSON yang dikirim ke aplikasi.
#
# Jalankan ulang skrip ini setiap kali aturan preprocessing atau artefak model
# berubah, lalu commit file JSON hasilnya.

import json
import os
import sys
from collections import Counter

import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from evaluate import clean_english_text, clean_indonesian_text  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ASSETS = os.path.join(ROOT, "android-app", "app", "src", "main", "assets")
OUT_PATH = os.path.join(
    ROOT, "android-app", "app", "src", "test", "resources", "golden_preprocessing.json"
)

TOKEN_PATTERN = r"(?u)\b\w\w+\b"

# 15 teks EN — termasuk kasus tepi yang rawan drift regex/tokenisasi.
ENGLISH_TEXTS = [
    "WASHINGTON (Reuters) - The White House announced a new trade deal on Monday.",
    "BREAKING: Aliens land in Washington DC!!! Government CONFIRMS everything...",
    "Read more at https://example.com/news?id=42 or www.fake-site.org today",
    "<p>Federal Reserve <b>raises</b> interest rates by 0.25 percent</p>",
    "Inflation dropped 25% in 2026 across all 50 states, officials said.",
    "It's a well-known fact: vaccines don't contain microchips.",
    "U.S. Senate passes bill; President Trump's veto expected.",
    "Doctors say drinking bleach cures COVID-19 - experts strongly disagree",
    "Hello,world!This-is_a test/with*punctuation&no=spaces",
    "   Multiple    spaces\tand\nnewlines   everywhere   ",
    "Stock markets rally as tech giants report record profits",
    "Reuters reported that reuters-style datelines should be removed",
    "Scientists discover new species of deep-sea fish near the Pacific trench",
    "Pope Francis visits the Vatican museum with 1,000 pilgrims",
    "A",
]

# 15 teks ID — termasuk boilerplate cek fakta, URL, email, domain, nama penulis.
INDONESIAN_TEXTS = [
    "Presiden Joko Widodo meresmikan jalan tol baru di Jawa Tengah pada Senin (12/10).",
    "Beredar kabar pemerintah membagikan bantuan uang tunai 100 juta rupiah lewat WhatsApp!",
    "Hasil penelusuran Tim Cek Fakta menunjukkan klaim tersebut adalah hoaks.",
    "Baca juga: https://turnbackhoax.id/2023/01/01/salah-foto-viral dan www.kompas.com",
    "Hubungi redaksi@detik.com atau kunjungi portal tempo.co.id untuk info lebih lanjut.",
    "Penulis: Bentang Febrylian | Editor: Julnis Firmansyah | Sumber: Kompas",
    "Berdasarkan hasil penelusuran, narasi yang dibagikan oleh akun Facebook itu keliru.",
    "Foto hoaks memperlihatkan menteri ditangkap KPK di bandara Soekarno-Hatta.",
    "DPR menyetujui RUU Cipta Kerja dalam rapat paripurna, Senin 5/10/2020.",
    "Kementerian Kesehatan mengimbau masyarakat waspada terhadap DBD & ISPA.",
    "Dengan demikian, klaim tersebut masuk ke dalam kategori konten yang menyesatkan.",
    "Gubernur BI: \"Stabilitas nilai tukar rupiah tetap terjaga\" kata reporter kami.",
    "KPU umumkan hasil pemilu 2024 secara resmi di Jakarta",
    "Minum air rebusan daun cabai dapat menyembuhkan stroke secara instan!!!",
    "",
]


def load_json(name):
    with open(os.path.join(ASSETS, name), encoding="utf-8") as f:
        return json.load(f)


def tfidf_sparse(counts, vocabulary, idf):
    """Kembalikan (indices terurut, values) vektor TF-IDF sublinear + L2."""
    entries = {}
    for feat, c in counts.items():
        idx = vocabulary.get(feat)
        if idx is not None:
            entries[idx] = (1.0 + np.log(float(c))) * idf[idx]
    norm = float(np.sqrt(sum(v * v for v in entries.values())))
    if norm <= 0.0:
        return [], []
    idxs = sorted(entries)
    return idxs, [round(entries[i] / norm, 7) for i in idxs]


def main():
    cfg_en = load_json("fake_news_model_en_v13.json")
    cfg_id = load_json("fake_news_model_id_political_v3.json")

    en_stop = sorted(cfg_en["word"]["stop_words"])
    en_word_an = TfidfVectorizer(
        analyzer="word", ngram_range=(1, 2), lowercase=False,
        token_pattern=TOKEN_PATTERN, stop_words=en_stop,
    ).build_analyzer()
    en_char_an = TfidfVectorizer(
        analyzer="char_wb", ngram_range=(3, 5), lowercase=False,
    ).build_analyzer()
    id_word_an = TfidfVectorizer(
        analyzer="word", ngram_range=(1, 2), lowercase=False,
        token_pattern=TOKEN_PATTERN,
    ).build_analyzer()

    english = []
    for text in ENGLISH_TEXTS:
        cleaned = clean_english_text(text)
        word_counts = Counter(en_word_an(cleaned))
        char_counts = Counter(en_char_an(cleaned))
        w_idx, w_val = tfidf_sparse(word_counts, cfg_en["word"]["vocabulary"], cfg_en["word"]["idf"])
        c_idx, c_val = tfidf_sparse(char_counts, cfg_en["char"]["vocabulary"], cfg_en["char"]["idf"])
        english.append({
            "input": text,
            "clean_text": cleaned,
            "word_counts": dict(sorted(word_counts.items())),
            "char_counts": dict(sorted(char_counts.items())),
            "word_tfidf": {"indices": w_idx, "values": w_val},
            "char_tfidf": {"indices": c_idx, "values": c_val},
        })

    indonesian = []
    for text in INDONESIAN_TEXTS:
        cleaned = clean_indonesian_text(text)
        word_counts = Counter(id_word_an(cleaned))
        idx, val = tfidf_sparse(word_counts, cfg_id["vocabulary"], cfg_id["idf"])
        indonesian.append({
            "input": text,
            "clean_text": cleaned,
            "word_counts": dict(sorted(word_counts.items())),
            "tfidf": {"indices": idx, "values": val},
        })

    out = {
        "generated_by": "src/generate_golden.py",
        "reference": "cleaning=src/evaluate.py; features=sklearn TfidfVectorizer.build_analyzer()",
        "english": english,
        "indonesian": indonesian,
    }
    os.makedirs(os.path.dirname(OUT_PATH), exist_ok=True)
    with open(OUT_PATH, "w", encoding="utf-8", newline="\n") as f:
        json.dump(out, f, ensure_ascii=False, indent=1)
        f.write("\n")
    print(f"Golden fixture: {len(english)} EN + {len(indonesian)} ID -> {OUT_PATH}")


if __name__ == "__main__":
    main()
