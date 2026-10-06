import os
from datetime import datetime

from dotenv import load_dotenv
from fastapi import FastAPI, Depends, HTTPException
from pydantic import BaseModel
from sqlalchemy import (
    create_engine,
    Integer,
    String,
    Text,
    Float,
    DateTime,
    select,
)
from sqlalchemy.engine import URL
from sqlalchemy.orm import (
    DeclarativeBase,
    Mapped,
    mapped_column,
    Session,
    sessionmaker,
)

# =========================================================
# LOAD ENVIRONMENT VARIABLES
# =========================================================

load_dotenv()


# =========================================================
# DATABASE CONFIGURATION
# =========================================================

database_url = URL.create(
    drivername="postgresql+psycopg",
    username=os.getenv("DB_USER"),
    password=os.getenv("DB_PASSWORD"),
    host=os.getenv("DB_HOST"),
    port=int(os.getenv("DB_PORT", "5432")),
    database=os.getenv("DB_NAME"),
)

engine = create_engine(
    database_url,
    pool_pre_ping=True,
)

SessionLocal = sessionmaker(
    bind=engine,
    autoflush=False,
    autocommit=False,
)


# =========================================================
# BASE MODEL
# =========================================================

class Base(DeclarativeBase):
    pass


# =========================================================
# DATABASE MODEL
# =========================================================

class DetectionHistory(Base):
    __tablename__ = "detection_history"

    id: Mapped[int] = mapped_column(
        Integer,
        primary_key=True,
        index=True,
    )

    title: Mapped[str] = mapped_column(
        String(255),
        nullable=False,
    )

    content: Mapped[str] = mapped_column(
        Text,
        nullable=False,
    )

    prediction: Mapped[str] = mapped_column(
        String(50),
        nullable=False,
    )

    score: Mapped[float | None] = mapped_column(
        Float,
        nullable=True,
    )

    created_at: Mapped[datetime] = mapped_column(
        DateTime,
        default=datetime.now,
        nullable=False,
    )


# Buat tabel otomatis jika belum ada
Base.metadata.create_all(bind=engine)


# =========================================================
# FASTAPI CONFIGURATION
# =========================================================

app = FastAPI(
    title="Fake News Detection API",
    description="API untuk menyimpan riwayat hasil deteksi berita menggunakan PostgreSQL",
    version="1.0.0",
)


# =========================================================
# REQUEST SCHEMA
# =========================================================

class DetectionRequest(BaseModel):
    title: str
    content: str
    prediction: str
    score: float | None = None


# =========================================================
# DATABASE SESSION
# =========================================================

def get_db():
    db = SessionLocal()

    try:
        yield db
    finally:
        db.close()


# =========================================================
# HOME / HEALTH CHECK
# =========================================================

@app.get("/")
def home():
    return {
        "message": "Fake News Detection API is running",
        "database": "PostgreSQL",
    }


# =========================================================
# CREATE HISTORY
# =========================================================

@app.post("/api/history")
def create_history(
    data: DetectionRequest,
    db: Session = Depends(get_db),
):
    history = DetectionHistory(
        title=data.title,
        content=data.content,
        prediction=data.prediction,
        score=data.score,
    )

    db.add(history)
    db.commit()
    db.refresh(history)

    return {
        "message": "Riwayat berhasil disimpan",
        "id": history.id,
    }


# =========================================================
# GET ALL HISTORY
# =========================================================

@app.get("/api/history")
def get_history(
    db: Session = Depends(get_db),
):
    statement = (
        select(DetectionHistory)
        .order_by(DetectionHistory.id.desc())
    )

    histories = db.scalars(statement).all()

    return [
        {
            "id": history.id,
            "title": history.title,
            "content": history.content,
            "prediction": history.prediction,
            "score": history.score,
            "created_at": history.created_at,
        }
        for history in histories
    ]


# =========================================================
# GET HISTORY DETAIL
# =========================================================

@app.get("/api/history/{history_id}")
def get_history_detail(
    history_id: int,
    db: Session = Depends(get_db),
):
    history = db.get(
        DetectionHistory,
        history_id,
    )

    if history is None:
        raise HTTPException(
            status_code=404,
            detail="Riwayat tidak ditemukan",
        )

    return {
        "id": history.id,
        "title": history.title,
        "content": history.content,
        "prediction": history.prediction,
        "score": history.score,
        "created_at": history.created_at,
    }


# =========================================================
# DELETE HISTORY
# =========================================================

@app.delete("/api/history/{history_id}")
def delete_history(
    history_id: int,
    db: Session = Depends(get_db),
):
    history = db.get(
        DetectionHistory,
        history_id,
    )

    if history is None:
        raise HTTPException(
            status_code=404,
            detail="Riwayat tidak ditemukan",
        )

    db.delete(history)
    db.commit()

    return {
        "message": "Riwayat berhasil dihapus",
    }