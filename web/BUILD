# Bazel BUILD ファイル - Webフロントエンド（React + TypeScript）
#
# このファイルは、React Webフロントエンドをビルドするためのルールを定義します。
#
# 初心者向け解説:
# Webフロントエンドは、ユーザーがブラウザで見る画面を構築する部分です。
# React（UIライブラリ）とTypeScript（型安全なJavaScript）を使用しています。
# Viteというツールでビルドし、静的ファイル（HTML/CSS/JS）を生成します。

load("@build_bazel_rules_nodejs//:index.bzl", "nodejs_binary", "npm_package_bin")
load("@npm_web//:index.bzl", "npm_link_all_packages")

# ============================================================================
# パッケージリンク
# ============================================================================

# package.jsonで定義された全てのnpmパッケージをBazelで使えるようにする
npm_link_all_packages(name = "node_modules")

# ============================================================================
# ソースファイルのグループ化
# ============================================================================

# TypeScriptソースファイル
filegroup(
    name = "typescript_sources",
    srcs = glob([
        "src/**/*.ts",
        "src/**/*.tsx",
    ]),
)

# 設定ファイル
filegroup(
    name = "config_files",
    srcs = [
        "tsconfig.json",
        "tsconfig.node.json",
        "vite.config.ts",
        "tailwind.config.js",
        "postcss.config.js",
        "index.html",
    ],
)

# CSSファイル
filegroup(
    name = "css_files",
    srcs = glob([
        "src/**/*.css",
    ]),
)

# 全てのソースファイル
filegroup(
    name = "all_sources",
    srcs = [
        ":typescript_sources",
        ":config_files",
        ":css_files",
        "package.json",
    ],
)

# ============================================================================
# ビルドターゲット: 開発サーバー
# ============================================================================

# Vite開発サーバーを起動するターゲット
# ホットリロード機能付きで、コードを変更すると自動的にブラウザが更新されます
nodejs_binary(
    name = "dev_server",
    data = [
        ":all_sources",
        ":node_modules",
    ],
    entry_point = ":node_modules/vite/bin/vite.js",
    args = [
        "--host",
        "0.0.0.0",
        "--port",
        "5173",
    ],
    visibility = ["//visibility:public"],
)

# ============================================================================
# ビルドターゲット: プロダクションビルド
# ============================================================================

# Viteでプロダクション用にビルドするターゲット
# 最適化された静的ファイル（HTML/CSS/JS）を生成します
npm_package_bin(
    name = "build_production",
    tool = ":node_modules/vite/bin/vite.js",
    args = [
        "build",
        "--outDir",
        "$(RULEDIR)/dist",
    ],
    data = [
        ":all_sources",
        ":node_modules",
    ],
    outs = ["dist"],
    visibility = ["//visibility:public"],
)

# ============================================================================
# ビルドターゲット: TypeScript型チェック
# ============================================================================

# TypeScriptの型チェックを実行するターゲット
# コンパイルエラーや型の不整合を検出します
nodejs_binary(
    name = "typecheck",
    data = [
        ":all_sources",
        ":node_modules",
    ],
    entry_point = ":node_modules/typescript/bin/tsc",
    args = [
        "--noEmit",
        "--project",
        "tsconfig.json",
    ],
    visibility = ["//visibility:public"],
)

# ============================================================================
# ビルドターゲット: ESLint（コード品質チェック）
# ============================================================================

# ESLintでコードの品質をチェックするターゲット
# コーディング規約違反や潜在的なバグを検出します
nodejs_binary(
    name = "lint",
    data = [
        ":all_sources",
        ":node_modules",
        ".eslintrc.cjs",
    ],
    entry_point = ":node_modules/eslint/bin/eslint.js",
    args = [
        "src",
        "--ext",
        ".ts,.tsx",
    ],
    visibility = ["//visibility:public"],
)

# ============================================================================
# ビルドターゲット: Prettier（コードフォーマット）
# ============================================================================

# Prettierでコードを自動整形するターゲット
# インデント、改行、スペースなどを統一します
nodejs_binary(
    name = "format",
    data = [
        ":all_sources",
        ":node_modules",
        ".prettierrc",
    ],
    entry_point = ":node_modules/prettier/bin-prettier.js",
    args = [
        "--write",
        "src/**/*.{ts,tsx,css}",
    ],
    visibility = ["//visibility:public"],
)

# ============================================================================
# ビルドターゲット: プレビューサーバー
# ============================================================================

# ビルドした成果物をプレビューするサーバー
# プロダクションビルドの動作確認に使用します
nodejs_binary(
    name = "preview_server",
    data = [
        ":build_production",
        ":node_modules",
    ],
    entry_point = ":node_modules/vite/bin/vite.js",
    args = [
        "preview",
        "--host",
        "0.0.0.0",
        "--port",
        "4173",
    ],
    visibility = ["//visibility:public"],
)

# ============================================================================
# テストターゲット（将来の拡張用）
# ============================================================================

# Vitestでユニットテストを実行するターゲット
# 現在はテストファイルがないため、将来の拡張用として定義
# nodejs_binary(
#     name = "test",
#     data = [
#         ":all_sources",
#         ":node_modules",
#     ],
#     entry_point = ":node_modules/vitest/vitest.mjs",
#     args = [
#         "run",
#     ],
#     visibility = ["//visibility:public"],
# )

# ============================================================================
# ビルド方法（コメント）
# ============================================================================

# このBUILDファイルの使い方:
#
# 1. 開発サーバーを起動する（ホットリロード有効）:
#    bazel run //web:dev_server
#    → ブラウザで http://localhost:5173 にアクセス
#
# 2. プロダクション用にビルドする:
#    bazel build //web:build_production
#    → 成果物は bazel-bin/web/dist/ に生成されます
#
# 3. TypeScript型チェックを実行する:
#    bazel run //web:typecheck
#
# 4. ESLintでコード品質をチェックする:
#    bazel run //web:lint
#
# 5. Prettierでコードを整形する:
#    bazel run //web:format
#
# 6. プロダクションビルドをプレビューする:
#    bazel build //web:build_production
#    bazel run //web:preview_server
#    → ブラウザで http://localhost:4173 にアクセス
#
# 開発ワークフロー:
# 1. bazel run //web:dev_server で開発サーバーを起動
# 2. コードを編集すると自動的にブラウザが更新される
# 3. 定期的に bazel run //web:typecheck でエラーチェック
# 4. コミット前に bazel run //web:lint でコード品質を確認
# 5. デプロイ前に bazel build //web:build_production でビルド
