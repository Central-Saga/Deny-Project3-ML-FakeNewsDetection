# train.py — latih model EN v1.3 dan ID Political v3 dari dataset mentah
# Dipanggil oleh: python src/train.py --data-dir data/ --out-dir models/
# Output: models/svm_en.pkl, models/tfidf_word_en.pkl, models/tfidf_char_en.pkl
#         models/svm_id.pkl, models/tfidf_id.pkl

import argparse
import os
import joblib
import numpy as np
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.svm import LinearSVC
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, f1_score
from scipy.sparse import hstack

# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def parse_args():
    p = argparse.ArgumentParser(
        description="Train Fake News Detection models (EN v1.3 + ID Political v3)"
    )
    p.add_argument(
        "--data-dir",
        default="data",
        help="Folder berisi Fake.csv + True.csv (EN) dan id_political.csv (ID). Default: data/",
    )
    p.add_argument(
        "--out-dir",
        default="models",
        help="Folder output artefak .pkl. Default: models/",
    )
    p.add_argument(
        "--seed",
        type=int,
        default=42,
        help="Random seed untuk reproducibility.",
    )
    return p.parse_args()


# ---------------------------------------------------------------------------
# English model v1.3
# ---------------------------------------------------------------------------

def train_english(data_dir: str, out_dir: str, seed: int):
    print("\n=== [EN] Melatih model English v1.3 ===")

    fake_path = os.path.join(data_dir, "Fake.csv")
    true_path = os.path.join(data_dir, "True.csv")

    if not os.path.exists(fake_path) or not os.path.exists(true_path):
        raise FileNotFoundError(
            f"Dataset tidak ditemukan di {data_dir}/. "
            "Download ISOT dataset: https://onlineacademiccommunity.uvic.ca/isot/ "
            "dan letakkan Fake.csv + True.csv di folder --data-dir."
        )

    fake = pd.read_csv(fake_path)
    true = pd.read_csv(true_path)
    fake["label"] = 0  # FAKE
    true["label"] = 1  # REAL

    df = pd.concat([fake, true], ignore_index=True)

    # Gabungkan title + text sebagai input
    df["text"] = (df["title"].fillna("") + " " + df["text"].fillna("")).str.strip()
    df = df.dropna(subset=["text"]).reset_index(drop=True)

    print(f"  Total data: {len(df):,} (FAKE={df['label'].eq(0).sum():,}, REAL={df['label'].eq(1).sum():,})")

    X_train, X_test, y_train, y_test = train_test_split(
        df["text"], df["label"], test_size=0.2, random_state=seed, stratify=df["label"]
    )

    print("  Melatih TF-IDF word (100k fitur, sublinear_tf)...")
    tfidf_word = TfidfVectorizer(
        analyzer="word",
        ngram_range=(1, 2),
        max_features=100_000,
        sublinear_tf=True,
        norm="l2",
    )
    X_train_word = tfidf_word.fit_transform(X_train)
    X_test_word = tfidf_word.transform(X_test)

    print("  Melatih TF-IDF char (50k fitur, sublinear_tf)...")
    tfidf_char = TfidfVectorizer(
        analyzer="char_wb",
        ngram_range=(2, 4),
        max_features=50_000,
        sublinear_tf=True,
        norm="l2",
    )
    X_train_char = tfidf_char.fit_transform(X_train)
    X_test_char = tfidf_char.transform(X_test)

    X_train_combined = hstack([X_train_word, X_train_char])
    X_test_combined = hstack([X_test_word, X_test_char])

    print("  Melatih LinearSVC (C=1.0)...")
    svm = LinearSVC(C=1.0, max_iter=2000, random_state=seed)
    svm.fit(X_train_combined, y_train)

    y_pred = svm.predict(X_test_combined)
    acc = accuracy_score(y_test, y_pred)
    f1 = f1_score(y_test, y_pred)
    print(f"  Accuracy: {acc:.5f}  |  F1: {f1:.5f}")

    os.makedirs(out_dir, exist_ok=True)
    joblib.dump(tfidf_word, os.path.join(out_dir, "tfidf_word_en.pkl"))
    joblib.dump(tfidf_char, os.path.join(out_dir, "tfidf_char_en.pkl"))
    joblib.dump(svm, os.path.join(out_dir, "svm_en.pkl"))
    print(f"  Artefak tersimpan di: {out_dir}/")
    return {"accuracy": round(acc, 5), "f1": round(f1, 5)}


# ---------------------------------------------------------------------------
# Indonesian Political model v3
# ---------------------------------------------------------------------------

def train_indonesian(data_dir: str, out_dir: str, seed: int):
    print("\n=== [ID] Melatih model Indonesian Political v3 ===")

    id_path = os.path.join(data_dir, "id_political.csv")

    if not os.path.exists(id_path):
        raise FileNotFoundError(
            f"Dataset tidak ditemukan: {id_path}. "
            "Letakkan file id_political.csv (kolom: text, label) di folder --data-dir."
        )

    df = pd.read_csv(id_path)
    # Ekspektasi kolom: text (str), label (0=REAL, 1=FAKE)
    df = df.dropna(subset=["text", "label"]).reset_index(drop=True)
    df["text"] = df["text"].astype(str).str.strip()

    print(f"  Total data: {len(df):,}")

    X_train, X_test, y_train, y_test = train_test_split(
        df["text"], df["label"].astype(int),
        test_size=0.2, random_state=seed, stratify=df["label"]
    )

    print("  Melatih TF-IDF word (100k fitur, ngram 1-2)...")
    tfidf_id = TfidfVectorizer(
        analyzer="word",
        ngram_range=(1, 2),
        max_features=100_000,
        sublinear_tf=True,
        norm="l2",
        lowercase=True,
        token_pattern=r"(?u)\b\w\w+\b",  # min 2 char
    )
    X_train_vec = tfidf_id.fit_transform(X_train)
    X_test_vec = tfidf_id.transform(X_test)

    print("  Melatih LinearSVC (C=1.0)...")
    svm_id = LinearSVC(C=1.0, max_iter=2000, random_state=seed)
    svm_id.fit(X_train_vec, y_train)

    y_pred = svm_id.predict(X_test_vec)
    acc = accuracy_score(y_test, y_pred)
    f1 = f1_score(y_test, y_pred)
    print(f"  Accuracy: {acc:.5f}  |  F1: {f1:.5f}")

    os.makedirs(out_dir, exist_ok=True)
    joblib.dump(tfidf_id, os.path.join(out_dir, "tfidf_id.pkl"))
    joblib.dump(svm_id, os.path.join(out_dir, "svm_id.pkl"))
    print(f"  Artefak tersimpan di: {out_dir}/")
    return {"accuracy": round(acc, 5), "f1": round(f1, 5)}


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    args = parse_args()
    metrics_en = train_english(args.data_dir, args.out_dir, args.seed)
    metrics_id = train_indonesian(args.data_dir, args.out_dir, args.seed)
    print("\n=== TRAINING SELESAI ===")
    print(f"EN  → Accuracy: {metrics_en['accuracy']}  F1: {metrics_en['f1']}")
    print(f"ID  → Accuracy: {metrics_id['accuracy']}  F1: {metrics_id['f1']}")
    print(f"\nJalankan src/export_android.py untuk menghasilkan ONNX + JSON.")
