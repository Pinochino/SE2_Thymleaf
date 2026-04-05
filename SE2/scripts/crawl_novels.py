"""
Crawl novels + chapters from Open Library API & Project Gutenberg.
Saves to local PostgreSQL (se2-new) without vector columns.
Each chapter has proper paragraph breaks (\n\n) for paragraph-level commenting.

Usage:
    pip install psycopg2-binary requests
    python crawl_novels.py
"""

import requests
import psycopg2
import uuid
import re
import time
import random
from datetime import datetime

# ─── DB Config ───────────────────────────────────────────────
DB_CONFIG = {
    "host": "127.0.0.1",
    "port": 5432,
    "dbname": "se2-new",
    "user": "postgres",
    "password": "Hieuhayho123!@#",
}

# ─── Genre IDs (from DB) ────────────────────────────────────
GENRE_IDS = {
    "FANTASY": 146,
    "COMEDY": 147,
    "CRIME": 148,
    "HORROR": 149,
    "HISTORIC": 150,
    "SCIFI": 151,
    "ROMANCE": 152,
}

# ─── Gutenberg books with full metadata ──────────────────────
GUTENBERG_BOOKS = [
    # (gutenberg_id, title, author, status, genres[])
    (84, "Frankenstein", "Mary Shelley", "COMPLETED", ["HORROR", "SCIFI"]),
    (345, "Dracula", "Bram Stoker", "COMPLETED", ["HORROR"]),
    (174, "The Picture of Dorian Gray", "Oscar Wilde", "COMPLETED", ["HORROR", "ROMANCE"]),
    (696, "Strange Case of Dr Jekyll and Mr Hyde", "Robert Louis Stevenson", "COMPLETED", ["HORROR", "CRIME"]),
    (1661, "The Adventures of Sherlock Holmes", "Arthur Conan Doyle", "COMPLETED", ["CRIME"]),
    (2852, "The Hound of the Baskervilles", "Arthur Conan Doyle", "COMPLETED", ["CRIME"]),
    (730, "Oliver Twist", "Charles Dickens", "COMPLETED", ["CRIME", "HISTORIC"]),
    (1342, "Pride and Prejudice", "Jane Austen", "COMPLETED", ["ROMANCE"]),
    (161, "Sense and Sensibility", "Jane Austen", "COMPLETED", ["ROMANCE"]),
    (768, "Wuthering Heights", "Emily Bronte", "COMPLETED", ["ROMANCE", "HORROR"]),
    (1260, "Jane Eyre", "Charlotte Bronte", "COMPLETED", ["ROMANCE", "HISTORIC"]),
    (11, "Alice's Adventures in Wonderland", "Lewis Carroll", "COMPLETED", ["FANTASY", "COMEDY"]),
    (35, "The Time Machine", "H.G. Wells", "COMPLETED", ["SCIFI", "FANTASY"]),
    (36, "The War of the Worlds", "H.G. Wells", "COMPLETED", ["SCIFI"]),
    (164, "Twenty Thousand Leagues Under the Sea", "Jules Verne", "COMPLETED", ["SCIFI"]),
    (98, "A Tale of Two Cities", "Charles Dickens", "COMPLETED", ["HISTORIC"]),
    (1399, "Anna Karenina", "Leo Tolstoy", "COMPLETED", ["HISTORIC", "ROMANCE"]),
    (76, "Adventures of Huckleberry Finn", "Mark Twain", "COMPLETED", ["COMEDY"]),
    (1400, "Great Expectations", "Charles Dickens", "COMPLETED", ["HISTORIC", "COMEDY"]),
    (120, "Treasure Island", "Robert Louis Stevenson", "COMPLETED", ["FANTASY", "CRIME"]),
    (16, "Peter Pan", "J. M. Barrie", "COMPLETED", ["FANTASY"]),
    (1952, "The Yellow Wallpaper", "Charlotte Perkins Gilman", "COMPLETED", ["HORROR"]),
    (205, "Walden", "Henry David Thoreau", "COMPLETED", ["HISTORIC"]),
    (74, "The Adventures of Tom Sawyer", "Mark Twain", "COMPLETED", ["COMEDY"]),
    (43, "The Strange Case of Dr. Jekyll and Mr. Hyde", "Robert Louis Stevenson", "COMPLETED", ["HORROR"]),
    (1080, "A Modest Proposal", "Jonathan Swift", "COMPLETED", ["COMEDY"]),
    (46, "A Christmas Carol", "Charles Dickens", "COMPLETED", ["FANTASY", "HISTORIC"]),
    (2701, "Moby Dick", "Herman Melville", "COMPLETED", ["HISTORIC"]),
    (1232, "The Prince", "Niccolo Machiavelli", "COMPLETED", ["HISTORIC"]),
    (55, "The Wonderful Wizard of Oz", "L. Frank Baum", "COMPLETED", ["FANTASY"]),
]

# ─── Open Library additional novels (metadata only, no chapters) ──
OL_QUERIES = {
    "FANTASY": "fantasy+fiction+novel",
    "HORROR": "horror+gothic+fiction",
    "ROMANCE": "romance+love+fiction",
    "HISTORIC": "historical+fiction+novel",
    "SCIFI": "science+fiction+dystopia",
    "COMEDY": "comedy+humor+fiction",
    "CRIME": "crime+mystery+thriller",
}

SUBJECT_GENRE_MAP = {
    "fantasy": "FANTASY", "magic": "FANTASY", "dragon": "FANTASY",
    "horror": "HORROR", "gothic": "HORROR", "vampire": "HORROR", "supernatural": "HORROR",
    "romance": "ROMANCE", "love stor": "ROMANCE",
    "histor": "HISTORIC",
    "science fiction": "SCIFI", "sci-fi": "SCIFI", "dystop": "SCIFI",
    "humor": "COMEDY", "comedy": "COMEDY", "satire": "COMEDY",
    "crime": "CRIME", "detective": "CRIME", "mystery": "CRIME", "thriller": "CRIME",
}


def get_conn():
    return psycopg2.connect(**DB_CONFIG)


def novel_exists(cur, title):
    cur.execute("SELECT id FROM novel WHERE title = %s", (title,))
    row = cur.fetchone()
    return row[0] if row else None


def insert_novel(cur, title, author, description, status, rating, cover_url):
    public_id = str(uuid.uuid4())
    now = datetime.now()
    cur.execute("""
        INSERT INTO novel (title, author, description, status, average_rating, cover_img_url, public_id, created_at, updated_at)
        VALUES (%s, %s, %s, %s, %s, %s, %s::uuid, %s, %s)
        RETURNING id
    """, (title, author, description, status, rating, cover_url, public_id, now, now))
    return cur.fetchone()[0]


def link_genre(cur, novel_id, genre_name):
    genre_id = GENRE_IDS.get(genre_name)
    if not genre_id:
        return
    cur.execute("SELECT 1 FROM novel_genre WHERE novel_id = %s AND genre_id = %s", (novel_id, genre_id))
    if not cur.fetchone():
        cur.execute("INSERT INTO novel_genre (novel_id, genre_id) VALUES (%s, %s)", (novel_id, genre_id))


def insert_chapter(cur, novel_id, chapter_number, title, content, paragraph_count):
    now = datetime.now()
    cur.execute("""
        INSERT INTO chapter (novel_id, chapter_number, title, content, paragraphs, created_at, updated_at)
        VALUES (%s, %s, %s, %s, %s, %s, %s)
        RETURNING id
    """, (novel_id, chapter_number, title, content, paragraph_count, now, now))
    return cur.fetchone()[0]


def chapter_exists(cur, novel_id):
    cur.execute("SELECT count(*) FROM chapter WHERE novel_id = %s", (novel_id,))
    return cur.fetchone()[0] > 0


# ─── Gutenberg text fetching ────────────────────────────────
def fetch_gutenberg_text(gutenberg_id):
    urls = [
        f"https://www.gutenberg.org/cache/epub/{gutenberg_id}/pg{gutenberg_id}.txt",
        f"https://www.gutenberg.org/files/{gutenberg_id}/{gutenberg_id}-0.txt",
    ]
    for url in urls:
        try:
            resp = requests.get(url, timeout=30, headers={"User-Agent": "SE2-Crawler/1.0"})
            if resp.status_code == 200:
                return strip_boilerplate(resp.text)
        except Exception as e:
            print(f"  Failed {url}: {e}")
    return None


def strip_boilerplate(text):
    start_markers = ["*** START OF THE PROJECT GUTENBERG", "*** START OF THIS PROJECT GUTENBERG",
                     "***START OF THE PROJECT GUTENBERG"]
    end_markers = ["*** END OF THE PROJECT GUTENBERG", "*** END OF THIS PROJECT GUTENBERG",
                   "***END OF THE PROJECT GUTENBERG", "End of the Project Gutenberg",
                   "End of Project Gutenberg"]

    start = 0
    for m in start_markers:
        idx = text.find(m)
        if idx >= 0:
            start = text.index('\n', idx) + 1
            break

    end = len(text)
    for m in end_markers:
        idx = text.find(m)
        if idx >= 0:
            end = idx
            break

    return text[start:end].strip()


def normalize_paragraphs(raw):
    """
    Convert Gutenberg line-wrapped text into proper paragraphs.
    Each paragraph separated by \\n\\n for paragraph-level commenting.
    """
    text = raw.replace("\r\n", "\n")
    # Split on blank lines (actual paragraph breaks)
    raw_paras = re.split(r'\n\s*\n', text)

    paragraphs = []
    for p in raw_paras:
        # Join line-wrapped lines within a paragraph
        joined = re.sub(r'\s*\n\s*', ' ', p.strip()).strip()
        if not joined:
            continue
        # Skip very short non-sentence fragments (headers/page numbers)
        if len(joined) < 15 and '.' not in joined:
            continue
        paragraphs.append(joined)

    return paragraphs


def parse_chapters(text):
    """
    Parse full text into chapters.
    Returns list of (title, content_str, paragraph_count).
    """
    # Match chapter headings
    pattern = re.compile(
        r'^[ \t]*(?:CHAPTER|Chapter)\s+([IVXLCDM]+|\d+|[A-Z][A-Z ]+)(?:[.:\s\u2014\u2013-].*)?$',
        re.MULTILINE
    )

    matches = list(pattern.finditer(text))

    if not matches:
        # Fallback: split into ~2000-word blocks
        return split_by_word_blocks(text)

    chapters = []
    for i, m in enumerate(matches):
        content_start = m.end()
        content_end = matches[i + 1].start() if i + 1 < len(matches) else len(text)
        raw_content = text[content_start:content_end].strip()

        paragraphs = normalize_paragraphs(raw_content)
        if len(paragraphs) < 2:
            continue

        content = "\n\n".join(paragraphs)
        heading = m.group().strip()
        title = re.sub(r'(?i)^\s*chapter\s+', '', heading).strip().rstrip('.:- \u2014\u2013')
        if not title:
            title = f"Chapter {i + 1}"
        else:
            title = f"Chapter {title}"

        chapters.append((title, content, len(paragraphs)))

    return chapters


def split_by_word_blocks(text):
    """Fallback: split into ~2000-word blocks as chapters."""
    paragraphs = normalize_paragraphs(text)
    chapters = []
    block = []
    word_count = 0
    ch_num = 1

    for p in paragraphs:
        block.append(p)
        word_count += len(p.split())
        if word_count >= 2000:
            content = "\n\n".join(block)
            chapters.append((f"Chapter {ch_num}", content, len(block)))
            block = []
            word_count = 0
            ch_num += 1

    if block:
        content = "\n\n".join(block)
        chapters.append((f"Chapter {ch_num}", content, len(block)))

    return chapters


# ─── Open Library novel fetching ─────────────────────────────
def fetch_ol_novels(genre, query, limit=5):
    """Fetch novel metadata from Open Library API."""
    url = f"https://openlibrary.org/search.json?q={query}&limit={limit + 5}&fields=title,author_name,first_sentence,cover_i,subject,ratings_average&language=eng"
    try:
        resp = requests.get(url, timeout=15, headers={"User-Agent": "SE2-Crawler/1.0"})
        if resp.status_code != 200:
            return []
        data = resp.json()
        docs = data.get("docs", [])
        novels = []
        for doc in docs:
            if len(novels) >= limit:
                break
            title = doc.get("title", "")
            if not title or len(title) < 3:
                continue
            authors = doc.get("author_name", ["Unknown"])
            author = authors[0] if authors else "Unknown"

            # Description from first_sentence
            sentences = doc.get("first_sentence", [])
            desc = sentences[0] if sentences else f"A {genre.lower()} novel by {author}."

            # Cover
            cover_id = doc.get("cover_i")
            cover_url = f"https://covers.openlibrary.org/b/id/{cover_id}-L.jpg" if cover_id else None

            # Rating
            rating = doc.get("ratings_average")
            if rating:
                rating = round(rating, 1)
            else:
                rating = round(random.uniform(3.5, 5.0), 1)

            # Detect genres from subjects
            subjects = doc.get("subject", [])
            genres = {genre}
            for s in subjects:
                sl = s.lower()
                for keyword, gname in SUBJECT_GENRE_MAP.items():
                    if keyword in sl:
                        genres.add(gname)

            status = random.choice(["ONGOING", "COMPLETED", "COMPLETED"])
            novels.append((title, author, desc, status, rating, cover_url, list(genres)))
        return novels
    except Exception as e:
        print(f"  OL error for {genre}: {e}")
        return []


# ─── Main ────────────────────────────────────────────────────
def main():
    conn = get_conn()
    conn.autocommit = False
    cur = conn.cursor()

    total_novels = 0
    total_chapters = 0

    print("=" * 60)
    print("PHASE 1: Crawl Gutenberg books (novels + chapters)")
    print("=" * 60)

    for gid, title, author, status, genres in GUTENBERG_BOOKS:
        novel_id = novel_exists(cur, title)

        if not novel_id:
            # Get cover from Open Library
            cover_url = f"https://covers.openlibrary.org/b/id/{gid * 7}-L.jpg"
            try:
                ol = requests.get(
                    f"https://openlibrary.org/search.json?q={title.replace(' ', '+')}&limit=1&fields=cover_i",
                    timeout=10, headers={"User-Agent": "SE2-Crawler/1.0"}
                ).json()
                docs = ol.get("docs", [])
                if docs and docs[0].get("cover_i"):
                    cover_url = f"https://covers.openlibrary.org/b/id/{docs[0]['cover_i']}-L.jpg"
            except:
                pass

            rating = round(random.uniform(3.8, 5.0), 1)
            desc = f"A classic novel by {author}."
            novel_id = insert_novel(cur, title, author, desc, status, rating, cover_url)
            for g in genres:
                link_genre(cur, novel_id, g)
            conn.commit()
            total_novels += 1
            print(f"  + Novel: {title} (id={novel_id})")
        else:
            print(f"  = Novel exists: {title} (id={novel_id})")

        # Fetch chapters
        if chapter_exists(cur, novel_id):
            print(f"    Chapters already exist, skipping")
            continue

        print(f"    Fetching Gutenberg #{gid}...")
        text = fetch_gutenberg_text(gid)
        if not text:
            print(f"    FAILED to fetch text")
            continue

        chapters = parse_chapters(text)
        if not chapters:
            print(f"    No chapters parsed")
            continue

        # Max 30 chapters per book
        chapters = chapters[:30]
        for i, (ch_title, content, para_count) in enumerate(chapters):
            insert_chapter(cur, novel_id, i + 1, ch_title, content, para_count)
            total_chapters += 1

        conn.commit()
        print(f"    Saved {len(chapters)} chapters ({sum(c[2] for c in chapters)} paragraphs total)")
        time.sleep(2)  # Rate limit

    print()
    print("=" * 60)
    print("PHASE 2: Crawl Open Library novels (metadata + genres)")
    print("=" * 60)

    for genre, query in OL_QUERIES.items():
        print(f"\n  Genre: {genre}")
        novels = fetch_ol_novels(genre, query, limit=5)

        for title, author, desc, status, rating, cover_url, genres in novels:
            if novel_exists(cur, title):
                print(f"    = Exists: {title}")
                continue

            novel_id = insert_novel(cur, title, author, desc, status, rating, cover_url)
            for g in genres:
                link_genre(cur, novel_id, g)
            conn.commit()
            total_novels += 1
            print(f"    + {title} by {author} [{', '.join(genres)}]")

        time.sleep(1.5)

    print()
    print("=" * 60)
    print(f"DONE: {total_novels} novels, {total_chapters} chapters saved")
    print("=" * 60)

    cur.close()
    conn.close()


if __name__ == "__main__":
    main()
