# 開発ガイド

このドキュメントでは、開発環境のセットアップ方法、Bazelを使用したビルド方法、テスト方法、デプロイ方法について説明します。

## 目次

1. [開発環境のセットアップ](#開発環境のセットアップ)
2. [Bazelビルドシステム](#bazelビルドシステム)
3. [バックエンド開発](#バックエンド開発)
4. [フロントエンド開発](#フロントエンド開発)
5. [モバイルアプリ開発](#モバイルアプリ開発)
6. [データベース管理](#データベース管理)
7. [テスト](#テスト)
8. [ビルドとデプロイ](#ビルドとデプロイ)
9. [トラブルシューティング](#トラブルシューティング)

---

## 開発環境のセットアップ

### 必要なソフトウェア

以下のソフトウェアをインストールしてください：

#### 1. Bazel 6.0以上

**重要**: このプロジェクトは、Bazelをビルドシステムとして使用します。

**macOS (Homebrew使用):**
```bash
brew install bazelisk
```

**Ubuntu/Debian:**
```bash
sudo apt install apt-transport-https curl gnupg
curl -fsSL https://bazel.build/bazel-release.pub.gpg | gpg --dearmor > bazel.gpg
sudo mv bazel.gpg /etc/apt/trusted.gpg.d/
echo "deb [arch=amd64] https://storage.googleapis.com/bazel-apt stable jdk1.8" | sudo tee /etc/apt/sources.list.d/bazel.list
sudo apt update && sudo apt install bazel
```

**Windows:**
[Bazelisk](https://github.com/bazelbuild/bazelisk/releases)をダウンロードして、PATHに追加

**確認:**
```bash
bazel --version
# bazel 6.x.x または それ以上が表示されればOK
```

**Bazelとは？**

Bazelは、Googleが開発したビルドシステムです。以下の特徴があります：

- **増分ビルド**: 変更されたファイルのみを再ビルド
- **並列ビルド**: 複数のターゲットを並列にビルド
- **再現性**: 同じ入力から常に同じ出力を生成
- **キャッシュ**: ビルド結果をキャッシュして高速化
- **モノレポ対応**: 複数のプロジェクトを1つのリポジトリで管理

#### 2. Java Development Kit (JDK) 11以上

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

#### 3. PostgreSQL 14以上

データベースサーバーです。

**macOS (Homebrew使用):**
```bash
brew install postgresql@15
brew services start postgresql@15
```

**Ubuntu/Debian:**
```bash
sudo apt-get update
sudo apt-get install postgresql-15 postgresql-contrib-15
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

**Windows:**
[PostgreSQL公式サイト](https://www.postgresql.org/download/windows/)からインストーラーをダウンロード

**確認:**
```bash
psql --version
# psql (PostgreSQL) 15.x または それ以上が表示されればOK
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

## Bazelビルドシステム

### Bazelの基本概念

#### WORKSPACE ファイル

プロジェクトのルートにある`WORKSPACE`ファイルは、外部依存関係を定義します。

```python
# 例: Scala rules の定義
http_archive(
    name = "io_bazel_rules_scala",
    url = "https://github.com/bazelbuild/rules_scala/releases/download/v6.4.0/rules_scala-v6.4.0.tar.gz",
)
```

#### BUILD ファイル

各ディレクトリの`BUILD`ファイルは、ビルドターゲットを定義します。

```python
# 例: Scalaライブラリの定義
scala_library(
    name = "auth_library",
    srcs = ["src/main/scala/com/taskmanagement/auth/*.scala"],
    deps = ["@maven//:com_github_jwt_scala_jwt_core_2_13"],
)
```

#### .bazelrc ファイル

`.bazelrc`ファイルは、Bazelのビルドオプションを設定します。

```bash
# 例: 並列ジョブ数を自動設定
build --jobs=auto

# 例: ビルドキャッシュを有効化
build --disk_cache=~/.cache/bazel
```

### Bazelの基本コマンド

#### ビルド

```bash
# 特定のターゲットをビルド
bazel build //backend:taskmanagement_backend

# 全てのターゲットをビルド
bazel build //...

# 特定のディレクトリ配下をビルド
bazel build //backend/...
```

#### 実行

```bash
# バイナリを実行
bazel run //backend:taskmanagement_backend

# 引数を渡す
bazel run //backend:taskmanagement_backend -- --port 8081
```

#### テスト

```bash
# 特定のテストを実行
bazel test //backend:authentication_service_test

# 全てのテストを実行
bazel test //...

# テスト結果を詳細表示
bazel test //backend/... --test_output=all
```

#### クリーン

```bash
# ビルド成果物を削除
bazel clean

# 全てのキャッシュを削除（完全クリーン）
bazel clean --expunge
```

#### クエリ

```bash
# ターゲットの依存関係を表示
bazel query --output=graph //backend:taskmanagement_backend

# 全てのターゲットを表示
bazel query //...

# 特定のファイルに依存するターゲットを検索
bazel query 'rdeps(//..., //backend/src/main/scala/com/taskmanagement/models:Models.scala)'
```

### Bazelの設定プロファイル

`.bazelrc`で定義された設定プロファイルを使用できます。

```bash
# デバッグビルド
bazel build --config=debug //backend:taskmanagement_backend

# リリースビルド（最適化有効）
bazel build --config=release //backend:taskmanagement_backend

# 開発用高速ビルド
bazel build --config=dev //backend:taskmanagement_backend

# CI環境用
bazel build --config=ci //backend:taskmanagement_backend
```

---

## バックエンド開発

### 初回セットアップ

#### 1. データベースの作成

```bash
# PostgreSQLに接続（デフォルトユーザーで）
psql postgres

# データベースを作成
CREATE DATABASE taskmanagement;

# ユーザーを作成
CREATE USER taskuser WITH PASSWORD 'taskpass';

# 権限を付与
GRANT ALL PRIVILEGES ON DATABASE taskmanagement TO taskuser;

# 接続を確認
\c taskmanagement

# スキーマの権限を付与
GRANT ALL ON SCHEMA public TO taskuser;

# 終了
\q
```

#### 2. スキーマの適用

```bash
# プロジェクトのルートディレクトリから
psql -U taskuser -d taskmanagement -f database/schema.sql
```

成功すると、以下のようなメッセージが表示されます：
```
CREATE TABLE
CREATE INDEX
CREATE TRIGGER
...
```

#### 3. 設定ファイルの確認

`backend/src/main/resources/application.conf` を確認します：

```hocon
database {
  url = "jdbc:postgresql://localhost:5432/taskmanagement"
  user = "taskuser"
  password = "taskpass"
  driver = "org.postgresql.Driver"
}

server {
  interface = "0.0.0.0"
  port = 8080
}

jwt {
  secretKey = "your-secret-key-change-this-in-production"
  expirationSeconds = 86400  # 24時間
}
```

**重要:** `jwt.secretKey` は、本番環境では必ず変更してください。

### Bazelでのビルドと実行

#### ビルド

```bash
# バックエンドをビルド
bazel build //backend:taskmanagement_backend

# ビルド成果物の場所
# bazel-bin/backend/taskmanagement_backend
```

#### 実行

```bash
# バックエンドを起動
bazel run //backend:taskmanagement_backend

# サーバーが起動すると、以下のようなメッセージが表示されます：
# サーバーが起動しました: http://0.0.0.0:8080/
```

#### 開発時のワークフロー

```bash
# 1. コードを変更
# 2. ビルドして実行（Bazelが自動的に変更を検出）
bazel run //backend:taskmanagement_backend

# または、ビルドだけ実行してエラーチェック
bazel build //backend:taskmanagement_backend
```

### APIのテスト

別のターミナルを開いて、curlでAPIをテストします：

```bash
# ユーザー登録
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "registrationEmail": "test@example.com",
    "registrationPassword": "password123",
    "registrationFullName": "テストユーザー"
  }'

# レスポンス例:
# {
#   "loginToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#   "loginUser": {
#     "responseUserId": 1,
#     "responseUserEmail": "test@example.com",
#     "responseUserFullName": "テストユーザー",
#     "responseUserCreatedAt": "2024-01-15T10:30:00Z"
#   }
# }

# ログイン
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "loginEmail": "test@example.com",
    "loginPassword": "password123"
  }'

# 現在のユーザー情報取得（トークンが必要）
TOKEN="<上記で取得したトークン>"
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN"

# タスク作成
curl -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "creationTitle": "新しいタスク",
    "creationDescription": "タスクの説明",
    "creationProjectId": 1,
    "creationPriority": "high",
    "creationDueDate": "2024-12-31"
  }'

# 自分のタスク一覧取得
curl -X GET http://localhost:8080/api/tasks/my \
  -H "Authorization: Bearer $TOKEN"
```

### ライブラリの追加

新しいMaven依存関係を追加する場合は、`WORKSPACE`ファイルを編集します：

```python
maven_install(
    artifacts = [
        # 既存の依存関係...
        
        # 新しい依存関係を追加
        "com.example:new-library:1.0.0",
    ],
    repositories = [
        "https://repo1.maven.org/maven2",
    ],
)
```

---

## フロントエンド開発

### 初回セットアップ

```bash
# Webディレクトリに移動
cd web

# 依存関係のインストール（初回のみ）
npm install
```

### Bazelでの開発サーバー起動

```bash
# プロジェクトのルートディレクトリから
bazel run //web:dev_server

# サーバーが起動すると、以下のようなメッセージが表示されます：
# VITE v5.0.11  ready in 500 ms
# ➜  Local:   http://localhost:5173/
```

ブラウザで `http://localhost:5173` を開くと、アプリケーションが表示されます。

### ホットリロード

コードを変更すると、自動的にブラウザがリロードされます。

### 型チェック

```bash
# TypeScriptの型チェックを実行
bazel run //web:typecheck
```

### Lint

```bash
# ESLintでコード品質をチェック
bazel run //web:lint
```

### フォーマット

```bash
# Prettierでコードを整形
bazel run //web:format
```

### プロダクションビルド

```bash
# 最適化されたビルドを作成
bazel build //web:build_production

# ビルド成果物の場所
# bazel-bin/web/dist/
```

### プレビュー

```bash
# ビルドしたファイルをプレビュー
bazel run //web:preview_server

# ブラウザで http://localhost:4173 にアクセス
```

---

## モバイルアプリ開発

### 初回セットアップ

```bash
# Mobileディレクトリに移動
cd mobile

# 依存関係のインストール（初回のみ）
npm install --legacy-peer-deps
```

### Bazelでの開発サーバー起動

```bash
# プロジェクトのルートディレクトリから
bazel run //mobile:start

# QRコードが表示されます
```

### デバイスでの実行

#### Android

```bash
# Androidエミュレータで起動
bazel run //mobile:android

# または、実機で実行
# 1. Google PlayストアからExpo Goアプリをインストール
# 2. Expo Goアプリを開く
# 3. QRコードをスキャン
```

#### iOS (macOSのみ)

```bash
# iOSシミュレータで起動
bazel run //mobile:ios

# または、実機で実行
# 1. App StoreからExpo Goアプリをインストール
# 2. Expo Goアプリを開く
# 3. QRコードをスキャン
```

### 型チェック

```bash
# TypeScriptの型チェックを実行
bazel run //mobile:typecheck
```

### Lint

```bash
# ESLintでコード品質をチェック
bazel run //mobile:lint
```

### テスト

```bash
# Jestでユニットテストを実行
bazel run //mobile:test
```

---

## データベース管理

### データベースへの接続

```bash
psql -U taskuser -d taskmanagement
```

### よく使うSQLコマンド

```sql
-- テーブル一覧を表示
\dt

-- テーブルの構造を表示
\d users
\d tasks
\d projects

-- ユーザー一覧を表示
SELECT user_id, user_email, user_full_name FROM users;

-- タスク一覧を表示
SELECT task_id, task_title, task_status, task_priority FROM tasks;

-- プロジェクト一覧を表示
SELECT project_id, project_name, project_status FROM projects;

-- 特定のユーザーのタスクを表示
SELECT t.task_id, t.task_title, t.task_status
FROM tasks t
WHERE t.task_assigned_to_user_id = 1;

-- データベースをリセット（全データ削除）
TRUNCATE users, organizations, workspaces, projects, sections, tasks, 
         task_dependencies, tags, task_tags, comments, attachments, 
         notifications, activity_logs, user_sessions, 
         organization_members, workspace_members, project_members 
CASCADE;
```

### スキーマの再適用

データベースを完全にリセットする場合：

```bash
# データベースを削除
dropdb -U taskuser taskmanagement

# データベースを再作成
createdb -U taskuser taskmanagement

# スキーマを適用
psql -U taskuser -d taskmanagement -f database/schema.sql
```

### データベースのバックアップ

```bash
# バックアップを作成
pg_dump -U taskuser taskmanagement > backup.sql

# バックアップから復元
psql -U taskuser -d taskmanagement < backup.sql
```

---

## テスト

### バックエンドのテスト

```bash
# 特定のテストを実行
bazel test //backend:authentication_service_test

# 全てのテストを実行
bazel test //backend/...

# テスト結果を詳細表示
bazel test //backend/... --test_output=all

# カバレッジレポートを生成
bazel coverage //backend/...
```

### フロントエンドのテスト

```bash
# Webのテストを実行（将来実装）
# bazel test //web:test

# Mobileのテストを実行
bazel run //mobile:test
```

### 統合テスト

```bash
# 全てのコンポーネントのテストを実行
bazel test //...
```

---

## ビルドとデプロイ

### バックエンドのビルド

#### JARファイルの作成

```bash
# 実行可能なバイナリをビルド
bazel build //backend:taskmanagement_backend

# ビルド成果物の場所
# bazel-bin/backend/taskmanagement_backend
```

#### Dockerイメージの作成

```bash
# Dockerイメージをビルド
bazel build //backend:taskmanagement_backend_image

# イメージをDockerにロード
bazel run //backend:taskmanagement_backend_image

# コンテナを起動
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/taskmanagement \
  -e DATABASE_USER=taskuser \
  -e DATABASE_PASSWORD=taskpass \
  -e JWT_SECRET=your-secret-key \
  bazel/backend:taskmanagement_backend_image
```

### フロントエンドのビルド

```bash
# プロダクションビルドを作成
bazel build //web:build_production

# ビルド成果物の場所
# bazel-bin/web/dist/
```

### 静的ファイルのホスティング

#### Nginxの設定例

```nginx
server {
    listen 80;
    server_name example.com;
    root /path/to/bazel-bin/web/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

#### Vercelへのデプロイ

```bash
cd web
npm run build
vercel --prod
```

### モバイルアプリのビルド

#### Android APK

```bash
# Android APKをビルド
bazel run //mobile:build_android

# EAS (Expo Application Services) を使用
```

#### iOS IPA

```bash
# iOS IPAをビルド（macOSのみ）
bazel run //mobile:build_ios

# EAS (Expo Application Services) を使用
```

---

## トラブルシューティング

### Bazel関連

#### 問題: Bazelのビルドが遅い

**解決方法:**
```bash
# ビルドキャッシュを確認
bazel info

# キャッシュをクリーンアップ
bazel clean

# 並列ジョブ数を増やす
bazel build --jobs=16 //backend:taskmanagement_backend
```

#### 問題: 依存関係の解決エラー

```
ERROR: Unable to find package for @maven//:com_typesafe_akka_akka_http_2_13
```

**解決方法:**
```bash
# WORKSPACEファイルの依存関係を再取得
bazel sync

# または、完全クリーン
bazel clean --expunge
bazel build //backend:taskmanagement_backend
```

### バックエンド関連

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
   psql -U taskuser -d taskmanagement
   ```

3. `application.conf`の設定を確認

### フロントエンド関連

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
   ```bash
   curl http://localhost:8080/api/auth/me
   ```

2. CORSエラーの場合、バックエンドのCORS設定を確認

3. ブラウザのネットワークタブでリクエストを確認

### モバイルアプリ関連

#### 問題: Expoアプリで接続できない

**解決方法:**
1. デバイスとPCが同じWi-Fiネットワークに接続されているか確認
2. ファイアウォールがポート19000-19001をブロックしていないか確認
3. トンネルモードを使用：
   ```bash
   cd mobile
   npm start -- --tunnel
   ```

---

## 開発のベストプラクティス

### 1. コミット前のチェックリスト

```bash
# ビルドが通るか確認
bazel build //...

# テストが通るか確認
bazel test //...

# Lintエラーがないか確認
bazel run //web:lint
bazel run //mobile:lint

# 型チェックが通るか確認
bazel run //web:typecheck
bazel run //mobile:typecheck
```

### 2. ブランチ戦略

- `main`: 本番環境用
- `develop`: 開発環境用
- `feature/機能名`: 新機能開発用
- `bugfix/バグ名`: バグ修正用

### 3. コードレビュー

- 変数名がわかりやすいか
- 一般的な名前（data, result, tempなど）を避けているか
- 型が明示されているか
- エラーハンドリングが適切か
- テストが書かれているか

### 4. パフォーマンス最適化

```bash
# ビルド時間を計測
bazel build //backend:taskmanagement_backend --profile=profile.json

# プロファイルを分析
bazel analyze-profile profile.json
```

---

## まとめ

このガイドに従って開発を進めることで、Bazelを使用した効率的な開発ワークフローを実現できます。

**開発の流れ**:
1. Bazelで依存関係を管理
2. `bazel build` でビルド
3. `bazel run` で実行
4. `bazel test` でテスト
5. コミット前に全てのチェックを実行

**重要なポイント**:
- Bazelは増分ビルドを行うため、変更されたファイルのみを再ビルドします
- ビルドキャッシュを活用することで、ビルド時間を大幅に短縮できます
- モノレポ構成により、全てのコンポーネントを統一的に管理できます

このガイドを参考に、効率的な開発を進めてください。
