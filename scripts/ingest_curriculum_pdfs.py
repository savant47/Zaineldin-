#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
===============================================================================
سكربت سحب ومعالجة كتب المنهج المدرسي (PDF Ingestion & Grounding Script)
تطبيق المدرس الخصوصي - لزين الدين آسر الشربيني (الصف الثاني الابتدائي لغات 2026/2027)
===============================================================================

الوظيفة:
1. يقوم بتحميل ملفات الـ PDF الرسمية للمدرسة من رابط مجلد Google Drive.
2. يستخرج النصوص باللغتين العربية والإنجليزية بدقة عالية باستخدام مكتبة PyMuPDF (fitz)
   مع معالجة اتجاه النص العربي (arabic-reshaper & python-bidi).
3. يستبعد تلقائياً أي ملفات تخص مادة "اكتشف" (Discover) الملغاة رسمياً في تحديثات 2026/2027.
4. يهيكل البيانات بصيغة JSON القياسية: (المادة -> الباب/الوحدة -> الدرس).
5. يرفع الدروس مباشرة إلى قاعدة بيانات Firebase Firestore المسمّاة (Named Database)
   لتكون "المصدر الوحيد والمرجع الأساسي" (Single Source of Truth / Grounding)
   لمحرك الذكاء الاصطناعي Gemini، مما يمنع الهلوسة بمعلومات خارجية.

طريقة التشغيل:
    pip install pymupdf gdown firebase-admin arabic-reshaper python-bidi
    python3 scripts/ingest_curriculum_pdfs.py \
        --drive-url "https://drive.google.com/drive/folders/YOUR_FOLDER_ID" \
        --user-id "YOUR_FIREBASE_AUTH_UID" \
        --service-account "path/to/serviceAccountKey.json"
"""

import argparse
import json
import os
import re
import tempfile
from datetime import datetime, timezone
from typing import Dict, List, Any

try:
    import fitz  # PyMuPDF
except ImportError:
    fitz = None

try:
    import gdown
except ImportError:
    gdown = None

try:
    import firebase_admin
    from firebase_admin import credentials, firestore
except ImportError:
    firebase_admin = None


# قاعدة المواد الخمس المعتمدة رسمياً للصف الثاني الابتدائي لغات (2026/2027)
# ملاحظة: تم إلغاء مادة "اكتشف" (Discover) نهائياً ولا يتم قبولها.
OFFICIAL_SUBJECTS = {
    "arabic": {
        "subjectId": "arabic",
        "subjectNameAr": "اللغة العربية (تواصل)",
        "keywords": ["arabic", "عربي", "اللغة العربية", "تواصل"],
    },
    "religion": {
        "subjectId": "religion",
        "subjectNameAr": "التربية الدينية",
        "keywords": ["religion", "دين", "التربية الدينية", "اسلامية", "إسلامية"],
    },
    "math": {
        "subjectId": "math",
        "subjectNameAr": "Math - الرياضيات لغات",
        "keywords": ["math", "mathematics", "ماث", "رياضيات"],
    },
    "connect": {
        "subjectId": "connect",
        "subjectNameAr": "English Connect",
        "keywords": ["connect_standard", "english_connect", "connect2"],
    },
    "connect_plus": {
        "subjectId": "connect_plus",
        "subjectNameAr": "English Advanced (Connect Plus)",
        "keywords": ["connect_plus", "connect plus", "advanced", "كونكت بلس"],
    },
}

CANCELLED_KEYWORDS = ["discover", "اكتشف", "إكتشف"]

DEFAULT_DATABASE_ID = "ai-studio-android-4e6de0b0-584f-4704-83b4-ad412818f423"


def is_cancelled_subject(filename: str) -> bool:
    lower_name = filename.lower()
    return any(cancelled in lower_name for cancelled in CANCELLED_KEYWORDS)


def detect_subject(filename: str) -> Dict[str, str]:
    lower_name = filename.lower()
    if "plus" in lower_name or "بلس" in lower_name or "advanced" in lower_name:
        return OFFICIAL_SUBJECTS["connect_plus"]
    for key, info in OFFICIAL_SUBJECTS.items():
        if any(kw in lower_name for kw in info["keywords"]):
            return info
    if "connect" in lower_name or "كونكت" in lower_name or "english" in lower_name:
        return OFFICIAL_SUBJECTS["connect"]
    return OFFICIAL_SUBJECTS["arabic"]


def clean_extracted_text(raw_text: str) -> str:
    """تنظيف النصوص العربية والإنجليزية المستخرجة من ملفات الـ PDF."""
    text = re.sub(r"[ \t]+", " ", raw_text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def parse_pdf_into_structured_lessons(pdf_path: str, user_id: str) -> List[Dict[str, Any]]:
    """
    يستخرج النص من ملف الـ PDF ويهيكله بصيغة:
    (المادة -> الباب/الوحدة -> الدرس)
    """
    if fitz is None:
        raise RuntimeError("يرجى تثبيت مكتبة PyMuPDF عبر الأمر: pip install pymupdf")

    filename = os.path.basename(pdf_path)
    if is_cancelled_subject(filename):
        print(f"[تخطي] تم تجاهل الملف '{filename}' لأن مادة 'اكتشف' (Discover) ملغاة رسمياً في منهج 2026/2027.")
        return []

    subject_meta = detect_subject(filename)
    doc = fitz.open(pdf_path)
    full_pages_text: List[str] = []

    for page_index in range(len(doc)):
        page = doc.load_page(page_index)
        page_text = page.get_text("text")
        if page_text and page_text.strip():
            full_pages_text.append(clean_extracted_text(page_text))

    combined_text = "\n\n".join(full_pages_text)
    if not combined_text:
        return []

    # تقسيم ذكي إلى وحدات (Units / المحاور) ودروس (Lessons / الدروس)
    unit_pattern = re.compile(
        r"(?:Unit\s+\d+|Chapter\s+\d+|المحور\s+[^\n]+|الوحدة\s+[^\n]+|الباب\s+[^\n]+)",
        re.IGNORECASE,
    )
    lesson_pattern = re.compile(
        r"(?:Lesson\s+\d+[^\n]*|الدرس\s+[^\n]+)",
        re.IGNORECASE,
    )

    unit_matches = list(unit_pattern.finditer(combined_text))
    structured_lessons: List[Dict[str, Any]] = []
    lesson_order = 1

    if not unit_matches:
        # في حال عدم وجود عناوين وحدات صريحة، يتم تقسيم الكتاب إلى دروس متتالية
        chunks = [ combined_text[i:i + 3500] for i in range(0, len(combined_text), 3500) ]
        for idx, chunk in enumerate(chunks, start=1):
            if len(chunk.strip()) < 20:
                continue
            structured_lessons.append({
                "userId": user_id,
                "subjectId": subject_meta["subjectId"],
                "subjectNameAr": subject_meta["subjectNameAr"],
                "unitTitle": "الوحدة الأولى - المنهج الرسمي 2026/2027",
                "lessonTitle": f"الدرس {idx} - {os.path.splitext(filename)[0]}",
                "lessonOrder": idx,
                "officialContent": chunk[:14500],
                "keyTerms": extract_key_terms(chunk),
                "sourceBookName": filename,
                "isCompleted": False,
            })
        return structured_lessons

    for u_idx, u_match in enumerate(unit_matches):
        unit_title = u_match.group(0).strip()[:140]
        u_start = u_match.start()
        u_end = unit_matches[u_idx + 1].start() if (u_idx + 1) < len(unit_matches) else len(combined_text)
        unit_block = combined_text[u_start:u_end]

        l_matches = list(lesson_pattern.finditer(unit_block))
        if not l_matches:
            content_slice = unit_block.strip()[:14500]
            if len(content_slice) >= 20:
                structured_lessons.append({
                    "userId": user_id,
                    "subjectId": subject_meta["subjectId"],
                    "subjectNameAr": subject_meta["subjectNameAr"],
                    "unitTitle": unit_title,
                    "lessonTitle": f"الدرس {lesson_order}",
                    "lessonOrder": lesson_order,
                    "officialContent": content_slice,
                    "keyTerms": extract_key_terms(content_slice),
                    "sourceBookName": filename,
                    "isCompleted": False,
                })
                lesson_order += 1
        else:
            for l_idx, l_match in enumerate(l_matches):
                lesson_title = l_match.group(0).strip()[:140]
                l_start = l_match.start()
                l_end = l_matches[l_idx + 1].start() if (l_idx + 1) < len(l_matches) else len(unit_block)
                lesson_content = unit_block[l_start:l_end].strip()[:14500]
                if len(lesson_content) < 20:
                    continue
                structured_lessons.append({
                    "userId": user_id,
                    "subjectId": subject_meta["subjectId"],
                    "subjectNameAr": subject_meta["subjectNameAr"],
                    "unitTitle": unit_title,
                    "lessonTitle": lesson_title,
                    "lessonOrder": lesson_order,
                    "officialContent": lesson_content,
                    "keyTerms": extract_key_terms(lesson_content),
                    "sourceBookName": filename,
                    "isCompleted": False,
                })
                lesson_order += 1

    return structured_lessons


def extract_key_terms(text: str) -> str:
    """استخراج أهم المصطلحات والكلمات المفتاحية من نص الدرس."""
    words = re.findall(r"[A-Za-z]{4,}|[\u0600-\u06FF]{4,}", text)
    seen = []
    for w in words:
        if w.lower() not in [s.lower() for s in seen]:
            seen.append(w)
        if len(seen) >= 12:
            break
    return "، ".join(seen) if seen else "مصطلحات الدرس الأساسية"


def download_drive_folder(drive_url: str, output_dir: str) -> List[str]:
    """تحميل جميع ملفات الـ PDF من مجلد Google Drive."""
    if gdown is None:
        raise RuntimeError("يرجى تثبيت مكتبة gdown عبر الأمر: pip install gdown")
    print(f"جاري تحميل ملفات المنهج من Google Drive: {drive_url}")
    gdown.download_folder(url=drive_url, output=output_dir, quiet=False, use_cookies=False)
    pdf_files = []
    for root, _, files in os.walk(output_dir):
        for f in files:
            if f.lower().endswith(".pdf"):
                pdf_files.append(os.path.join(root, f))
    return sorted(pdf_files)


def upload_lessons_to_firestore(
    lessons: List[Dict[str, Any]],
    user_id: str,
    service_account_path: str,
    database_id: str = DEFAULT_DATABASE_ID,
) -> None:
    """رفع الدروس المهيكلة مباشرة إلى Firebase Firestore."""
    if firebase_admin is None:
        raise RuntimeError("يرجى تثبيت مكتبة firebase-admin عبر الأمر: pip install firebase-admin")

    if not firebase_admin._apps:
        cred = credentials.Certificate(service_account_path)
        firebase_admin.initialize_app(cred)

    db = firestore.client(database_id=database_id)
    batch = db.batch()
    collection_ref = db.collection("users").document(user_id).collection("lessons")

    for idx, lesson in enumerate(lessons, start=1):
        doc_id = f"{lesson['subjectId']}_pdf_{idx}"
        doc_ref = collection_ref.document(doc_id)
        payload = {
            **lesson,
            "createdAt": firestore.SERVER_TIMESTAMP,
            "updatedAt": firestore.SERVER_TIMESTAMP,
        }
        batch.set(doc_ref, payload)
        if idx % 400 == 0:
            batch.commit()
            batch = db.batch()

    batch.commit()
    print(f"تم رفع {len(lessons)} درساً بنجاح إلى Firebase Firestore في مسار users/{user_id}/lessons!")


def main():
    parser = argparse.ArgumentParser(
        description="سكربت سحب كتب المنهج (PDF) من Google Drive ورفعها إلى Firebase Firestore لزين الدين"
    )
    parser.add_argument("--drive-url", help="رابط مجلد Google Drive الذي يحتوي على كتب الـ PDF")
    parser.add_argument("--local-dir", help="مسار مجلد محلي يحتوي على ملفات الـ PDF (بديل اختياري)")
    parser.add_argument("--user-id", required=True, help="معرّف المستخدم في Firebase Auth (UID)")
    parser.add_argument("--output-json", default="curriculum_output.json", help="مسار حفظ ملف JSON المهيكل")
    parser.add_argument("--service-account", help="مسار ملف مفاتيح الخدمة serviceAccountKey.json للرفع المباشر")
    parser.add_argument("--database-id", default=DEFAULT_DATABASE_ID, help="معرّف قاعدة بيانات Firestore")

    args = parser.parse_args()

    all_lessons: List[Dict[str, Any]] = []
    with tempfile.TemporaryDirectory() as tmp_dir:
        target_dir = args.local_dir if args.local_dir else tmp_dir
        if args.drive_url:
            pdf_files = download_drive_folder(args.drive_url, target_dir)
        elif args.local_dir:
            pdf_files = [
                os.path.join(args.local_dir, f)
                for f in os.listdir(args.local_dir)
                if f.lower().endswith(".pdf")
            ]
        else:
            raise ValueError("يرجى تمرير --drive-url أو --local-dir")

        for pdf_file in pdf_files:
            print(f"جاري معالجة الكتاب: {os.path.basename(pdf_file)} ...")
            lessons = parse_pdf_into_structured_lessons(pdf_file, args.user_id)
            all_lessons.extend(lessons)

    # حفظ النسخة المهيكلة بصيغة JSON (المادة -> الباب -> الدرس)
    with open(args.output_json, "w", encoding="utf-8") as f:
        json.dump(all_lessons, f, ensure_ascii=False, indent=2)
    print(f"تم استخراج وحفظ {len(all_lessons)} درساً في الملف: {args.output_json}")

    if args.service_account:
        upload_lessons_to_firestore(
            lessons=all_lessons,
            user_id=args.user_id,
            service_account_path=args.service_account,
            database_id=args.database_id,
        )


if __name__ == "__main__":
    main()
