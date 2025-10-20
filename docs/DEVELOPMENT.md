# 開発ガイド

このドキュメントでは、開発環境のセットアップ方法、ビルド方法、テスト方法、デプロイ方法について説明します。

## 目次

1. [開発環境のセットアップ](#開発環境のセットアップ)
2. [バックエンド開発](#バックエンド開発)
3. [フロントエンド開発](#フロントエンド開発)
4. [モバイルアプリ開発](#モバイルアプリ開発)
5. [データベース管理](#データベース管理)
6. [テスト](#テスト)
7. [ビルドとデプロイ](#ビルドとデプロイ)
8. [トラブルシューティング](#トラブルシューティング)

---

## 開発環境のセットアップ

### 必要なソフトウェア

以下のソフトウェアをインストールしてください：

#### 1. Java Development Kit (JDK) 11以上

Scalaの実行に必要です。

**macOS (Homebrew使用):**
```bash
brew install openjdk@11
```

**Ubuntu/Debian:**
```bash
sudo apt-get update
sudo apt-get install openjdk-11-jdk
```

**Windows:**
[Oracle JDK](https://www.oracle.com/java/technologies/downloads/)または[OpenJDK](https://adoptium.net/)からダウンロードしてインストール

**確認:**
```bash
java -version
# java version "11.0.x" または それ以上が表示されればOK
```

#### 2. SBT (Scala Build Tool) 1.9以上

Scalaプロジェクトのビルドツールです。

**macOS (Homebrew使用):**
```bash
brew install sbt
```

**Ubuntu/Debian:**
```bash
echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | sudo tee /etc/apt/sources.list.d/sbt.list
curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo apt-key add
sudo apt-get update
sudo apt-get install sbt
```

**Windows:**
[SBT公式サイト](https://www.scala-sbt.org/download.html)からインストーラーをダウンロード

**確認:**
```bash
sbt --version
# sbt version 1.9.x または それ以上が表示されればOK
```

#### 3. PostgreSQL 14以上

データベースサーバーです。

**macOS (Homebrew使用):**
```bash
brew install postgresql@14
brew services start postgresql@14
```

**Ubuntu/Debian:**
```bash
sudo apt-get update
sudo apt-get install postgresql-14 postgresql-contrib-14
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

**Windows:**
[PostgreSQL公式サイト](https://www.postgresql.org/download/windows/)からインストーラーをダウンロード

**確認:**
```bash
psql --version
# psql (PostgreSQL) 14.x または それ以上が表示されればOK
```

#### 4. Node.js 18以上

フロントエンド開発に必要です。

**macOS (Homebrew使用):**
```bash
brew install node@18
```

**Ubuntu/Debian:**
```bash
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs
```

**Windows:**
[Node.js公式サイト](https://nodejs.org/)からインストーラーをダウンロード

**確認:**
```bash
node --version
# v18.x.x または それ以上が表示されればOK

npm --version
# 9.x.x または それ以上が表示されればOK
```

---

## バックエンド開発

### 初回セットアップ

#### 1. データベースの作成

```bash
# PostgreSQLに接続（デフォルトユーザーで）
psql postgres

# データベースを作成
CREATE DATABASE taskmanagement_db;

# 接続を確認
\c taskmanagement_db

# 終了
\q
```

#### 2. スキーマの適用

```bash
# プロジェクトのルートディレクトリから
psql -d taskmanagement_db -f database/schema.sql
```

成功すると、以下のようなメッセージが表示されます：
```
CREATE EXTENSION
CREATE TABLE
CREATE INDEX
...
INSERT 0 3
```

#### 3. 環境変数の設定

`.env`ファイルを`backend/`ディレクトリに作成します：

```bash
cd backend
cat > .env << 'EOF'
DATABASE_URL=jdbc:postgresql://localhost:5432/taskmanagement_db
DATABASE_USER=postgres
DATABASE_PASSWORD=your_password_here
JWT_SECRET=your-secret-key-change-this-in-production-make-it-long-and-random
JWT_EXPIRATION_HOURS=24
SERVER_INTERFACE=0.0.0.0
SERVER_PORT=8080
EOF
```

**重要:** `DATABASE_PASSWORD`と`JWT_SECRET`は必ず変更してください。

### 開発サーバーの起動

```bash
cd backend

# 環境変数を読み込む
export $(cat .env | xargs)

# SBTを起動
sbt

# SBTシェル内で以下を実行
> compile  # 初回コンパイル（時間がかかります）
> run      # サーバーを起動
```

サーバーが起動すると、以下のようなメッセージが表示されます：
```
✓ サーバーが起動しました: http://0.0.0.0:8080/
```

### APIのテスト

別のターミナルを開いて、curlでAPIをテストします：

```bash
# ヘルスチェック
curl http://localhost:8080/health

# ユーザー登録
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "userEmail": "test@example.com",
    "userFullName": "テストユーザー",
    "plainTextPassword": "password123"
  }'

# レスポンス例:
# {
#   "jwtToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#   "userInfo": {
#     "userId": 4,
#     "userEmail": "test@example.com",
#     "userFullName": "テストユーザー",
#     ...
#   },
#   "sessionExpiresAt": 1234567890
# }
```

### コードの変更

SBTは自動的にコードの変更を検出しません。変更後は以下を実行してください：

```bash
# SBTシェル内で
> reStart  # サーバーを再起動
```

または、SBTシェルを終了して`sbt run`を再実行します。

### ログの確認

ログは標準出力に表示されます。ログレベルは`src/main/resources/application.conf`で設定できます：

```hocon
akka {
  loglevel = "DEBUG"  # DEBUG, INFO, WARNING, ERROR
}
```

---

## フロントエンド開発

### 初回セットアップ

```bash
cd web

# 依存関係のインストール
npm install
```

### 開発サーバーの起動

```bash
npm run dev
```

サーバーが起動すると、以下のようなメッセージが表示されます：
```
VITE v5.0.11  ready in 500 ms

➜  Local:   http://localhost:3000/
➜  Network: use --host to expose
```

ブラウザで `http://localhost:3000` を開くと、アプリケーションが表示されます。

### ホットリロード

コードを変更すると、自動的にブラウザがリロードされます。

### ビルド

本番用のビルドを作成するには：

```bash
npm run build
```

ビルドされたファイルは`dist/`ディレクトリに出力されます。

### プレビュー

ビルドしたファイルをプレビューするには：

```bash
npm run preview
```

---

## モバイルアプリ開発

### 初回セットアップ

```bash
cd mobile

# 依存関係のインストール
npm install
```

### Expoの起動

```bash
npm start
```

QRコードが表示されます。

### デバイスでの実行

#### Android

1. Google PlayストアからExpo Goアプリをインストール
2. Expo Goアプリを開く
3. QRコードをスキャン

#### iOS

1. App StoreからExpo Goアプリをインストール
2. Expo Goアプリを開く
3. QRコードをスキャン

### エミュレーターでの実行

#### Android Emulator

```bash
npm run android
```

#### iOS Simulator (macOSのみ)

```bash
npm run ios
```

---

## データベース管理

### データベースへの接続

```bash
psql -d taskmanagement_db
```

### よく使うSQLコマンド

```sql
-- テーブル一覧を表示
\dt

-- テーブルの構造を表示
\d users

-- ユーザー一覧を表示
SELECT user_id, user_email, user_full_name FROM users;

-- タスク一覧を表示
SELECT task_id, task_title, current_task_status FROM tasks;

-- データベースをリセット（全データ削除）
TRUNCATE users, organizations, workspaces, projects, tasks CASCADE;
```

### スキーマの再適用

データベースを完全にリセットする場合：

```bash
# データベースを削除
dropdb taskmanagement_db

# データベースを再作成
createdb taskmanagement_db

# スキーマを適用
psql -d taskmanagement_db -f database/schema.sql
```

---

## テスト

### バックエンドのテスト

```bash
cd backend
sbt test
```

### フロントエンドのテスト

```bash
cd web
npm test
```

---

## ビルドとデプロイ

### バックエンドのビルド

実行可能なJARファイルを作成します：

```bash
cd backend
sbt assembly
```

JARファイルは`target/scala-2.13/taskmanagement-server.jar`に生成されます。

### JARファイルの実行

```bash
# 環境変数を設定
export DATABASE_URL="jdbc:postgresql://localhost:5432/taskmanagement_db"
export DATABASE_USER="postgres"
export DATABASE_PASSWORD="your_password"
export JWT_SECRET="your-secret-key"

# JARファイルを実行
java -jar target/scala-2.13/taskmanagement-server.jar
```

### フロントエンドのビルド

```bash
cd web
npm run build
```

ビルドされたファイルは`dist/`ディレクトリに出力されます。

### 静的ファイルのホスティング

Nginxの設定例：

```nginx
server {
    listen 80;
    server_name example.com;
    root /path/to/portfolio1/web/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## トラブルシューティング

### バックエンドが起動しない

#### 問題: ポートが既に使用されている

```
java.net.BindException: Address already in use
```

**解決方法:**
```bash
# ポート8080を使用しているプロセスを確認
lsof -i :8080

# プロセスを終了
kill -9 <PID>
```

または、別のポートを使用：
```bash
export SERVER_PORT=8081
sbt run
```

#### 問題: データベースに接続できない

```
org.postgresql.util.PSQLException: Connection refused
```

**解決方法:**
1. PostgreSQLが起動しているか確認
   ```bash
   # macOS
   brew services list
   
   # Linux
   sudo systemctl status postgresql
   ```

2. 接続情報が正しいか確認
   ```bash
   psql -d taskmanagement_db -U postgres
   ```

3. `pg_hba.conf`の設定を確認（必要に応じて）

### フロントエンドが起動しない

#### 問題: 依存関係のインストールエラー

```
npm ERR! code ERESOLVE
```

**解決方法:**
```bash
# node_modulesとpackage-lock.jsonを削除
rm -rf node_modules package-lock.json

# 再インストール
npm install
```

#### 問題: APIに接続できない

ブラウザのコンソールに以下のエラーが表示される：
```
Failed to fetch
```

**解決方法:**
1. バックエンドが起動しているか確認
2. `vite.config.ts`のプロキシ設定を確認
3. ブラウザのネットワークタブでリクエストを確認

### モバイルアプリが起動しない

#### 問題: Expoアプリで接続できない

**解決方法:**
1. デバイスとPCが同じWi-Fiネットワークに接続されているか確認
2. ファイアウォールがポート19000-19001をブロックしていないか確認
3. トンネルモードを使用：
   ```bash
   npm start -- --tunnel
   ```

---

## 開発のベストプラクティス

### 1. コミット前のチェックリスト

- [ ] コードがコンパイルできる
- [ ] テストが通る
- [ ] リンターエラーがない
- [ ] コミットメッセージが明確

### 2. ブランチ戦略

- `main`: 本番環境用
- `develop`: 開発環境用
- `feature/機能名`: 新機能開発用
- `bugfix/バグ名`: バグ修正用

### 3. コードレビュー

プルリクエストを作成する際は、以下を含めてください：

- 変更内容の説明
- テスト方法
- スクリーンショット（UI変更の場合）
- 関連するIssue番号

---

## 次のステップ

開発環境のセットアップが完了したら、以下のドキュメントを参照してください：

- [アーキテクチャ設計](ARCHITECTURE.md) - システムの全体像を理解する
- [コーディングガイドライン](CODING_GUIDELINES.md) - コーディング規約を学ぶ
- [実装ガイド](IMPLEMENTATION_GUIDE.md) - 一から実装する方法を学ぶ
