==ye# SE2 Software User Guide

## 1. Giới thiệu
SE2 là ứng dụng web quản lý và đọc tiểu thuyết sử dụng Spring Boot, Thymeleaf, PostgreSQL với pgvector cho tìm kiếm vector embeddings. Các tính năng chính:
- Đăng ký/đăng nhập (local, OAuth2 Google/GitHub), quên mật khẩu.
- Quản lý tiểu thuyết: xem chi tiết, chương, bình luận, đánh giá, bookmark, favorite.
- Dịch chương, lịch sử dịch, tiến độ đọc.
- Tìm kiếm nâng cao (embedding-based).
- Admin panel: quản lý novels, users.
- Cài đặt đọc: theme, font, spacing.
- Thông báo, upload file.

App chạy trên port 9000 (prod) hoặc 8080 (dev mặc định).

## 2. Yêu cầu hệ thống (Prerequisites)
- **JDK 21+** (từ pom.xml).
- **Maven 3.9+** (sử dụng mvnw wrapper).
- **PostgreSQL 15+** với [pgvector extension](https://github.com/pgvector/pgvector) (cho embeddings).
- **Docker** (optional, cho DB nhanh).
- **Ollama** (optional, http://localhost:11434, model nomic-embed-text cho AI features).
- **Git** (clone repo).
- **Node.js không cần** (pure Java/Spring).

## 3. Cài đặt & Setup
### 3.1 Clone project
```
cd d:/SE2/SE2_Thymleaf/
git clone <repo-url> SE2  # hoặc đã có
cd SE2
```

### 3.2 Tạo file .env (tại root project)
```
GOOGLE_CLIENT_ID=your_google_id
GOOGLE_CLIENT_SECRET=your_google_secret
GITHUB_CLIENT_ID=your_github_id
GITHUB_CLIENT_SECRET=your_github_secret
MAIL_USERNAME=your_gmail@gmail.com
MAIL_PASSWORD=your_app_password
```
- Lấy Google/GitHub OAuth từ console.
- Gmail App Password (enable 2FA).

### 3.3 Database Setup
**Option 1: Local Postgres**
- Cài Postgres, enable pgvector: `CREATE EXTENSION vector;`
- Tạo DB `se2-new`, user `postgres` pw `123456`.
- Hoặc chỉnh `application.yml`.

**Option 2: Docker (khuyến nghị)**
```
docker-compose up -d postgres
```
- DB accessible tại localhost:5433/se2, user postgres/123456.
- init.sql tự run.

Dev sử dụng H2 in-memory (profile dev).

## 4. Chạy ứng dụng Locally
### 4.1 Build & Run Dev (H2 DB)
```
mvnw spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=--spring.profiles.active=dev
```
- Port: 8080 (mặc định).
- H2 Console: http://localhost:8080/h2-console (JDBC: jdbc:h2:mem:se2dev).

### 4.2 Run Prod (Postgres)
```
mvnw spring-boot:run
```
- Port: **http://localhost:9000**.
- Đảm bảo DB chạy, .env set.

**Hot reload** enabled in dev.

## 5. Docker Full Stack (nếu extend docker-compose.yml)
docker-compose chỉ có DB hiện tại. Để containerize app:
```
# Thêm service app vào docker-compose.yml
  app:
    build: .
    ports:
      - \"9000:9000\"
    depends_on:
      - postgres
    environment:
      SPRING_PROFILES_ACTIVE: postgres  # custom profile nếu cần
```
```
docker-compose up --build
```

## 6. Truy cập ứng dụng
- **Prod**: http://localhost:9000
- **Dev**: http://localhost:8080
- Trang chủ: danh sách novels.
- Đăng ký/login để dùng đầy đủ.

## 7. Tài khoản mặc định / Data Seed
- App có DataSeed/DataSeeder tự seed data mẫu (roles, genres, novels?).
- Check logs sau run đầu: `Data seeding completed`.
- Admin mặc định: check console hoặc tạo manual.

## 8. Sử dụng tính năng
- **Đọc novel**: /novels/{id} → chapters → đọc chapter.
- **Tìm kiếm**: /search → filter genre/status.
- **User**: /user/profile, /user/translations, favorites.
- **Admin**: /admin (nếu role ADMIN).
- **Upload dịch**: /user/translation-submit.
- **Settings**: appearance, reading progress tự save.

## 9. Troubleshooting
- **Port occupied**: Kill process or change server.port.
- **DB connection**: Check docker ps, pg_isready, credentials.
- **OAuth fail**: Set đúng .env, callback URLs: http://localhost:9000/login/oauth2/code/google.
- **Ollama errors**: Install Ollama, `ollama pull nomic-embed-text`.
- **mvnw fail**: `mvnw clean install`.
- **Embeddings**: Ensure pgvector installed in DB.
- **Logs**: Check console, application logs in target/.

## 10. Build JAR & Deploy
```
mvnw clean package -DskipTests
java -jar target/demo-0.0.1-SNAPSHOT.jar
```
(Demo là artifactId từ pom.xml).

Cập nhật README.md với link guide này.

---
*Hướng dẫn cập nhật: 2026. Cảm ơn sử dụng SE2!*

