# Bazel WORKSPACE ファイル
#
# このファイルは、Bazelビルドシステムのルート設定ファイルです。
# 外部依存関係（ライブラリ、ツール）をここで定義します。
#
# 初心者向け解説:
# WORKSPACEファイルは、プロジェクト全体で使用する外部ライブラリや
# ビルドルールを定義する場所です。例えば、Scalaのコンパイラ、
# Node.jsのツール、Mavenリポジトリなどをここで設定します。

workspace(name = "portfolio1")

# ============================================================================
# Scala関連のルール
# ============================================================================

# rules_scalaは、BazelでScalaプロジェクトをビルドするためのルール集です
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# Scala rules のバージョン 6.4.0 を使用
http_archive(
    name = "io_bazel_rules_scala",
    sha256 = "9a23058a36183a556a9ba7229b4f204d3e68c8c6eb7b28260521016b38ef4e00",
    strip_prefix = "rules_scala-6.4.0",
    url = "https://github.com/bazelbuild/rules_scala/releases/download/v6.4.0/rules_scala-v6.4.0.tar.gz",
)

# Scala rules の初期化
load("@io_bazel_rules_scala//:scala_config.bzl", "scala_config")
scala_config(scala_version = "2.13.12")

load("@io_bazel_rules_scala//scala:scala.bzl", "scala_repositories")
scala_repositories()

load("@io_bazel_rules_scala//scala:toolchains.bzl", "scala_register_toolchains")
scala_register_toolchains()

# ============================================================================
# JVM外部依存関係（Maven）
# ============================================================================

# rules_jvm_externalは、MavenリポジトリからJavaライブラリを取得するためのルールです
http_archive(
    name = "rules_jvm_external",
    sha256 = "cd1a77b7b02e8e008439ca76fd34f5b07aecb8c752961f9640dea15e9e5ba1ca",
    strip_prefix = "rules_jvm_external-4.2",
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/4.2.zip",
)

load("@rules_jvm_external//:defs.bzl", "maven_install")

# Scalaバックエンドで使用するMaven依存関係を定義
maven_install(
    artifacts = [
        # Akka HTTP - HTTPサーバーフレームワーク
        "com.typesafe.akka:akka-actor-typed_2.13:2.8.5",
        "com.typesafe.akka:akka-stream_2.13:2.8.5",
        "com.typesafe.akka:akka-http_2.13:10.5.3",
        "com.typesafe.akka:akka-http-spray-json_2.13:10.5.3",
        
        # Slick - データベースORM
        "com.typesafe.slick:slick_2.13:3.5.0",
        "com.typesafe.slick:slick-hikaricp_2.13:3.5.0",
        
        # PostgreSQL ドライバー
        "org.postgresql:postgresql:42.7.1",
        
        # JWT認証
        "com.github.jwt-scala:jwt-core_2.13:9.4.5",
        
        # BCrypt - パスワードハッシュ化
        "org.mindrot:jbcrypt:0.4",
        
        # ロギング
        "ch.qos.logback:logback-classic:1.4.14",
        "com.typesafe.scala-logging:scala-logging_2.13:3.9.5",
        
        # JSON処理
        "io.spray:spray-json_2.13:1.3.6",
        
        # 設定ファイル読み込み
        "com.typesafe:config:1.4.3",
        
        # テスト用
        "org.scalatest:scalatest_2.13:3.2.17",
        "com.typesafe.akka:akka-http-testkit_2.13:10.5.3",
        "com.typesafe.akka:akka-actor-testkit-typed_2.13:2.8.5",
    ],
    repositories = [
        "https://repo1.maven.org/maven2",
        "https://maven-central.storage-download.googleapis.com/maven2",
    ],
)

# ============================================================================
# Node.js関連のルール（Web/Mobile用）
# ============================================================================

# rules_nodejsは、BazelでNode.jsプロジェクトをビルドするためのルールです
http_archive(
    name = "build_bazel_rules_nodejs",
    sha256 = "5dd1e5dea1322174c57d3ca7b899da381d516220793d0adef3ba03b9d23baa8e",
    urls = ["https://github.com/bazelbuild/rules_nodejs/releases/download/5.8.5/rules_nodejs-5.8.5.tar.gz"],
)

load("@build_bazel_rules_nodejs//:repositories.bzl", "build_bazel_rules_nodejs_dependencies")
build_bazel_rules_nodejs_dependencies()

load("@build_bazel_rules_nodejs//:index.bzl", "node_repositories", "npm_install")

# Node.js 18.19.0 を使用
node_repositories(
    node_version = "18.19.0",
)

# Webフロントエンドの依存関係をインストール
npm_install(
    name = "npm_web",
    package_json = "//web:package.json",
    package_lock_json = "//web:package-lock.json",
)

# モバイルアプリの依存関係をインストール
npm_install(
    name = "npm_mobile",
    package_json = "//mobile:package.json",
    package_lock_json = "//mobile:package-lock.json",
)

# ============================================================================
# Protocol Buffers（将来の拡張用）
# ============================================================================

# Protocol Buffersは、構造化データのシリアライズフォーマットです
# 将来的にgRPCを使う場合に必要になります
http_archive(
    name = "com_google_protobuf",
    sha256 = "bc3dbf1f09dba1b2eb3f2f70352ee97b9049066c9040ce0c9b67fb3294e91e4b",
    strip_prefix = "protobuf-3.15.8",
    urls = [
        "https://github.com/protocolbuffers/protobuf/archive/v3.15.8.tar.gz",
    ],
)

load("@com_google_protobuf//:protobuf_deps.bzl", "protobuf_deps")
protobuf_deps()

# ============================================================================
# Docker（将来のコンテナ化用）
# ============================================================================

# rules_dockerは、BazelでDockerイメージをビルドするためのルールです
http_archive(
    name = "io_bazel_rules_docker",
    sha256 = "b1e80761a8a8243d03ebca8845e9cc1ba6c82ce7c5179ce2b295cd36f7e394bf",
    urls = ["https://github.com/bazelbuild/rules_docker/releases/download/v0.25.0/rules_docker-v0.25.0.tar.gz"],
)

load(
    "@io_bazel_rules_docker//repositories:repositories.bzl",
    container_repositories = "repositories",
)
container_repositories()

load("@io_bazel_rules_docker//repositories:deps.bzl", container_deps = "deps")
container_deps()

load(
    "@io_bazel_rules_docker//container:container.bzl",
    "container_pull",
)

# OpenJDK 11のベースイメージ（バックエンド用）
container_pull(
    name = "java_base",
    registry = "gcr.io",
    repository = "distroless/java11-debian11",
    digest = "sha256:161a1d97d592b3f1919801578c3a47c8e932071168a96267698f4b669c24c76d",
)

# Node.js 18のベースイメージ（フロントエンド用）
container_pull(
    name = "nodejs_base",
    registry = "index.docker.io",
    repository = "library/node",
    tag = "18-alpine",
)
