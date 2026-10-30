# export_android.py — ekspor model .pkl ke format ONNX + JSON.GZ yang dipakai aplikasi Android
#
# Penggunaan:
#   python src/export_android.py --model-dir models/ --out-dir android-app/app/src/main/assets/
#
# Output:
#   fake_news_model_en_v13.onnx  + fake_news_model_en_v13.json.gz
#   fake_news_model_id_political_v3.onnx + fake_news_model_id_political_v3.json.gz
#
# Format ONNX:
#   EN  : input  word_tfidf [1, 100000] float32 + char_tfidf [1, 50000] float32
#         output score      [1, 1]      float32
#   ID  : input  tfidf      [1, 100000] float32
#         output score      [1, 1]      float32
#
# Arsitektur SVM linear: score = coef_word @ x_word + coef_char @ x_char + intercept

import argparse
import gzip
import json
import os

import joblib
import numpy as np
import onnx
from onnx import helper, TensorProto, numpy_helper


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def parse_args():
    p = argparse.ArgumentParser(
        description="Ekspor model .pkl ke ONNX + JSON.GZ untuk aplikasi Android"
    )
    p.add_argument("--model-dir", default="models", help="Folder .pkl (output train.py).")
    p.add_argument(
        "--out-dir",
        default="android-app/app/src/main/assets",
        help="Folder output ONNX + JSON.GZ.",
    )
    p.add_argument(
        "--uncertain-threshold",
        type=float,
        default=0.2,
        help="Threshold margin untuk label UNCERTAIN (EN model). Default: 0.2",
    )
    return p.parse_args()


# ---------------------------------------------------------------------------
# Helper: konversi vocab dict ke list terurut berdasarkan indeks
# ---------------------------------------------------------------------------

def vocab_to_ordered_list(vocabulary: dict) -> list:
    """Kembalikan list token yang diurutkan sesuai indeks kolom TF-IDF."""
    ordered = [""] * len(vocabulary)
    for token, idx in vocabulary.items():
        ordered[idx] = token
    return ordered


# ---------------------------------------------------------------------------
# Helper: bangun ONNX graph LinearSVC satu input
# ---------------------------------------------------------------------------

def build_onnx_single_input(
    coef: np.ndarray,         # shape (1, n_features)
    intercept: float,
    input_name: str,
    n_features: int,
    model_doc: str,
) -> onnx.ModelProto:
    """
    Buat ONNX graph: score = matmul(x, coef.T) + intercept
    Input : [1, n_features] float32
    Output: [1, 1]          float32
    """
    coef_f32 = coef.astype(np.float32).reshape(n_features, 1)
    bias_f32 = np.array([[intercept]], dtype=np.float32)

    coef_init = numpy_helper.from_array(coef_f32, name="coef")
    bias_init = numpy_helper.from_array(bias_f32, name="bias")

    matmul_node = helper.make_node("MatMul", inputs=[input_name, "coef"], outputs=["raw"])
    add_node = helper.make_node("Add", inputs=["raw", "bias"], outputs=["score"])

    graph = helper.make_graph(
        nodes=[matmul_node, add_node],
        name="LinearSVC",
        inputs=[helper.make_tensor_value_info(input_name, TensorProto.FLOAT, [1, n_features])],
        outputs=[helper.make_tensor_value_info("score", TensorProto.FLOAT, [1, 1])],
        initializer=[coef_init, bias_init],
    )

    model = helper.make_model(graph, opset_imports=[helper.make_opsetid("", 17)])
    model.doc_string = model_doc
    onnx.checker.check_model(model)
    return model


# ---------------------------------------------------------------------------
# Helper: bangun ONNX graph EN (dua input: word + char)
# ---------------------------------------------------------------------------

def build_onnx_dual_input(
    coef_word: np.ndarray,     # shape (1, 100000)
    coef_char: np.ndarray,     # shape (1, 50000)
    intercept: float,
    n_word: int,
    n_char: int,
    model_doc: str,
) -> onnx.ModelProto:
    """
    Graph: score = matmul(word_tfidf, coef_word.T)
                 + matmul(char_tfidf, coef_char.T)
                 + intercept
    """
    cw = coef_word.astype(np.float32).reshape(n_word, 1)
    cc = coef_char.astype(np.float32).reshape(n_char, 1)
    bias = np.array([[intercept]], dtype=np.float32)

    cw_init = numpy_helper.from_array(cw, name="coef_word")
    cc_init = numpy_helper.from_array(cc, name="coef_char")
    bias_init = numpy_helper.from_array(bias, name="bias")

    nodes = [
        helper.make_node("MatMul", inputs=["word_tfidf", "coef_word"], outputs=["score_word"]),
        helper.make_node("MatMul", inputs=["char_tfidf", "coef_char"], outputs=["score_char"]),
        helper.make_node("Add",    inputs=["score_word", "score_char"], outputs=["score_wc"]),
        helper.make_node("Add",    inputs=["score_wc",  "bias"],        outputs=["score"]),
    ]

    graph = helper.make_graph(
        nodes=nodes,
        name="LinearSVC_EN",
        inputs=[
            helper.make_tensor_value_info("word_tfidf", TensorProto.FLOAT, [1, n_word]),
            helper.make_tensor_value_info("char_tfidf", TensorProto.FLOAT, [1, n_char]),
        ],
        outputs=[
            helper.make_tensor_value_info("score", TensorProto.FLOAT, [1, 1]),
        ],
        initializer=[cw_init, cc_init, bias_init],
    )

    model = helper.make_model(graph, opset_imports=[helper.make_opsetid("", 17)])
    model.doc_string = model_doc
    onnx.checker.check_model(model)
    return model


# ---------------------------------------------------------------------------
# Export English v1.3
# ---------------------------------------------------------------------------

def export_english(model_dir: str, out_dir: str, uncertain_threshold: float):
    print("\n=== [EN] Mengekspor model English v1.3 ===")

    tfidf_word = joblib.load(os.path.join(model_dir, "tfidf_word_en.pkl"))
    tfidf_char = joblib.load(os.path.join(model_dir, "tfidf_char_en.pkl"))
    svm: object = joblib.load(os.path.join(model_dir, "svm_en.pkl"))

    coef = svm.coef_  # shape (1, n_word + n_char)
    intercept = float(svm.intercept_[0])
    n_word = len(tfidf_word.vocabulary_)
    n_char = len(tfidf_char.vocabulary_)

    print(f"  word features : {n_word:,}")
    print(f"  char features : {n_char:,}")
    print(f"  intercept     : {intercept:.6f}")

    coef_word = coef[0, :n_word]
    coef_char = coef[0, n_word:]

    # --- ONNX ---
    onnx_model = build_onnx_dual_input(
        coef_word=coef_word,
        coef_char=coef_char,
        intercept=intercept,
        n_word=n_word,
        n_char=n_char,
        model_doc="FakeNewsDetection EN v1.3 — LinearSVC dual TF-IDF (word+char)",
    )
    onnx_path = os.path.join(out_dir, "fake_news_model_en_v13.onnx")
    with open(onnx_path, "wb") as f:
        f.write(onnx_model.SerializeToString())
    print(f"  ONNX → {onnx_path} ({os.path.getsize(onnx_path) / 1024:.1f} KB)")

    # --- JSON.GZ (tanpa duplikasi coef karena bobot SVM sudah di dalam .onnx) ---
    payload = {
        "model_version": "1.3",
        "language": "en",
        "labels": {"0": "FAKE", "1": "REAL", "-1": "UNCERTAIN"},
        "uncertain_threshold": uncertain_threshold,
        "cleaning": {
            "lowercase": True,
            "remove_urls": True,
            "remove_html": True,
            "remove_reuters": True,
            "remove_numbers": True,
            "delete_punctuation": True,
            "collapse_whitespace": True,
        },
        "word": {
            "analyzer": "word",
            "ngram_min": 1,
            "ngram_max": 2,
            "sublinear_tf": True,
            "norm": "l2",
            "vocabulary": {token: int(idx) for token, idx in tfidf_word.vocabulary_.items()},
            "idf": tfidf_word.idf_.tolist(),
            "stop_words": list(tfidf_word.stop_words_) if hasattr(tfidf_word, "stop_words_") and tfidf_word.stop_words_ else [],
        },
        "char": {
            "analyzer": "char_wb",
            "ngram_min": 3,
            "ngram_max": 5,
            "sublinear_tf": True,
            "norm": "l2",
            "vocabulary": {token: int(idx) for token, idx in tfidf_char.vocabulary_.items()},
            "idf": tfidf_char.idf_.tolist(),
        },
        "svm": {
            "intercept": intercept,
            "classes": svm.classes_.tolist(),
            "C": float(svm.C),
        },
        "metrics": {
            "note": "Diisi setelah menjalankan src/evaluate.py"
        },
    }

    gz_path = os.path.join(out_dir, "fake_news_model_en_v13.json.gz")
    raw_bytes = json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    with open(gz_path, "wb") as raw_f:
        with gzip.GzipFile(filename="", mode="wb", fileobj=raw_f, compresslevel=9, mtime=0) as gz_f:
            gz_f.write(raw_bytes)
    print(f"  JSON.GZ → {gz_path} ({os.path.getsize(gz_path) / 1024 / 1024:.2f} MB)")


# ---------------------------------------------------------------------------
# Export Indonesian Political v3
# ---------------------------------------------------------------------------

def export_indonesian(model_dir: str, out_dir: str):
    print("\n=== [ID] Mengekspor model Indonesian Political v3 ===")

    tfidf_id = joblib.load(os.path.join(model_dir, "tfidf_id.pkl"))
    svm_id: object = joblib.load(os.path.join(model_dir, "svm_id.pkl"))

    coef = svm_id.coef_  # shape (1, 100000)
    intercept = float(svm_id.intercept_[0])
    n_feat = len(tfidf_id.vocabulary_)

    print(f"  features  : {n_feat:,}")
    print(f"  intercept : {intercept:.6f}")

    # --- ONNX ---
    onnx_model = build_onnx_single_input(
        coef=coef[0],
        intercept=intercept,
        input_name="tfidf",
        n_features=n_feat,
        model_doc="FakeNewsDetection ID Political v3 — LinearSVC TF-IDF word",
    )
    onnx_path = os.path.join(out_dir, "fake_news_model_id_political_v3.onnx")
    with open(onnx_path, "wb") as f:
        f.write(onnx_model.SerializeToString())
    print(f"  ONNX → {onnx_path} ({os.path.getsize(onnx_path) / 1024:.1f} KB)")

    # --- JSON.GZ (tanpa duplikasi coef karena bobot SVM sudah di dalam .onnx) ---
    payload = {
        "version": "political-v3-android",
        "language": "id",
        "scope": "political_news",
        "model_type": "tfidf_linear_svc",
        "labels": {"0": "REAL", "1": "FAKE"},
        "classes": svm_id.classes_.tolist(),
        "intercept": intercept,
        "threshold": 0.1,
        "vectorizer": {
            "ngram_min": 1,
            "ngram_max": 2,
            "sublinear_tf": True,
            "norm": "l2",
            "lowercase": True,
            "min_token_length": 2,
        },
        "preprocessing": {
            "lowercase": True,
            "remove_urls": True,
            "remove_html": True,
            "collapse_whitespace": True,
        },
        "vocabulary": {token: int(idx) for token, idx in tfidf_id.vocabulary_.items()},
        "idf": tfidf_id.idf_.tolist(),
    }

    gz_path = os.path.join(out_dir, "fake_news_model_id_political_v3.json.gz")
    raw_bytes = json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    with open(gz_path, "wb") as raw_f:
        with gzip.GzipFile(filename="", mode="wb", fileobj=raw_f, compresslevel=9, mtime=0) as gz_f:
            gz_f.write(raw_bytes)
    print(f"  JSON.GZ → {gz_path} ({os.path.getsize(gz_path) / 1024 / 1024:.2f} MB)")


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    args = parse_args()
    os.makedirs(args.out_dir, exist_ok=True)
    export_english(args.model_dir, args.out_dir, args.uncertain_threshold)
    export_indonesian(args.model_dir, args.out_dir)
    print("\n=== EKSPOR SELESAI ===")
    print("Jalankan src/evaluate.py untuk memverifikasi artefak ONNX.")
