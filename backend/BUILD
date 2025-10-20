# Bazel BUILD ファイル - バックエンド（Scala）
#
# このファイルは、Scalaバックエンドをビルドするためのルールを定義します。
#
# 初心者向け解説:
# BUILDファイルは、各ディレクトリでどのようにコードをビルドするかを
# 定義するファイルです。ソースファイル、依存関係、ビルドターゲットを
# 指定します。

load("@io_bazel_rules_scala//scala:scala.bzl", "scala_binary", "scala_library", "scala_test")
# Docker image support temporarily disabled
# load("@io_bazel_rules_docker//scala:image.bzl", "scala_image")

# ============================================================================
# ライブラリターゲット: 認証関連
# ============================================================================

# JWTAuthenticationHandlerとPasswordHashingUtilityを含むライブラリ
scala_library(
    name = "auth_library",
    srcs = [
        "src/main/scala/com/taskmanagement/auth/JwtAuthenticationHandler.scala",
        "src/main/scala/com/taskmanagement/auth/PasswordHashingUtility.scala",
    ],
    deps = [
        "@maven//:com_github_jwt_scala_jwt_core_2_13",
        "@maven//:org_mindrot_jbcrypt",
        "@maven//:com_typesafe_config",
        "@maven//:ch_qos_logback_logback_classic",
        "@maven//:com_typesafe_scala_logging_scala_logging_2_13",
    ],
    visibility = ["//visibility:public"],
)

# ============================================================================
# ライブラリターゲット: データベーススキーマ
# ============================================================================

# Slickのテーブル定義を含むライブラリ
scala_library(
    name = "database_schema_library",
    srcs = [
        "src/main/scala/com/taskmanagement/db/DatabaseSchema.scala",
    ],
    deps = [
        "@maven//:com_typesafe_slick_slick_2_13",
        "@maven//:com_typesafe_slick_slick_hikaricp_2_13",
        "@maven//:org_postgresql_postgresql",
    ],
    visibility = ["//visibility:public"],
)

# ============================================================================
# ライブラリターゲット: モデル定義
# ============================================================================

# 全てのケースクラス（モデル）を含むライブラリ
scala_library(
    name = "models_library",
    srcs = [
        "src/main/scala/com/taskmanagement/models/Models.scala",
    ],
    visibility = ["//visibility:public"],
)

# ============================================================================
# ライブラリターゲット: サービス層
# ============================================================================

# 認証サービス
scala_library(
    name = "authentication_service_library",
    srcs = [
        "src/main/scala/com/taskmanagement/services/AuthenticationService.scala",
    ],
    deps = [
        ":auth_library",
        ":database_schema_library",
        ":models_library",
        "@maven//:com_typesafe_slick_slick_2_13",
        "@maven//:org_apache_pekko_pekko_actor_typed_2_13",
        "@maven//:com_typesafe_scala_logging_scala_logging_2_13",
    ],
    visibility = ["//visibility:public"],
)

# タスク管理サービス
scala_library(
    name = "task_management_service_library",
    srcs = [
        "src/main/scala/com/taskmanagement/services/TaskManagementService.scala",
    ],
    deps = [
        ":database_schema_library",
        ":models_library",
        "@maven//:com_typesafe_slick_slick_2_13",
        "@maven//:org_apache_pekko_pekko_actor_typed_2_13",
        "@maven//:com_typesafe_scala_logging_scala_logging_2_13",
    ],
    visibility = ["//visibility:public"],
)

# プロジェクト管理サービス
scala_library(
    name = "project_management_service_library",
    srcs = [
        "src/main/scala/com/taskmanagement/services/ProjectManagementService.scala",
    ],
    deps = [
        ":database_schema_library",
        ":models_library",
        "@maven//:com_typesafe_slick_slick_2_13",
        "@maven//:org_apache_pekko_pekko_actor_typed_2_13",
        "@maven//:com_typesafe_scala_logging_scala_logging_2_13",
    ],
    visibility = ["//visibility:public"],
)

# ============================================================================
# ライブラリターゲット: API層
# ============================================================================

# JSONプロトコル（シリアライズ/デシリアライズ）
scala_library(
    name = "json_protocols_library",
    srcs = [
        "src/main/scala/com/taskmanagement/api/JsonProtocols.scala",
    ],
    deps = [
        ":models_library",
        "@maven//:org_apache_pekko_pekko_http_spray_json_2_13",
        "@maven//:io_spray_spray_json_2_13",
    ],
    visibility = ["//visibility:public"],
)

# APIルート定義
scala_library(
    name = "routes_library",
    srcs = [
        "src/main/scala/com/taskmanagement/api/Routes.scala",
    ],
    deps = [
        ":auth_library",
        ":authentication_service_library",
        ":task_management_service_library",
        ":project_management_service_library",
        ":json_protocols_library",
        ":models_library",
        "@maven//:org_apache_pekko_pekko_http_2_13",
        "@maven//:org_apache_pekko_pekko_stream_2_13",
        "@maven//:com_typesafe_scala_logging_scala_logging_2_13",
    ],
    visibility = ["//visibility:public"],
)

# ============================================================================
# バイナリターゲット: メインアプリケーション
# ============================================================================

# 実行可能なバックエンドアプリケーション
# このターゲットをビルドすると、HTTPサーバーが起動できるJARファイルが生成されます
scala_binary(
    name = "taskmanagement_backend",
    srcs = [
        "src/main/scala/com/taskmanagement/Main.scala",
    ],
    main_class = "com.taskmanagement.Main",
    resources = [
        "src/main/resources/application.conf",
    ],
    deps = [
        ":auth_library",
        ":database_schema_library",
        ":models_library",
        ":authentication_service_library",
        ":task_management_service_library",
        ":project_management_service_library",
        ":routes_library",
        ":json_protocols_library",
        "@maven//:org_apache_pekko_pekko_actor_typed_2_13",
        "@maven//:org_apache_pekko_pekko_stream_2_13",
        "@maven//:org_apache_pekko_pekko_http_2_13",
        "@maven//:com_typesafe_slick_slick_2_13",
        "@maven//:com_typesafe_slick_slick_hikaricp_2_13",
        "@maven//:org_postgresql_postgresql",
        "@maven//:com_typesafe_config",
        "@maven//:ch_qos_logback_logback_classic",
        "@maven//:com_typesafe_scala_logging_scala_logging_2_13",
    ],
    visibility = ["//visibility:public"],
)

# ============================================================================
# Dockerイメージターゲット
# ============================================================================

# バックエンドのDockerイメージ
# このターゲットをビルドすると、デプロイ可能なDockerイメージが生成されます
# scala_image(
#     name = "taskmanagement_backend_image",
#     srcs = [
#         "src/main/scala/com/taskmanagement/Main.scala",
#     ],
#     main_class = "com.taskmanagement.Main",
#     resources = [
#         "src/main/resources/application.conf",
#     ],
#     deps = [
#         ":auth_library",
#         ":database_schema_library",
#         ":models_library",
#         ":authentication_service_library",
#         ":task_management_service_library",
#         ":project_management_service_library",
#         ":routes_library",
#         ":json_protocols_library",
#         "@maven//:org_apache_pekko_pekko_actor_typed_2_13",
#         "@maven//:org_apache_pekko_pekko_stream_2_13",
#         "@maven//:org_apache_pekko_pekko_http_2_13",
#         "@maven//:com_typesafe_slick_slick_2_13",
#         "@maven//:com_typesafe_slick_slick_hikaricp_2_13",
#         "@maven//:org_postgresql_postgresql",
#         "@maven//:com_typesafe_config",
#         "@maven//:ch_qos_logback_logback_classic",
#         "@maven//:com_typesafe_scala_logging_scala_logging_2_13",
#     ],
#     base = "@java_base//image",
#     visibility = ["//visibility:public"],
# )

# ============================================================================
# テストターゲット
# ============================================================================

# # 認証サービスのテスト
# scala_test(
#     name = "authentication_service_test",
#     srcs = glob(["src/test/scala/com/taskmanagement/services/AuthenticationServiceTest.scala"]),
#     deps = [
#         ":authentication_service_library",
#         ":auth_library",
#         ":database_schema_library",
#         ":models_library",
#         "@maven//:org_scalatest_scalatest_2_13",
#         "@maven//:org_apache_pekko_pekko_actor_testkit_typed_2_13",
#     ],
#     size = "small",
# )
# 
# # タスク管理サービスのテスト
# scala_test(
#     name = "task_management_service_test",
#     srcs = glob(["src/test/scala/com/taskmanagement/services/TaskManagementServiceTest.scala"]),
#     deps = [
#         ":task_management_service_library",
#         ":database_schema_library",
#         ":models_library",
#         "@maven//:org_scalatest_scalatest_2_13",
#         "@maven//:org_apache_pekko_pekko_actor_testkit_typed_2_13",
#     ],
#     size = "small",
# )
# 
# # APIルートのテスト
# scala_test(
#     name = "routes_test",
#     srcs = glob(["src/test/scala/com/taskmanagement/api/RoutesTest.scala"]),
#     deps = [
#         ":routes_library",
#         ":authentication_service_library",
#         ":task_management_service_library",
#         ":project_management_service_library",
#         ":json_protocols_library",
#         ":models_library",
#         "@maven//:org_scalatest_scalatest_2_13",
#         "@maven//:org_apache_pekko_pekko_http_testkit_2_13",
#     ],
#     size = "medium",
# )

# ============================================================================
# ビルド方法（コメント）
# ============================================================================

# このBUILDファイルの使い方:
#
# 1. バックエンドをビルドする:
#    bazel build //backend:taskmanagement_backend
#
# 2. バックエンドを実行する:
#    bazel run //backend:taskmanagement_backend
#
# 3. テストを実行する:
#    bazel test //backend:authentication_service_test
#    bazel test //backend:task_management_service_test
#    bazel test //backend:routes_test
#    bazel test //backend/...  # 全てのテストを実行
#
# 4. Dockerイメージをビルドする:
#    bazel build //backend:taskmanagement_backend_image
#
# 5. 特定のライブラリだけをビルドする:
#    bazel build //backend:auth_library
#
# ビルド成果物の場所:
# - バイナリ: bazel-bin/backend/taskmanagement_backend
# - Dockerイメージ: bazel-bin/backend/taskmanagement_backend_image
