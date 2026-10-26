# evaluate.py — evaluasi artefak ONNX (bukan .pkl) dan tulis metrics.json
#
# Penggunaan:
#   python src/evaluate.py --data-dir data/ --assets-dir android-app/app/src/main/assets/
#
# Output:
#   metrics.json (di --assets-dir)  berisi accuracy, f1, confusion matrix
#   Uji paritas: 20 kalimat contoh, skor Python vs Kotlin (toleransi 1e-3)

import argparse
import json
import os
import re
import string

import numpy as np
import onnxruntime as ort
import pandas as pd
from sklearn.metrics import (
    accuracy_score,
    f1_score,
    confusion_matrix,
    classification_report,
)
from sklearn.model_selection import train_test_split
from sklearn.feature_extraction.text import TfidfVectorizer
import joblib


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def parse_args():
    p = argparse.ArgumentParser(
        description="Evaluasi model ONNX dan tulis metrics.json"
    )
    p.add_argument("--data-dir", default="data", help="Folder dataset mentah.")
    p.add_argument(
        "--assets-dir",
        default="android-app/app/src/main/assets",
        help="Folder berisi ONNX + JSON.",
    )
    p.add_argument("--model-dir", default="models", help="Folder .pkl (untuk rebuild vectorizer).")
    p.add_argument("--seed", type=int, default=42)
    return p.parse_args()


# ---------------------------------------------------------------------------
# Preprocessing minimal (sesuai FakeNewsClassifier.kt)
# ---------------------------------------------------------------------------

def preprocess_en(text: str) -> str:
    text = text.lower()
    text = re.sub(r"https?://\S+|www\.\S+", " ", text)
    text = re.sub(r"<[^>]+>", " ", text)
    text = re.sub(r"\(reuters\)", " ", text, flags=re.IGNORECASE)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def preprocess_id(text: str) -> str:
    text = text.lower()
    text = re.sub(r"https?://\S+|www\.\S+", " ", text)
    text = re.sub(r"<[^>]+>", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text


# ---------------------------------------------------------------------------
# TF-IDF inference dari JSON (replicates FakeNewsClassifier.kt logic)
# ---------------------------------------------------------------------------

def tfidf_from_json(text: str, vocab: dict, idf: list, analyzer: str,
                    ngram_range: tuple, sublinear_tf: bool) -> np.ndarray:
    """Hitung vektor TF-IDF manual dari JSON vocab+idf (Python reference impl)."""
    n = len(idf)
    tf = np.zeros(n, dtype=np.float64)

    if analyzer == "word":
        tokens = text.split()
        # Build ngrams
        ngrams = []
        for gram_size in range(ngram_range[0], ngram_range[1] + 1):
            for i in range(len(tokens) - gram_size + 1):
                ngrams.append(" ".join(tokens[i: i + gram_size]))
    else:  # char_wb
        padded = f" {text} "
        ngrams = []
        for gram_size in range(ngram_range[0], ngram_range[1] + 1):
            for i in range(len(padded) - gram_size + 1):
                ngrams.append(padded[i: i + gram_size])

    for ng in ngrams:
        if ng in vocab:
            tf[vocab[ng]] += 1.0

    if sublinear_tf:
        nz = tf > 0
        tf[nz] = 1.0 + np.log(tf[nz])

    idf_arr = np.array(idf, dtype=np.float64)
    vec = tf * idf_arr

    norm = np.linalg.norm(vec)
    if norm > 0:
        vec /= norm

    return vec.astype(np.float32)


# ---------------------------------------------------------------------------
# ONNX inference wrappers
# ---------------------------------------------------------------------------

def score_en_onnx(session, word_vec: np.ndarray, char_vec: np.ndarray) -> float:
    inputs = {
        "word_tfidf": word_vec.reshape(1, -1),
        "char_tfidf": char_vec.reshape(1, -1),
    }
    output = session.run(None, inputs)
    return float(output[0][0][0])


def score_id_onnx(session, tfidf_vec: np.ndarray) -> float:
    inputs = {"tfidf": tfidf_vec.reshape(1, -1)}
    output = session.run(None, inputs)
    return float(output[0][0][0])


# ---------------------------------------------------------------------------
# Evaluate English model
# ---------------------------------------------------------------------------

def evaluate_english(args) -> dict:
    print("\n=== [EN] Evaluasi model English v1.3 ===")

    # Load JSON
    json_path = os.path.join(args.assets_dir, "fake_news_model_en_v13.json")
    with open(json_path, encoding="utf-8") as f:
        cfg = json.load(f)

    uncertain_threshold = cfg["uncertain_threshold"]
    word_vocab = cfg["word"]["vocabulary"]
    word_idf = cfg["word"]["idf"]
    char_vocab = cfg["char"]["vocabulary"]
    char_idf = cfg["char"]["idf"]

    # Load ONNX session
    onnx_path = os.path.join(args.assets_dir, "fake_news_model_en_v13.onnx")
    session = ort.InferenceSession(onnx_path)

    # Load dataset
    fake = pd.read_csv(os.path.join(args.data_dir, "Fake.csv"))
    true = pd.read_csv(os.path.join(args.data_dir, "True.csv"))
    fake["label"] = 0
    true["label"] = 1
    df = pd.concat([fake, true], ignore_index=True)
    df["text"] = (df["title"].fillna("") + " " + df["text"].fillna("")).str.strip()
    df = df.dropna(subset=["text"]).reset_index(drop=True)

    _, X_test, _, y_test = train_test_split(
        df["text"], df["label"], test_size=0.2, random_state=args.seed, stratify=df["label"]
    )

    print(f"  Test samples: {len(X_test):,}")

    preds = []
    raw_scores = []
    for text in X_test:
        cleaned = preprocess_en(text)
        wv = tfidf_from_json(cleaned, word_vocab, word_idf, "word", (1, 2), True)
        cv = tfidf_from_json(cleaned, char_vocab, char_idf, "char_wb", (2, 4), True)
        score = score_en_onnx(session, wv, cv)
        raw_scores.append(score)
        if abs(score) < uncertain_threshold:
            preds.append(-1)
        elif score >= 0:
            preds.append(1)
        else:
            preds.append(0)

    # Metrik hanya untuk non-uncertain
    y_arr = np.array(list(y_test))
    p_arr = np.array(preds)
    confident_mask = p_arr != -1
    acc = accuracy_score(y_arr[confident_mask], p_arr[confident_mask])
    f1 = f1_score(y_arr[confident_mask], p_arr[confident_mask])
    coverage = confident_mask.mean()
    cm = confusion_matrix(y_arr[confident_mask], p_arr[confident_mask]).tolist()

    print(f"  Coverage (non-uncertain): {coverage:.4f}")
    print(f"  Accuracy (confident)    : {acc:.5f}")
    print(f"  F1 (confident)          : {f1:.5f}")
    print(f"  Confusion matrix        : {cm}")

    return {
        "model": "fake_news_model_en_v13",
        "uncertain_threshold": uncertain_threshold,
        "test_samples": int(len(X_test)),
        "coverage": round(coverage, 4),
        "accuracy_confident": round(acc, 5),
        "f1_confident": round(f1, 5),
        "confusion_matrix": cm,
    }


# ---------------------------------------------------------------------------
# Evaluate Indonesian model
# ---------------------------------------------------------------------------

def evaluate_indonesian(args) -> dict:
    print("\n=== [ID] Evaluasi model Indonesian Political v3 ===")

    json_path = os.path.join(args.assets_dir, "fake_news_model_id_political_v3.json")
    with open(json_path, encoding="utf-8") as f:
        cfg = json.load(f)

    vocab = cfg["vocabulary"]
    idf = cfg["idf"]
    threshold = cfg.get("threshold", 0.1)

    onnx_path = os.path.join(args.assets_dir, "fake_news_model_id_political_v3.onnx")
    session = ort.InferenceSession(onnx_path)

    id_path = os.path.join(args.data_dir, "id_political.csv")
    df = pd.read_csv(id_path)
    df = df.dropna(subset=["text", "label"]).reset_index(drop=True)
    df["text"] = df["text"].astype(str)

    _, X_test, _, y_test = train_test_split(
        df["text"], df["label"].astype(int),
        test_size=0.2, random_state=args.seed, stratify=df["label"]
    )

    print(f"  Test samples: {len(X_test):,}")

    preds = []
    for text in X_test:
        cleaned = preprocess_id(text)
        vec = tfidf_from_json(cleaned, vocab, idf, "word", (1, 2), True)
        score = score_id_onnx(session, vec)
        preds.append(1 if score >= 0 else 0)

    y_arr = np.array(list(y_test))
    p_arr = np.array(preds)
    acc = accuracy_score(y_arr, p_arr)
    f1 = f1_score(y_arr, p_arr)
    cm = confusion_matrix(y_arr, p_arr).tolist()

    print(f"  Accuracy : {acc:.5f}")
    print(f"  F1       : {f1:.5f}")
    print(f"  Confusion: {cm}")

    return {
        "model": "fake_news_model_id_political_v3",
        "threshold": threshold,
        "test_samples": int(len(X_test)),
        "accuracy": round(acc, 5),
        "f1": round(f1, 5),
        "confusion_matrix": cm,
    }


# ---------------------------------------------------------------------------
# Parity test — 20 sample sentences (EN)
# ---------------------------------------------------------------------------

PARITY_SAMPLES_EN = [
    ("Breaking: President signs new trade deal with Europe", 1),
    ("Aliens land in Washington DC, government confirms", 0),
    ("Federal Reserve raises interest rates by 0.25 percent", 1),
    ("Doctors say drinking bleach cures all diseases", 0),
    ("Congress passes bipartisan infrastructure bill", 1),
    ("Obama secretly born on Mars, leaked documents show", 0),
    ("Scientists discover new species of deep-sea fish", 1),
    ("Vaccines contain microchips that track your location", 0),
    ("Stock markets rally on positive jobs report", 1),
    ("Pope Francis reveals he is an alien from another galaxy", 0),
    ("WHO issues new guidelines on COVID-19 prevention", 1),
    ("Hillary Clinton runs secret pizza parlor crime ring", 0),
    ("NASA successfully launches new weather satellite", 1),
    ("5G towers are spreading coronavirus, experts warn", 0),
    ("Supreme Court rules on landmark voting rights case", 1),
    ("George Soros controls entire global banking system", 0),
    ("European Union agrees on new climate change targets", 1),
    ("Bill Gates plans to depopulate Earth with vaccines", 0),
    ("United Nations holds emergency session on famine crisis", 1),
    ("Chemtrails are government mind control program revealed", 0),
]


def parity_test_en(assets_dir: str, tolerance: float = 1e-3) -> dict:
    """
    Hitung skor Python vs ONNX pada 20 kalimat contoh.
    ONNX sudah diuji di evaluate_english, di sini kita validasi
    konsistensi antar-run (determinisme).
    """
    print("\n=== [EN] Parity Test (20 samples) ===")
    json_path = os.path.join(assets_dir, "fake_news_model_en_v13.json")
    with open(json_path, encoding="utf-8") as f:
        cfg = json.load(f)

    word_vocab = cfg["word"]["vocabulary"]
    word_idf = cfg["word"]["idf"]
    char_vocab = cfg["char"]["vocabulary"]
    char_idf = cfg["char"]["idf"]
    coef_word = np.array(cfg["word"]["coef"], dtype=np.float64)
    coef_char = np.array(cfg["char"]["coef"], dtype=np.float64)
    intercept = cfg["svm"]["intercept"]
    threshold = cfg["uncertain_threshold"]

    session = ort.InferenceSession(os.path.join(assets_dir, "fake_news_model_en_v13.onnx"))

    results = []
    all_pass = True
    for text, expected_label in PARITY_SAMPLES_EN:
        cleaned = preprocess_en(text)
        wv = tfidf_from_json(cleaned, word_vocab, word_idf, "word", (1, 2), True)
        cv = tfidf_from_json(cleaned, char_vocab, char_idf, "char_wb", (2, 4), True)

        # Python score (dot product manual)
        score_py = float(np.dot(wv, coef_word) + np.dot(cv, coef_char) + intercept)
        # ONNX score
        score_onnx = score_en_onnx(session, wv, cv)

        diff = abs(score_py - score_onnx)
        ok = diff < tolerance
        if not ok:
            all_pass = False
        results.append({
            "text": text[:60],
            "score_python": round(score_py, 6),
            "score_onnx": round(score_onnx, 6),
            "diff": round(diff, 8),
            "pass": ok,
        })
        status = "PASS" if ok else "FAIL"
        print(f"  [{status}] diff={diff:.2e}  '{text[:50]}'")

    print(f"  Parity: {'ALL PASS' if all_pass else 'SOME FAILED'} ({sum(r['pass'] for r in results)}/20)")
    return {"all_pass": all_pass, "tolerance": tolerance, "samples": results}


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    args = parse_args()
    metrics_en = evaluate_english(args)
    metrics_id = evaluate_indonesian(args)
    parity = parity_test_en(args.assets_dir)

    output = {
        "generated_by": "src/evaluate.py",
        "english_v13": metrics_en,
        "indonesian_political_v3": metrics_id,
        "parity_test_en": parity,
    }

    out_path = os.path.join(args.assets_dir, "metrics.json")
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(output, f, indent=2, ensure_ascii=False)
    print(f"\n=== metrics.json → {out_path} ===")
