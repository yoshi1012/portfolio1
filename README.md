# Asana風タスク管理アプリケーション

このプロジェクトは、Asanaの設計思想に基づいて構築されたタスク管理アプリケーションです。
モノレポ構成で、バックエンド（Scala）、Webフロントエンド（React）、モバイルアプリ（React Native）を含みます。

## 📚 ドキュメント

このプロジェクトには、初心者でも理解しやすいように詳細なドキュメントが用意されています。

### 必読ドキュメント

1. **[Asanaの設計思想](docs/ASANA_PHILOSOPHY.md)** - Asanaの設計思想とこのプロジェクトへの適用方法
2. **[開発ガイド](docs/DEVELOPMENT.md)** - 開発環境のセットアップと開発手順
3. **[アーキテクチャ設計](docs/ARCHITECTURE.md)** - システムアーキテクチャの詳細
4. **[コーディングガイドライン](docs/CODING_GUIDELINES.md)** - コーディング規約と命名規則
5. **[実装ガイド](docs/IMPLEMENTATION_GUIDE.md)** - 一から実装する際のステップバイステップガイド

## 🎯 プロジェクトの目的

このプロジェクトは、以下の目的で作成されました：

1. **学習用リファレンス**: Asanaのような大規模タスク管理システムの設計を学ぶ
2. **技術スタックの実践**: Scala、React、React Nativeを使った実践的な開発
3. **清書用テンプレート**: 後で一から実装し直す際の参考資料

## 🏗️ 技術スタック

### バックエンド
- **言語**: Scala 2.13.12
- **フレームワーク**: Akka HTTP 10.5
- **データベース**: PostgreSQL 15+
- **ORM**: Slick 3.5
- **認証**: JWT (JSON Web Token) + BCrypt
- **ビルドツール**: Bazel 6.0+

### Webフロントエンド
- **言語**: TypeScript
- **フレームワーク**: React 18
- **ビルドツール**: Vite
- **スタイリング**: Tailwind CSS
- **HTTPクライアント**: Axios

### モバイルアプリ
- **言語**: TypeScript
- **フレームワーク**: React Native (Expo)
- **プラットフォーム**: iOS / Android

### データベース
- **RDBMS**: PostgreSQL 15+
- **スキーマ管理**: SQLスクリプト
- **テーブル数**: 17テーブル（users, tasks, projects, etc.）

### ビルドシステム
- **Bazel**: モノレポ全体の統合ビルド
- **増分ビルド**: 変更されたファイルのみを再ビルド
- **並列ビルド**: 複数のターゲットを並列にビルド
- **キャッシュ**: ビルド結果をキャッシュして高速化

## 🚀 クイックスタート

### 前提条件

- **Bazel 6.0+** (ビルドシステム) - **必須**
- **Java 11+** (Scalaの実行に必要)
- **PostgreSQL 15+** (データベース)
- **Node.js 18+** (フロントエンドのビルドに必要)

### 1. データベースのセットアップ

```bash
# データベースとユーザーを作成
psql postgres
CREATE DATABASE taskmanagement;
CREATE USER taskuser WITH PASSWORD 'taskpass';
GRANT ALL PRIVILEGES ON DATABASE taskmanagement TO taskuser;
\q

# スキーマを適用
psql -U taskuser -d taskmanagement -f database/schema.sql
```

### 2. バックエンドの起動（Bazel使用）

```bash
# バックエンドをビルドして起動
bazel run //backend:taskmanagement_backend

# サーバーが http://localhost:8080 で起動します
```

### 3. Webフロントエンドの起動（Bazel使用）

```bash
# 開発サーバーを起動
bazel run //web:dev_server

# ブラウザで http://localhost:5173 にアクセス
```

### 4. モバイルアプリの起動（Bazel使用）

```bash
# Expo開発サーバーを起動
bazel run //mobile:start

# QRコードをスキャンして実機で確認
```

## 🔧 Bazelコマンド

### ビルド

```bash
# 全てのコンポーネントをビルド
bazel build //...

# 特定のコンポーネントをビルド
bazel build //backend:taskmanagement_backend
bazel build //web:build_production
```

### テスト

```bash
# 全てのテストを実行
bazel test //...

# バックエンドのテストのみ実行
bazel test //backend/...
```

### クリーン

```bash
# ビルド成果物を削除
bazel clean

# 完全クリーン（全てのキャッシュを削除）
bazel clean --expunge
```

## 📝 主な機能

- ユーザー認証（JWT）
- プロジェクト管理
- タスク管理（作成、更新、完了、削除）
- 通知システム
- アクティビティログ

詳細は各ドキュメントを参照してください。
