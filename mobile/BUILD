# Bazel BUILD ファイル - モバイルアプリ（React Native + Expo）
#
# このファイルは、React Nativeモバイルアプリをビルドするためのルールを定義します。
#
# 初心者向け解説:
# モバイルアプリは、AndroidやiOSで動作するネイティブアプリケーションです。
# React Native（クロスプラットフォームフレームワーク）とExpo（開発ツール）を使用しています。
# JavaScriptで書いたコードが、ネイティブアプリとして動作します。

load("@build_bazel_rules_nodejs//:index.bzl", "nodejs_binary")
load("@npm_mobile//:index.bzl", "npm_link_all_packages")

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
        "package.json",
        "tsconfig.json",
        "babel.config.js",
        "app.json",
        "index.js",
    ],
)

# 全てのソースファイル
filegroup(
    name = "all_sources",
    srcs = [
        ":typescript_sources",
        ":config_files",
    ],
)

# ============================================================================
# ビルドターゲット: Expo開発サーバー
# ============================================================================

# Expo開発サーバーを起動するターゲット
# QRコードをスキャンして、実機やエミュレータでアプリを確認できます
nodejs_binary(
    name = "start",
    data = [
        ":all_sources",
        ":node_modules",
    ],
    entry_point = ":node_modules/expo/bin/cli.js",
    args = [
        "start",
    ],
    visibility = ["//visibility:public"],
)

# ============================================================================
# ビルドターゲット: Android開発
# ============================================================================

# Android向けにExpo開発サーバーを起動するターゲット
# Androidエミュレータまたは実機で直接起動します
nodejs_binary(
    name = "android",
    data = [
        ":all_sources",
        ":node_modules",
    ],
    entry_point = ":node_modules/expo/bin/cli.js",
    args = [
        "start",
        "--android",
    ],
    visibility = ["//visibility:public"],
)

# ============================================================================
# ビルドターゲット: iOS開発
# ============================================================================

# iOS向けにExpo開発サーバーを起動するターゲット
# iOSシミュレータまたは実機で直接起動します（macOSのみ）
nodejs_binary(
    name = "ios",
    data = [
        ":all_sources",
        ":node_modules",
    ],
    entry_point = ":node_modules/expo/bin/cli.js",
    args = [
        "start",
        "--ios",
    ],
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
        ".eslintrc.js",
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
# ビルドターゲット: Jest（ユニットテスト）
# ============================================================================

# Jestでユニットテストを実行するターゲット
# コンポーネントやロジックの動作を検証します
nodejs_binary(
    name = "test",
    data = [
        ":all_sources",
        ":node_modules",
        "jest.config.js",
    ],
    entry_point = ":node_modules/jest/bin/jest.js",
    args = [
        "--coverage",
    ],
    visibility = ["//visibility:public"],
)

# ============================================================================
# ビルドターゲット: APKビルド（Android）
# ============================================================================

# Android用のAPKファイルをビルドするターゲット
# Expo Application Services (EAS)を使用してビルドします
nodejs_binary(
    name = "build_android",
    data = [
        ":all_sources",
        ":node_modules",
    ],
    entry_point = ":node_modules/expo/bin/cli.js",
    args = [
        "build:android",
        "--profile",
        "production",
    ],
    visibility = ["//visibility:public"],
)

# ============================================================================
# ビルドターゲット: IPAビルド（iOS）
# ============================================================================

# iOS用のIPAファイルをビルドするターゲット
# Expo Application Services (EAS)を使用してビルドします
nodejs_binary(
    name = "build_ios",
    data = [
        ":all_sources",
        ":node_modules",
    ],
    entry_point = ":node_modules/expo/bin/cli.js",
    args = [
        "build:ios",
        "--profile",
        "production",
    ],
    visibility = ["//visibility:public"],
)

# ============================================================================
# ビルド方法（コメント）
# ============================================================================

# このBUILDファイルの使い方:
#
# 1. Expo開発サーバーを起動する:
#    bazel run //mobile:start
#    → QRコードが表示されるので、Expoアプリでスキャン
#
# 2. Androidエミュレータで起動する:
#    bazel run //mobile:android
#    → Android Studioのエミュレータが必要
#
# 3. iOSシミュレータで起動する（macOSのみ）:
#    bazel run //mobile:ios
#    → Xcodeのシミュレータが必要
#
# 4. TypeScript型チェックを実行する:
#    bazel run //mobile:typecheck
#
# 5. ESLintでコード品質をチェックする:
#    bazel run //mobile:lint
#
# 6. Jestでテストを実行する:
#    bazel run //mobile:test
#
# 7. Android APKをビルドする:
#    bazel run //mobile:build_android
#    → EASアカウントが必要
#
# 8. iOS IPAをビルドする:
#    bazel run //mobile:build_ios
#    → EASアカウントとApple Developer Programが必要
#
# 開発ワークフロー:
# 1. bazel run //mobile:start で開発サーバーを起動
# 2. Expoアプリ（iOS/Android）でQRコードをスキャン
# 3. コードを編集すると自動的にアプリが更新される
# 4. 定期的に bazel run //mobile:typecheck でエラーチェック
# 5. コミット前に bazel run //mobile:lint でコード品質を確認
# 6. デプロイ前に bazel run //mobile:build_android または build_ios でビルド
#
# 注意事項:
# - React Nativeの開発には、Android StudioまたはXcodeが必要です
# - 実機でテストする場合は、Expoアプリをインストールしてください
# - プロダクションビルドには、Expo Application Services (EAS)のアカウントが必要です
