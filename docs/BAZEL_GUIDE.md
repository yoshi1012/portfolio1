# Bazel + Bzlmodビルドシステム完全ガイド

このドキュメントでは、このプロジェクトで使用しているBazel 8.3.1とBzlmod（モダンなBazelモジュールシステム）について、初心者向けに詳しく解説します。

## 目次

1. [Bazelとは](#bazelとは)
2. [Bzlmodとは](#bzlmodとは)
3. [プロジェクトのBazel設定](#プロジェクトのbazel設定)
4. [MODULE.bazelの詳細解説](#modulebazelの詳細解説)
5. [BUILDファイルの詳細解説](#buildファイルの詳細解説)
6. [.bazelrcの設定](#bazelrcの設定)
7. [依存関係の追加方法](#依存関係の追加方法)
8. [よくある問題と解決方法](#よくある問題と解決方法)

---

## Bazelとは

Bazelは、Googleが開発したオープンソースのビルドシステムです。大規模なモノレポ（複数のプロジェクトを1つのリポジトリで管理）に最適化されています。

### Bazelの主な特徴

1. **増分ビルド**: 変更されたファイルとその依存関係のみを再ビルドします
2. **並列ビルド**: 複数のターゲットを並列にビルドして高速化します
3. **再現性**: 同じ入力から常に同じ出力を生成します（決定論的ビルド）
4. **キャッシュ**: ビルド結果をキャッシュして、2回目以降のビルドを高速化します
5. **言語横断**: Scala、TypeScript、Go、Rustなど、複数の言語を統一的にビルドできます

### なぜBazelを使うのか？

このプロジェクトは、以下の理由でBazelを採用しています：

- **モノレポ構成**: バックエンド（Scala）、Web（React）、モバイル（React Native）を1つのリポジトリで管理
- **ビルド速度**: 変更されたファイルのみを再ビルドするため、大規模プロジェクトでも高速
- **依存関係管理**: 各コンポーネント間の依存関係を明確に定義できる
- **再現性**: 誰がビルドしても同じ結果が得られる

---

## Bzlmodとは

**Bzlmod**（Bazel Module）は、Bazel 6.0以降で導入された**モダンなモジュールシステム**です。

### 従来のWORKSPACEとの違い

| 項目 | WORKSPACE（旧方式） | Bzlmod（新方式） |
|------|---------------------|------------------|
| 設定ファイル | `WORKSPACE` | `MODULE.bazel` |
| 依存関係の解決 | 手動で全て定義 | 自動的に推移的依存関係を解決 |
| バージョン管理 | 困難 | 簡単（セマンティックバージョニング対応） |
| 依存関係の衝突 | 手動で解決 | 自動的に解決 |
| 学習コスト | 高い | 低い |

### Bzlmodのメリット

1. **シンプルな設定**: 必要な依存関係を宣言するだけで、推移的依存関係は自動的に解決されます
2. **バージョン管理**: セマンティックバージョニングに対応し、依存関係のバージョンを明確に管理できます
3. **モジュールレジストリ**: Bazel Central Registry（BCR）から公式モジュールを簡単にインストールできます
4. **依存関係の可視化**: `bazel mod graph`コマンドで依存関係を可視化できます

---

## プロジェクトのBazel設定

このプロジェクトのBazel設定ファイルは、以下の3つです：

```
portfolio1/
├── MODULE.bazel      # Bzlmodモジュール定義（依存関係管理）
├── .bazelrc          # Bazelの設定オプション
└── .bazelversion     # 使用するBazelバージョン（8.3.1）
```

### .bazelversion

`.bazelversion`ファイルは、使用するBazelのバージョンを指定します。

```
8.3.1
```

**重要**: Bazeliskを使用している場合、このファイルで指定されたバージョンが自動的にダウンロードされて使用されます。

### .bazelrc

`.bazelrc`ファイルは、Bazelのビルドオプションを設定します。

```bash
# Bzlmodを有効化（必須）
common --enable_bzlmod

# サンドボックスデバッグ（ビルドエラーの調査に便利）
build --sandbox_debug
```

**`--enable_bzlmod`**: Bzlmodを有効化するフラグです。このプロジェクトでは必須です。

---

## MODULE.bazelの詳細解説

`MODULE.bazel`ファイルは、プロジェクトのモジュール定義と依存関係を管理します。

### 完全な設定内容

```python
# モジュール定義
module(
    name = "portfolio1",
    version = "0.1.0",
)

# Bazel依存関係（Bazel Central Registryから取得）
bazel_dep(name = "rules_scala", version = "7.0.0")
bazel_dep(name = "rules_jvm_external", version = "6.3")

# Maven依存関係の設定
maven = use_extension("@rules_jvm_external//:extensions.bzl", "maven")
maven.install(
    name = "maven_portfolio",
    artifacts = [
        # Pekko Actor（クラシックアクターシステム）
        "org.apache.pekko:pekko-actor_2.13:1.0.3",
        
        # Pekko Actor Typed（型付きアクターシステム）
        "org.apache.pekko:pekko-actor-typed_2.13:1.0.3",
        
        # Pekko Stream（ストリーム処理）
        "org.apache.pekko:pekko-stream_2.13:1.0.3",
        
        # Pekko HTTP Core（HTTPモデル定義）
        "org.apache.pekko:pekko-http-core_2.13:1.0.1",
        
        # Pekko HTTP（HTTPサーバー）
        "org.apache.pekko:pekko-http_2.13:1.0.1",
        
        # Pekko HTTP Spray JSON（JSONシリアライゼーション）
        "org.apache.pekko:pekko-http-spray-json_2.13:1.0.1",
        
        # Spray JSON（JSON処理）
        "io.spray:spray-json_2.13:1.3.6",
        
        # Slick（データベースORM）
        "com.typesafe.slick:slick_2.13:3.5.0",
        "com.typesafe.slick:slick-hikaricp_2.13:3.5.0",
        
        # PostgreSQL JDBCドライバー
        "org.postgresql:postgresql:42.7.1",
        
        # JWT認証
        "com.github.jwt-scala:jwt-core_2.13:9.4.5",
        
        # BCrypt（パスワードハッシュ化）
        "com.github.t3hnar:scala-bcrypt_2.13:4.3.0",
        
        # Typesafe Config（設定ファイル読み込み）
        "com.typesafe:config:1.4.3",
        
        # ロギング
        "ch.qos.logback:logback-classic:1.4.14",
        "ch.qos.logback:logback-core:1.4.14",
        
        # テストフレームワーク
        "org.scalatest:scalatest_2.13:3.2.17",
        "org.scalatest:scalatest-wordspec_2.13:3.2.17",
        "org.scalatest:scalatest-funspec_2.13:3.2.17",
    ],
    repositories = [
        "https://repo1.maven.org/maven2",
    ],
)

# Mavenリポジトリを使用可能にする
use_repo(maven, "maven_portfolio")

# Scala バージョン設定
scala_config = use_extension("@rules_scala//scala/extensions:config.bzl", "scala_config")
scala_config.settings(scala_version = "2.13.16")

# Scala コンパイラとランタイムをセットアップ
scala_deps = use_extension("@rules_scala//scala/extensions:deps.bzl", "scala_deps")
scala_deps.scala()
```

### 各セクションの詳細解説

#### 1. モジュール定義

```python
module(
    name = "portfolio1",
    version = "0.1.0",
)
```

- **`name`**: このプロジェクトのモジュール名
- **`version`**: プロジェクトのバージョン（セマンティックバージョニング）

#### 2. Bazel依存関係

```python
bazel_dep(name = "rules_scala", version = "7.0.0")
bazel_dep(name = "rules_jvm_external", version = "6.3")
```

- **`rules_scala`**: Scalaをビルドするためのルール（Bazel Central Registryから取得）
- **`rules_jvm_external`**: Maven依存関係を管理するためのルール

#### 3. Maven依存関係

```python
maven = use_extension("@rules_jvm_external//:extensions.bzl", "maven")
maven.install(
    name = "maven_portfolio",
    artifacts = [
        "org.apache.pekko:pekko-actor_2.13:1.0.3",
        # ...
    ],
    repositories = [
        "https://repo1.maven.org/maven2",
    ],
)
use_repo(maven, "maven_portfolio")
```

- **`use_extension`**: `rules_jvm_external`のMaven拡張機能を使用
- **`maven.install`**: Maven依存関係をインストール
  - **`name`**: リポジトリ名（`@maven_portfolio`として参照）
  - **`artifacts`**: Maven座標のリスト（`groupId:artifactId:version`形式）
  - **`repositories`**: Mavenリポジトリのリスト
- **`use_repo`**: インストールしたリポジトリを使用可能にする

**重要**: Scala 2.13用のライブラリは、`_2.13`サフィックスが必要です。
- 例: `pekko-actor_2.13`（Scala 2.13用）
- 例: `pekko-actor_2.12`（Scala 2.12用）

#### 4. Scala設定

```python
scala_config = use_extension("@rules_scala//scala/extensions:config.bzl", "scala_config")
scala_config.settings(scala_version = "2.13.16")

scala_deps = use_extension("@rules_scala//scala/extensions:deps.bzl", "scala_deps")
scala_deps.scala()
```

- **`scala_config.settings`**: Scalaのバージョンを設定（2.13.16）
- **`scala_deps.scala()`**: Scalaコンパイラとランタイムをセットアップ

---

## BUILDファイルの詳細解説

各ディレクトリの`BUILD`ファイルは、ビルドターゲットを定義します。

### backend/BUILD の例

```python
load("@rules_scala//scala:scala.bzl", "scala_library", "scala_binary", "scala_test")

# モデル層のライブラリ
scala_library(
    name = "models_library",
    srcs = ["src/main/scala/com/taskmanagement/models/Models.scala"],
    deps = [
        "@maven_portfolio//:io_spray_spray_json_2_13",
    ],
)

# 認証ライブラリ
scala_library(
    name = "auth_library",
    srcs = glob(["src/main/scala/com/taskmanagement/auth/*.scala"]),
    deps = [
        ":models_library",
        "@maven_portfolio//:com_github_jwt_scala_jwt_core_2_13",
        "@maven_portfolio//:com_github_t3hnar_scala_bcrypt_2_13",
        "@maven_portfolio//:io_spray_spray_json_2_13",
    ],
)

# データベースライブラリ
scala_library(
    name = "database_library",
    srcs = ["src/main/scala/com/taskmanagement/db/DatabaseSchema.scala"],
    deps = [
        ":models_library",
        "@maven_portfolio//:com_typesafe_slick_slick_2_13",
        "@maven_portfolio//:org_postgresql_postgresql",
    ],
)

# サービス層ライブラリ
scala_library(
    name = "services_library",
    srcs = glob(["src/main/scala/com/taskmanagement/services/*.scala"]),
    deps = [
        ":models_library",
        ":database_library",
        ":auth_library",
        "@maven_portfolio//:com_typesafe_slick_slick_2_13",
        "@maven_portfolio//:org_apache_pekko_pekko_actor_2_13",
    ],
)

# API層ライブラリ
scala_library(
    name = "routes_library",
    srcs = glob(["src/main/scala/com/taskmanagement/api/*.scala"]),
    deps = [
        ":models_library",
        ":services_library",
        ":auth_library",
        "@maven_portfolio//:org_apache_pekko_pekko_http_core_2_13",
        "@maven_portfolio//:org_apache_pekko_pekko_http_2_13",
        "@maven_portfolio//:org_apache_pekko_pekko_http_spray_json_2_13",
        "@maven_portfolio//:io_spray_spray_json_2_13",
    ],
)

# メインバイナリ
scala_binary(
    name = "taskmanagement_backend",
    srcs = ["src/main/scala/com/taskmanagement/Main.scala"],
    main_class = "com.taskmanagement.Main",
    resources = glob(["src/main/resources/**/*"]),
    deps = [
        ":models_library",
        ":database_library",
        ":services_library",
        ":routes_library",
        ":auth_library",
        "@maven_portfolio//:org_apache_pekko_pekko_actor_2_13",
        "@maven_portfolio//:org_apache_pekko_pekko_http_2_13",
        "@maven_portfolio//:org_apache_pekko_pekko_http_spray_json_2_13",
        "@maven_portfolio//:io_spray_spray_json_2_13",
        "@maven_portfolio//:com_typesafe_slick_slick_2_13",
        "@maven_portfolio//:com_typesafe_slick_slick_hikaricp_2_13",
        "@maven_portfolio//:org_postgresql_postgresql",
        "@maven_portfolio//:com_typesafe_config",
        "@maven_portfolio//:ch_qos_logback_logback_classic",
        "@maven_portfolio//:ch_qos_logback_logback_core",
    ],
)

# テスト
scala_test(
    name = "authentication_service_test",
    srcs = ["src/test/scala/com/taskmanagement/services/AuthenticationServiceTest.scala"],
    deps = [
        ":services_library",
        ":models_library",
        ":database_library",
        "@maven_portfolio//:org_scalatest_scalatest_2_13",
        "@maven_portfolio//:org_scalatest_scalatest_wordspec_2_13",
    ],
)
```

### 各ターゲットの説明

#### scala_library

```python
scala_library(
    name = "models_library",
    srcs = ["src/main/scala/com/taskmanagement/models/Models.scala"],
    deps = [
        "@maven_portfolio//:io_spray_spray_json_2_13",
    ],
)
```

- **`name`**: ライブラリの名前（他のターゲットから`:models_library`として参照）
- **`srcs`**: ソースファイルのリスト（`glob()`で複数ファイルを指定可能）
- **`deps`**: 依存関係のリスト
  - `:models_library`（同じBUILDファイル内のターゲット）
  - `@maven_portfolio//:io_spray_spray_json_2_13`（Maven依存関係）

#### scala_binary

```python
scala_binary(
    name = "taskmanagement_backend",
    srcs = ["src/main/scala/com/taskmanagement/Main.scala"],
    main_class = "com.taskmanagement.Main",
    resources = glob(["src/main/resources/**/*"]),
    deps = [
        # ...
    ],
)
```

- **`name`**: バイナリの名前（`bazel run //backend:taskmanagement_backend`で実行）
- **`srcs`**: メインクラスのソースファイル
- **`main_class`**: エントリーポイントのクラス名
- **`resources`**: リソースファイル（`application.conf`など）
- **`deps`**: 依存関係のリスト

#### scala_test

```python
scala_test(
    name = "authentication_service_test",
    srcs = ["src/test/scala/com/taskmanagement/services/AuthenticationServiceTest.scala"],
    deps = [
        ":services_library",
        "@maven_portfolio//:org_scalatest_scalatest_2_13",
    ],
)
```

- **`name`**: テストの名前（`bazel test //backend:authentication_service_test`で実行）
- **`srcs`**: テストファイルのリスト
- **`deps`**: 依存関係のリスト（テストフレームワークを含む）

---

## 依存関係の追加方法

### 1. Maven依存関係の追加

新しいMaven依存関係を追加する場合は、`MODULE.bazel`を編集します。

```python
# MODULE.bazel
maven.install(
    name = "maven_portfolio",
    artifacts = [
        # 既存の依存関係...
        "org.apache.pekko:pekko-actor_2.13:1.0.3",
        
        # 新しい依存関係を追加
        "com.example:new-library_2.13:1.0.0",
    ],
    repositories = [
        "https://repo1.maven.org/maven2",
    ],
)
```

### 2. BUILDファイルでの使用

追加した依存関係を使用するには、`BUILD`ファイルの`deps`に追加します。

```python
scala_library(
    name = "my_library",
    srcs = ["MyClass.scala"],
    deps = [
        "@maven_portfolio//:com_example_new_library_2_13",
    ],
)
```

**注意**: Maven座標の`:`と`.`は`_`に変換されます。
- `com.example:new-library_2.13:1.0.0` → `@maven_portfolio//:com_example_new_library_2_13`

### 3. 依存関係の確認

```bash
# 依存関係のグラフを表示
bazel query --output=graph //backend:taskmanagement_backend

# 特定のターゲットの依存関係を表示
bazel query 'deps(//backend:taskmanagement_backend)'
```

---

## よくある問題と解決方法

### 問題1: `compatibility_proxy`エラー

```
ERROR: Failed to load Starlark extension '@@compatibility_proxy//:proxy.bzl'.
Cycle in the workspace file detected.
```

**原因**: WORKSPACEファイルとBzlmodを混在させている

**解決方法**: WORKSPACEファイルを削除し、Bzlmodのみを使用する

```bash
rm WORKSPACE
```

### 問題2: `object model is not a member of package org.apache.pekko.http.scaladsl`

```
error: object model is not a member of package org.apache.pekko.http.scaladsl
```

**原因**: `pekko-http-core`が依存関係に含まれていない

**解決方法**: `MODULE.bazel`と`BUILD`ファイルに`pekko-http-core`を追加

```python
# MODULE.bazel
"org.apache.pekko:pekko-http-core_2.13:1.0.1",

# backend/BUILD
"@maven_portfolio//:org_apache_pekko_pekko_http_core_2_13",
```

### 問題3: `ActorSystem not found`

```
error: object ActorSystem is not a member of package org.apache.pekko.actor
```

**原因**: `pekko-actor`（クラシックアクターシステム）が依存関係に含まれていない

**解決方法**: `MODULE.bazel`に`pekko-actor`を追加

```python
# MODULE.bazel
"org.apache.pekko:pekko-actor_2.13:1.0.3",
```

### 問題4: Scala 2.13のライブラリが見つからない

```
error: Could not find artifact com.example:library:jar:1.0.0
```

**原因**: Scala 2.13用のライブラリは`_2.13`サフィックスが必要

**解決方法**: Maven座標に`_2.13`を追加

```python
# 誤り
"com.example:library:1.0.0"

# 正しい
"com.example:library_2.13:1.0.0"
```

### 問題5: ビルドキャッシュの問題

```
ERROR: Analysis of target '//backend:taskmanagement_backend' failed
```

**解決方法**: ビルドキャッシュをクリアして再ビルド

```bash
# キャッシュをクリア
bazel clean --expunge

# 再ビルド
bazel build //backend:taskmanagement_backend
```

---

## まとめ

このプロジェクトは、**Bazel 8.3.1 + Bzlmod**を使用しています。

### 重要なポイント

1. **WORKSPACEファイルは使用しない**: Bzlmodのみを使用
2. **MODULE.bazelで依存関係を管理**: Maven依存関係は`maven.install()`で定義
3. **Scala 2.13用のライブラリは`_2.13`サフィックスが必要**
4. **BUILDファイルでターゲットを定義**: `scala_library`、`scala_binary`、`scala_test`

### 参考リンク

- [Bazel公式ドキュメント](https://bazel.build/)
- [Bzlmodガイド](https://bazel.build/external/module)
- [rules_scala](https://github.com/bazelbuild/rules_scala)
- [rules_jvm_external](https://github.com/bazelbuild/rules_jvm_external)
- [Bazel Central Registry](https://registry.bazel.build/)

このガイドを参考に、Bazelビルドシステムを理解し、効率的に開発を進めてください。
