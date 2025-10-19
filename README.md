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
- **データベース**: PostgreSQL 14+
- **ORM**: Slick 3.5
- **認証**: JWT (JSON Web Token)
- **ビルドツール**: SBT / Bazel

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
- **RDBMS**: PostgreSQL 14+
- **スキーマ管理**: SQLスクリプト

## 🚀 クイックスタート

### 前提条件

- **Java 11+** (Scalaの実行に必要)
- **SBT 1.9+** (Scalaのビルドツール)
- **PostgreSQL 14+** (データベース)
- **Node.js 18+** (フロントエンドのビルドに必要)

### 1. データベースのセットアップ

```bash
createdb taskmanagement_db
psql -d taskmanagement_db -f database/schema.sql
```

### 2. バックエンドの起動

```bash
cd backend
export DATABASE_URL="jdbc:postgresql://localhost:5432/taskmanagement_db"
export DATABASE_USER="postgres"
export DATABASE_PASSWORD="your_password"
export JWT_SECRET="your-secret-key"
sbt run
```

### 3. Webフロントエンドの起動

```bash
cd web
npm install
npm run dev
```

## 📝 主な機能

- ユーザー認証（JWT）
- プロジェクト管理
- タスク管理（作成、更新、完了、削除）
- 通知システム
- アクティビティログ

詳細は各ドキュメントを参照してください。
