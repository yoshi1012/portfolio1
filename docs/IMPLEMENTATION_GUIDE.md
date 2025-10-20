# 実装ガイド

このドキュメントでは、Asana風タスク管理アプリケーションを一から実装する際の手順を、初心者向けに詳しく解説します。

## 目次

1. [実装の全体フロー](#実装の全体フロー)
2. [データベースの実装](#データベースの実装)
3. [バックエンドの実装](#バックエンドの実装)
4. [Webフロントエンドの実装](#webフロントエンドの実装)
5. [モバイルアプリの実装](#モバイルアプリの実装)
6. [テストの実装](#テストの実装)
7. [デプロイ](#デプロイ)
8. [トラブルシューティング](#トラブルシューティング)

---

## 実装の全体フロー

アプリケーションを実装する際は、以下の順序で進めることを推奨します。

```
1. データベース設計
   ↓
2. バックエンド（認証機能）
   ↓
3. バックエンド（タスク管理機能）
   ↓
4. バックエンド（プロジェクト管理機能）
   ↓
5. Webフロントエンド（認証画面）
   ↓
6. Webフロントエンド（タスク管理画面）
   ↓
7. Webフロントエンド（プロジェクト管理画面）
   ↓
8. モバイルアプリ（認証画面）
   ↓
9. モバイルアプリ（タスク管理画面）
   ↓
10. テスト
   ↓
11. デプロイ
```

**なぜこの順序なのか？**

1. **データベースから始める理由**: データ構造が決まらないと、バックエンドのモデルが定義できません
2. **認証から始める理由**: 全ての機能は認証が前提となるため、最初に実装します
3. **バックエンドを先に実装する理由**: フロントエンドはバックエンドのAPIに依存するため、APIを先に完成させます
4. **Webを先に実装する理由**: デバッグがしやすく、開発速度が速いためです

---

## データベースの実装

### ステップ1: PostgreSQLのインストールと起動

**macOSの場合**:
```bash
# Homebrewでインストール
brew install postgresql@15

# PostgreSQLを起動
brew services start postgresql@15

# データベースに接続
psql postgres
```

**Linuxの場合**:
```bash
# PostgreSQLをインストール
sudo apt update
sudo apt install postgresql postgresql-contrib

# PostgreSQLを起動
sudo systemctl start postgresql

# データベースに接続
sudo -u postgres psql
```

**Windowsの場合**:
1. [PostgreSQL公式サイト](https://www.postgresql.org/download/windows/)からインストーラーをダウンロード
2. インストーラーを実行し、指示に従ってインストール
3. pgAdmin 4を起動してデータベースに接続

### ステップ2: データベースとユーザーの作成

PostgreSQLに接続したら、以下のコマンドを実行します。

```sql
-- データベースを作成
CREATE DATABASE taskmanagement;

-- ユーザーを作成
CREATE USER taskuser WITH PASSWORD 'taskpass';

-- ユーザーに権限を付与
GRANT ALL PRIVILEGES ON DATABASE taskmanagement TO taskuser;

-- データベースに接続
\c taskmanagement

-- スキーマの権限を付与
GRANT ALL ON SCHEMA public TO taskuser;
```

### ステップ3: スキーマの適用

`database/schema.sql` ファイルを実行して、テーブルを作成します。

```bash
# コマンドラインから実行
psql -U taskuser -d taskmanagement -f database/schema.sql

# または、psql内で実行
\c taskmanagement
\i database/schema.sql
```

### ステップ4: データベースの確認

テーブルが正しく作成されたか確認します。

```sql
-- 全てのテーブルを表示
\dt

-- 特定のテーブルの構造を確認
\d users
\d tasks
\d projects

-- サンプルデータを挿入して確認
INSERT INTO users (user_email, user_password_hash, user_full_name)
VALUES ('test@example.com', 'dummy_hash', 'テストユーザー');

SELECT * FROM users;
```

---

## バックエンドの実装

### ステップ1: プロジェクトのセットアップ

#### Bazelを使用する場合

```bash
# Bazelをインストール（macOS）
brew install bazelisk

# Bazelをインストール（Linux）
sudo apt install bazel

# 依存関係を取得
bazel fetch //backend/...

# ビルドして確認
bazel build //backend:taskmanagement_backend
```

**注意**: このプロジェクトは**Bazelでのビルドが必須**です。`build.sbt`ファイルは参考用として残していますが、実際のビルドには使用しません。

### ステップ2: 設定ファイルの作成

`backend/src/main/resources/application.conf` を作成します。

```hocon
# データベース設定
database {
  url = "jdbc:postgresql://localhost:5432/taskmanagement"
  user = "taskuser"
  password = "taskpass"
  driver = "org.postgresql.Driver"
  
  # コネクションプール設定
  connectionPool {
    maxConnections = 20
    minConnections = 5
  }
}

# サーバー設定
server {
  interface = "0.0.0.0"
  port = 8080
}

# JWT設定
jwt {
  secretKey = "your-secret-key-change-this-in-production"
  expirationSeconds = 86400  # 24時間
}
```

**重要**: `jwt.secretKey` は、本番環境では必ず変更してください。

### ステップ3: モデルの実装

`backend/src/main/scala/com/taskmanagement/models/Models.scala` を実装します。

**実装のポイント**:

1. **ケースクラスを使用**: イミュータブルなデータ構造
2. **わかりやすい変数名**: `userId`, `userEmail` など
3. **型を明示**: `Long`, `String`, `Timestamp` など

```scala
package com.taskmanagement.models

import java.sql.Timestamp

// ユーザーモデル
// データベースのusersテーブルに対応します
case class User(
  userId: Long,                    // ユーザーID（主キー）
  userEmail: String,               // メールアドレス（ログインID）
  userPasswordHash: String,        // BCryptでハッシュ化されたパスワード
  userFullName: String,            // ユーザーのフルネーム
  userCreatedAt: Timestamp,        // 作成日時
  userUpdatedAt: Timestamp         // 更新日時
)

// ユーザー登録リクエスト
// クライアントから送信されるJSON形式のリクエストに対応します
case class UserRegistrationRequest(
  registrationEmail: String,       // 登録するメールアドレス
  registrationPassword: String,    // 登録するパスワード（平文）
  registrationFullName: String     // 登録するフルネーム
)

// ログインリクエスト
case class UserLoginRequest(
  loginEmail: String,              // ログインメールアドレス
  loginPassword: String            // ログインパスワード（平文）
)

// ログインレスポンス
// クライアントに返すJSON形式のレスポンスに対応します
case class LoginResponse(
  loginToken: String,              // JWTトークン
  loginUser: UserResponse          // ユーザー情報
)

// ユーザーレスポンス
// パスワードハッシュを含まない、安全なユーザー情報
case class UserResponse(
  responseUserId: Long,
  responseUserEmail: String,
  responseUserFullName: String,
  responseUserCreatedAt: Timestamp
)

object UserResponse {
  // UserからUserResponseに変換するヘルパーメソッド
  def fromUser(user: User): UserResponse = {
    UserResponse(
      responseUserId = user.userId,
      responseUserEmail = user.userEmail,
      responseUserFullName = user.userFullName,
      responseUserCreatedAt = user.userCreatedAt
    )
  }
}

// タスクモデル
case class Task(
  taskId: Long,
  taskTitle: String,
  taskDescription: String,
  taskProjectId: Long,
  taskSectionId: Option[Long],
  taskAssignedToUserId: Option[Long],
  taskCreatedByUserId: Long,
  taskParentTaskId: Option[Long],
  taskStatus: String,
  taskPriority: String,
  taskDueDate: Option[java.sql.Date],
  taskCompletedAt: Option[Timestamp],
  taskCreatedAt: Timestamp,
  taskUpdatedAt: Timestamp
)

// タスク作成リクエスト
case class TaskCreationRequest(
  creationTitle: String,
  creationDescription: String,
  creationProjectId: Long,
  creationSectionId: Option[Long],
  creationAssignedToUserId: Option[Long],
  creationPriority: String,
  creationDueDate: Option[java.sql.Date]
)

// タスク更新リクエスト
case class TaskUpdateRequest(
  updateTitle: Option[String],
  updateDescription: Option[String],
  updateSectionId: Option[Long],
  updateAssignedToUserId: Option[Long],
  updateStatus: Option[String],
  updatePriority: Option[String],
  updateDueDate: Option[java.sql.Date]
)

// プロジェクトモデル
case class Project(
  projectId: Long,
  projectName: String,
  projectDescription: String,
  projectWorkspaceId: Long,
  projectColor: String,
  projectStatus: String,
  projectCreatedAt: Timestamp
)

// プロジェクト作成リクエスト
case class ProjectCreationRequest(
  creationName: String,
  creationDescription: String,
  creationWorkspaceId: Long,
  creationColor: String
)

// エラーレスポンス
case class ErrorResponse(
  errorMessage: String
)

// ページネーションレスポンス
case class PaginatedResponse[T](
  paginatedData: List[T],
  paginatedTotalCount: Long,
  paginatedCurrentPage: Int,
  paginatedPageSize: Int,
  paginatedTotalPages: Int
)
```

### ステップ4: データベーススキーマの実装

`backend/src/main/scala/com/taskmanagement/db/DatabaseSchema.scala` を実装します。

**実装のポイント**:

1. **Slickのテーブル定義**: PostgreSQLのテーブルに対応
2. **型安全なクエリ**: コンパイル時にエラーを検出
3. **外部キーの定義**: データの整合性を保証

```scala
package com.taskmanagement.db

import slick.jdbc.PostgresProfile.api._
import com.taskmanagement.models._
import java.sql.{Timestamp, Date}

// Usersテーブルの定義
// データベースのusersテーブルに対応するSlickテーブルクラス
class UsersTable(tag: Tag) extends Table[User](tag, "users") {
  // カラムの定義
  def userId = column[Long]("user_id", O.PrimaryKey, O.AutoInc)
  def userEmail = column[String]("user_email", O.Unique)
  def userPasswordHash = column[String]("user_password_hash")
  def userFullName = column[String]("user_full_name")
  def userCreatedAt = column[Timestamp]("user_created_at")
  def userUpdatedAt = column[Timestamp]("user_updated_at")
  
  // *プロジェクション: テーブルの行をUserケースクラスにマッピング
  def * = (userId, userEmail, userPasswordHash, userFullName, userCreatedAt, userUpdatedAt) <> 
    (User.tupled, User.unapply)
}

// Tasksテーブルの定義
class TasksTable(tag: Tag) extends Table[Task](tag, "tasks") {
  def taskId = column[Long]("task_id", O.PrimaryKey, O.AutoInc)
  def taskTitle = column[String]("task_title")
  def taskDescription = column[String]("task_description")
  def taskProjectId = column[Long]("task_project_id")
  def taskSectionId = column[Option[Long]]("task_section_id")
  def taskAssignedToUserId = column[Option[Long]]("task_assigned_to_user_id")
  def taskCreatedByUserId = column[Long]("task_created_by_user_id")
  def taskParentTaskId = column[Option[Long]]("task_parent_task_id")
  def taskStatus = column[String]("task_status")
  def taskPriority = column[String]("task_priority")
  def taskDueDate = column[Option[Date]]("task_due_date")
  def taskCompletedAt = column[Option[Timestamp]]("task_completed_at")
  def taskCreatedAt = column[Timestamp]("task_created_at")
  def taskUpdatedAt = column[Timestamp]("task_updated_at")
  
  def * = (taskId, taskTitle, taskDescription, taskProjectId, taskSectionId, 
           taskAssignedToUserId, taskCreatedByUserId, taskParentTaskId, taskStatus, 
           taskPriority, taskDueDate, taskCompletedAt, taskCreatedAt, taskUpdatedAt) <> 
    (Task.tupled, Task.unapply)
  
  // 外部キー制約
  def project = foreignKey("fk_task_project", taskProjectId, projectsTableQuery)(_.projectId)
  def assignedToUser = foreignKey("fk_task_assigned_user", taskAssignedToUserId, usersTableQuery)(_.userId.?)
  def createdByUser = foreignKey("fk_task_created_user", taskCreatedByUserId, usersTableQuery)(_.userId)
}

// Projectsテーブルの定義
class ProjectsTable(tag: Tag) extends Table[Project](tag, "projects") {
  def projectId = column[Long]("project_id", O.PrimaryKey, O.AutoInc)
  def projectName = column[String]("project_name")
  def projectDescription = column[String]("project_description")
  def projectWorkspaceId = column[Long]("project_workspace_id")
  def projectColor = column[String]("project_color")
  def projectStatus = column[String]("project_status")
  def projectCreatedAt = column[Timestamp]("project_created_at")
  
  def * = (projectId, projectName, projectDescription, projectWorkspaceId, 
           projectColor, projectStatus, projectCreatedAt) <> 
    (Project.tupled, Project.unapply)
}

// DatabaseSchemaオブジェクト
// 全てのテーブルクエリを提供します
object DatabaseSchema {
  // TableQueryは、テーブルに対するクエリを実行するためのオブジェクトです
  val usersTableQuery = TableQuery[UsersTable]
  val tasksTableQuery = TableQuery[TasksTable]
  val projectsTableQuery = TableQuery[ProjectsTable]
  
  // 他のテーブルも同様に定義...
}
```

### ステップ5: 認証機能の実装

#### 5-1: パスワードハッシュ化ユーティリティ

`backend/src/main/scala/com/taskmanagement/auth/PasswordHashingUtility.scala`

```scala
package com.taskmanagement.auth

import org.mindrot.jbcrypt.BCrypt

/**
 * パスワードハッシュ化ユーティリティ
 * 
 * BCryptを使用して、パスワードを安全にハッシュ化します。
 * BCryptは、ソルト（ランダムな文字列）を自動生成し、
 * レインボーテーブル攻撃に強いハッシュアルゴリズムです。
 */
object PasswordHashingUtility {
  
  /**
   * パスワードをハッシュ化します
   * 
   * @param plainTextPassword 平文のパスワード
   * @return BCryptでハッシュ化されたパスワード
   */
  def hashPassword(plainTextPassword: String): String = {
    // BCrypt.gensalt()でソルトを生成し、hashpwでハッシュ化
    // デフォルトのコストファクターは10（2^10回の計算）
    BCrypt.hashpw(plainTextPassword, BCrypt.gensalt())
  }
  
  /**
   * パスワードを検証します
   * 
   * @param plainTextPassword 平文のパスワード
   * @param hashedPassword ハッシュ化されたパスワード
   * @return パスワードが一致すればtrue、そうでなければfalse
   */
  def verifyPassword(plainTextPassword: String, hashedPassword: String): Boolean = {
    try {
      BCrypt.checkpw(plainTextPassword, hashedPassword)
    } catch {
      case _: Exception => false
    }
  }
}
```

#### 5-2: JWT認証ハンドラー

`backend/src/main/scala/com/taskmanagement/auth/JwtAuthenticationHandler.scala`

```scala
package com.taskmanagement.auth

import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import spray.json._
import java.time.Instant
import scala.util.{Try, Success, Failure}

/**
 * JWTペイロード
 * トークンに含まれる情報
 */
case class JwtPayload(
  userId: Long,
  userEmail: String,
  issuedAt: Long,
  expiresAt: Long
)

/**
 * JWT認証ハンドラー
 * 
 * JWTトークンの生成、検証、更新を行います。
 */
class JwtAuthenticationHandler(secretKey: String, expirationSeconds: Long) {
  
  // JWTアルゴリズム（HMAC SHA-256）
  private val jwtAlgorithm = JwtAlgorithm.HS256
  
  /**
   * ユーザーのためのJWTトークンを生成します
   * 
   * @param userId ユーザーID
   * @param userEmail ユーザーのメールアドレス
   * @return JWTトークン文字列
   */
  def generateTokenForUser(userId: Long, userEmail: String): String = {
    val currentTimestamp = Instant.now().getEpochSecond
    val expirationTimestamp = currentTimestamp + expirationSeconds
    
    // ペイロードを作成
    val payload = JwtPayload(
      userId = userId,
      userEmail = userEmail,
      issuedAt = currentTimestamp,
      expiresAt = expirationTimestamp
    )
    
    // ペイロードをJSON文字列に変換
    val payloadJson = s"""
      {
        "userId": ${payload.userId},
        "userEmail": "${payload.userEmail}",
        "iat": ${payload.issuedAt},
        "exp": ${payload.expiresAt}
      }
    """
    
    // JWTクレームを作成
    val claim = JwtClaim(payloadJson)
      .issuedAt(currentTimestamp)
      .expiresAt(expirationTimestamp)
    
    // トークンを生成
    Jwt.encode(claim, secretKey, jwtAlgorithm)
  }
  
  /**
   * JWTトークンを検証し、ペイロードを抽出します
   * 
   * @param tokenString JWTトークン文字列
   * @return 検証成功時はSome(JwtPayload)、失敗時はNone
   */
  def validateTokenAndExtractPayload(tokenString: String): Option[JwtPayload] = {
    Try {
      // トークンをデコードして検証
      Jwt.decode(tokenString, secretKey, Seq(jwtAlgorithm))
    } match {
      case Success(claim) =>
        // ペイロードをパース
        try {
          val json = claim.content.parseJson.asJsObject
          val userId = json.fields("userId").convertTo[Long]
          val userEmail = json.fields("userEmail").convertTo[String]
          val issuedAt = json.fields("iat").convertTo[Long]
          val expiresAt = json.fields("exp").convertTo[Long]
          
          Some(JwtPayload(userId, userEmail, issuedAt, expiresAt))
        } catch {
          case _: Exception => None
        }
      case Failure(_) =>
        None
    }
  }
  
  /**
   * Authorizationヘッダーからトークンを抽出します
   * 
   * @param authorizationHeaderValue Authorizationヘッダーの値
   * @return トークン文字列（"Bearer "プレフィックスを除く）
   */
  def extractTokenFromAuthorizationHeader(authorizationHeaderValue: String): Option[String] = {
    if (authorizationHeaderValue.startsWith("Bearer ")) {
      Some(authorizationHeaderValue.substring(7))
    } else {
      None
    }
  }
}
```

#### 5-3: 認証サービス

`backend/src/main/scala/com/taskmanagement/services/AuthenticationService.scala`

```scala
package com.taskmanagement.services

import com.taskmanagement.models._
import com.taskmanagement.db.DatabaseSchema._
import com.taskmanagement.auth.{JwtAuthenticationHandler, PasswordHashingUtility}
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{Future, ExecutionContext}
import java.sql.Timestamp
import java.time.LocalDateTime

/**
 * 認証サービス
 * 
 * ユーザーの登録、ログイン、ログアウトを管理します。
 */
class AuthenticationService(
  databaseInstance: Database,
  jwtAuthHandler: JwtAuthenticationHandler
)(implicit executionContext: ExecutionContext) {
  
  /**
   * 新規ユーザーを登録します
   * 
   * 処理の流れ:
   * 1. メールアドレスの重複チェック
   * 2. パスワードのバリデーション
   * 3. パスワードのハッシュ化
   * 4. データベースへのユーザー挿入
   * 5. JWTトークンの生成
   * 
   * @param registrationRequest ユーザー登録リクエスト
   * @return 成功時はRight(LoginResponse)、失敗時はLeft(エラーメッセージ)
   */
  def registerNewUser(
    registrationRequest: UserRegistrationRequest
  ): Future[Either[String, LoginResponse]] = {
    
    // バリデーション
    if (registrationRequest.registrationEmail.trim.isEmpty) {
      return Future.successful(Left("メールアドレスは必須です"))
    }
    
    if (registrationRequest.registrationPassword.length < 8) {
      return Future.successful(Left("パスワードは8文字以上で入力してください"))
    }
    
    if (registrationRequest.registrationFullName.trim.isEmpty) {
      return Future.successful(Left("名前は必須です"))
    }
    
    // メールアドレスの重複チェック
    val queryCheckDuplicateEmail = usersTableQuery
      .filter(_.userEmail === registrationRequest.registrationEmail)
      .result
      .headOption
    
    databaseInstance.run(queryCheckDuplicateEmail).flatMap {
      case Some(_) =>
        // 既に存在する
        Future.successful(Left("このメールアドレスは既に登録されています"))
      
      case None =>
        // パスワードをハッシュ化
        val hashedPassword = PasswordHashingUtility.hashPassword(
          registrationRequest.registrationPassword
        )
        
        // 新しいユーザーを作成
        val currentTimestamp = Timestamp.valueOf(LocalDateTime.now())
        val newUser = User(
          userId = 0,  // 自動採番されるため0
          userEmail = registrationRequest.registrationEmail,
          userPasswordHash = hashedPassword,
          userFullName = registrationRequest.registrationFullName,
          userCreatedAt = currentTimestamp,
          userUpdatedAt = currentTimestamp
        )
        
        // データベースに挿入
        val queryInsertUser = (usersTableQuery returning usersTableQuery.map(_.userId)
          into ((user, userId) => user.copy(userId = userId))) += newUser
        
        databaseInstance.run(queryInsertUser).map { createdUser =>
          // JWTトークンを生成
          val generatedToken = jwtAuthHandler.generateTokenForUser(
            createdUser.userId,
            createdUser.userEmail
          )
          
          // レスポンスを作成
          val loginResponse = LoginResponse(
            loginToken = generatedToken,
            loginUser = UserResponse.fromUser(createdUser)
          )
          
          Right(loginResponse)
        }.recover {
          case exception: Exception =>
            Left(s"ユーザー登録中にエラーが発生しました: ${exception.getMessage}")
        }
    }
  }
  
  /**
   * ユーザーを認証します（ログイン）
   * 
   * @param loginRequest ログインリクエスト
   * @return 成功時はRight(LoginResponse)、失敗時はLeft(エラーメッセージ)
   */
  def authenticateUser(
    loginRequest: UserLoginRequest
  ): Future[Either[String, LoginResponse]] = {
    
    // メールアドレスでユーザーを検索
    val queryFindUserByEmail = usersTableQuery
      .filter(_.userEmail === loginRequest.loginEmail)
      .result
      .headOption
    
    databaseInstance.run(queryFindUserByEmail).map {
      case None =>
        // ユーザーが見つからない
        Left("メールアドレスまたはパスワードが正しくありません")
      
      case Some(foundUser) =>
        // パスワードを検証
        val isPasswordValid = PasswordHashingUtility.verifyPassword(
          loginRequest.loginPassword,
          foundUser.userPasswordHash
        )
        
        if (isPasswordValid) {
          // 認証成功
          val generatedToken = jwtAuthHandler.generateTokenForUser(
            foundUser.userId,
            foundUser.userEmail
          )
          
          val loginResponse = LoginResponse(
            loginToken = generatedToken,
            loginUser = UserResponse.fromUser(foundUser)
          )
          
          Right(loginResponse)
        } else {
          // パスワードが間違っている
          Left("メールアドレスまたはパスワードが正しくありません")
        }
    }.recover {
      case exception: Exception =>
        Left(s"ログイン中にエラーが発生しました: ${exception.getMessage}")
    }
  }
  
  /**
   * トークンからユーザー情報を取得します
   * 
   * @param jwtToken JWTトークン
   * @return ユーザー情報（Option）
   */
  def getUserFromToken(jwtToken: String): Future[Option[UserResponse]] = {
    jwtAuthHandler.validateTokenAndExtractPayload(jwtToken) match {
      case None =>
        Future.successful(None)
      
      case Some(payload) =>
        val queryFindUserById = usersTableQuery
          .filter(_.userId === payload.userId)
          .result
          .headOption
        
        databaseInstance.run(queryFindUserById).map {
          case Some(foundUser) => Some(UserResponse.fromUser(foundUser))
          case None => None
        }
    }
  }
}
```

### ステップ6: APIルートの実装

`backend/src/main/scala/com/taskmanagement/api/Routes.scala`

```scala
package com.taskmanagement.api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.StatusCodes
import com.taskmanagement.services._
import com.taskmanagement.models._
import com.taskmanagement.auth.JwtAuthenticationHandler
import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directive1

/**
 * APIルート定義
 * 
 * 全てのHTTPエンドポイントを定義します。
 */
object Routes {
  
  def createRoutes(
    authenticationService: AuthenticationService,
    taskManagementService: TaskManagementService,
    projectManagementService: ProjectManagementService,
    jwtAuthHandler: JwtAuthenticationHandler
  )(implicit executionContext: ExecutionContext): Route = {
    
    // JSON形式のリクエスト/レスポンスを処理するためのプロトコル
    import JsonProtocols._
    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
    
    // 認証ディレクティブ
    // Authorizationヘッダーからトークンを抽出し、検証してユーザーIDを返す
    def authenticateUser: Directive1[Long] = {
      optionalHeaderValueByName("Authorization").flatMap {
        case Some(authorizationHeader) =>
          jwtAuthHandler.extractTokenFromAuthorizationHeader(authorizationHeader) match {
            case Some(token) =>
              jwtAuthHandler.validateTokenAndExtractPayload(token) match {
                case Some(payload) =>
                  provide(payload.userId)
                case None =>
                  complete(StatusCodes.Unauthorized, ErrorResponse("無効なトークンです"))
              }
            case None =>
              complete(StatusCodes.Unauthorized, ErrorResponse("トークンが見つかりません"))
          }
        case None =>
          complete(StatusCodes.Unauthorized, ErrorResponse("認証が必要です"))
      }
    }
    
    // ルート定義
    pathPrefix("api") {
      concat(
        // 認証API
        pathPrefix("auth") {
          concat(
            // POST /api/auth/register
            path("register") {
              post {
                entity(as[UserRegistrationRequest]) { registrationRequest =>
                  onSuccess(authenticationService.registerNewUser(registrationRequest)) {
                    case Right(loginResponse) =>
                      complete(StatusCodes.Created, loginResponse)
                    case Left(errorMessage) =>
                      complete(StatusCodes.BadRequest, ErrorResponse(errorMessage))
                  }
                }
              }
            },
            // POST /api/auth/login
            path("login") {
              post {
                entity(as[UserLoginRequest]) { loginRequest =>
                  onSuccess(authenticationService.authenticateUser(loginRequest)) {
                    case Right(loginResponse) =>
                      complete(StatusCodes.OK, loginResponse)
                    case Left(errorMessage) =>
                      complete(StatusCodes.Unauthorized, ErrorResponse(errorMessage))
                  }
                }
              }
            },
            // GET /api/auth/me
            path("me") {
              get {
                authenticateUser { authenticatedUserId =>
                  headerValueByName("Authorization") { authorizationHeader =>
                    val token = jwtAuthHandler.extractTokenFromAuthorizationHeader(authorizationHeader).get
                    onSuccess(authenticationService.getUserFromToken(token)) {
                      case Some(userResponse) =>
                        complete(StatusCodes.OK, userResponse)
                      case None =>
                        complete(StatusCodes.NotFound, ErrorResponse("ユーザーが見つかりません"))
                    }
                  }
                }
              }
            }
          )
        },
        // タスクAPI
        pathPrefix("tasks") {
          concat(
            // POST /api/tasks
            pathEnd {
              post {
                authenticateUser { authenticatedUserId =>
                  entity(as[TaskCreationRequest]) { taskCreationRequest =>
                    onSuccess(taskManagementService.createNewTask(taskCreationRequest, authenticatedUserId)) {
                      case Right(createdTask) =>
                        complete(StatusCodes.Created, createdTask)
                      case Left(errorMessage) =>
                        complete(StatusCodes.BadRequest, ErrorResponse(errorMessage))
                    }
                  }
                }
              }
            },
            // GET /api/tasks/my
            path("my") {
              get {
                authenticateUser { authenticatedUserId =>
                  onSuccess(taskManagementService.getTasksAssignedToUser(authenticatedUserId)) { tasksList =>
                    complete(StatusCodes.OK, tasksList)
                  }
                }
              }
            }
          )
        }
      )
    }
  }
}
```

### ステップ7: メインアプリケーションの実装

`backend/src/main/scala/com/taskmanagement/Main.scala`

```scala
package com.taskmanagement

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.taskmanagement.api.Routes
import com.taskmanagement.services._
import com.taskmanagement.auth.JwtAuthenticationHandler
import slick.jdbc.PostgresProfile.api._
import com.typesafe.config.ConfigFactory
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}

/**
 * メインアプリケーション
 * 
 * アプリケーションのエントリーポイントです。
 * HTTPサーバーを起動し、全てのコンポーネントを初期化します。
 */
object Main {
  
  def main(args: Array[String]): Unit = {
    // Akka ActorSystemを作成
    implicit val actorSystem: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "taskmanagement-system")
    implicit val executionContext: ExecutionContext = actorSystem.executionContext
    
    // 設定ファイルを読み込み
    val config = ConfigFactory.load()
    
    // データベース接続を作成
    val databaseInstance = Database.forConfig("database", config)
    
    // JWT認証ハンドラーを作成
    val jwtSecretKey = config.getString("jwt.secretKey")
    val jwtExpirationSeconds = config.getLong("jwt.expirationSeconds")
    val jwtAuthHandler = new JwtAuthenticationHandler(jwtSecretKey, jwtExpirationSeconds)
    
    // サービスを作成
    val authenticationService = new AuthenticationService(databaseInstance, jwtAuthHandler)
    val taskManagementService = new TaskManagementService(databaseInstance)
    val projectManagementService = new ProjectManagementService(databaseInstance)
    
    // ルートを作成
    val routes: Route = Routes.createRoutes(
      authenticationService,
      taskManagementService,
      projectManagementService,
      jwtAuthHandler
    )
    
    // HTTPサーバーを起動
    val serverInterface = config.getString("server.interface")
    val serverPort = config.getInt("server.port")
    
    val bindingFuture: Future[Http.ServerBinding] = Http()
      .newServerAt(serverInterface, serverPort)
      .bind(routes)
    
    bindingFuture.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        println(s"サーバーが起動しました: http://${address.getHostString}:${address.getPort}/")
        println("終了するには、Ctrl+Cを押してください")
      
      case Failure(exception) =>
        println(s"サーバーの起動に失敗しました: ${exception.getMessage}")
        actorSystem.terminate()
    }
    
    // シャットダウンフック
    sys.addShutdownHook {
      println("\nサーバーをシャットダウンしています...")
      bindingFuture
        .flatMap(_.unbind())
        .onComplete { _ =>
          databaseInstance.close()
          actorSystem.terminate()
        }
    }
  }
}
```

### ステップ8: バックエンドの起動とテスト

```bash
# Bazelでビルドして起動
bazel run //backend:taskmanagement_backend
```

**curlでテスト**:

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
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <トークン>"
```

---

## Webフロントエンドの実装

### ステップ1: プロジェクトのセットアップ

```bash
# Bazelで開発サーバーを起動
bazel run //web:dev_server

# または、npmで起動
cd web
npm install
npm run dev
```

### ステップ2: APIクライアントの実装

`web/src/api/client.ts`

**実装のポイント**:

1. **axiosインスタンス**: ベースURLと共通ヘッダーを設定
2. **リクエストインターセプター**: JWTトークンを自動付与
3. **レスポンスインターセプター**: 401エラー時に自動ログアウト

```typescript
import axios, { AxiosInstance } from 'axios'

// ユーザー型定義
interface User {
  responseUserId: number
  responseUserEmail: string
  responseUserFullName: string
  responseUserCreatedAt: string
}

// ログインレスポンス型定義
interface LoginResponse {
  loginToken: string
  loginUser: User
}

// タスク型定義
interface Task {
  taskId: number
  taskTitle: string
  taskDescription: string
  taskProjectId: number
  taskSectionId: number | null
  taskAssignedToUserId: number | null
  taskCreatedByUserId: number
  taskParentTaskId: number | null
  taskStatus: string
  taskPriority: string
  taskDueDate: string | null
  taskCompletedAt: string | null
  taskCreatedAt: string
  taskUpdatedAt: string
}

/**
 * APIクライアント
 * 
 * バックエンドAPIとの通信を担当します。
 */
class ApiClient {
  private axiosInstance: AxiosInstance

  constructor() {
    // axiosインスタンスを作成
    this.axiosInstance = axios.create({
      baseURL: 'http://localhost:8080',
      headers: {
        'Content-Type': 'application/json',
      },
    })

    // リクエストインターセプター: JWTトークンを自動付与
    this.axiosInstance.interceptors.request.use((config) => {
      const storedToken = localStorage.getItem('jwtToken')
      if (storedToken) {
        config.headers.Authorization = `Bearer ${storedToken}`
      }
      return config
    })

    // レスポンスインターセプター: 401エラー時にログアウト
    this.axiosInstance.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401) {
          // トークンが無効な場合、ログアウト
          localStorage.removeItem('jwtToken')
          window.location.href = '/login'
        }
        return Promise.reject(error)
      }
    )
  }

  /**
   * ユーザー登録
   */
  async register(email: string, password: string, fullName: string): Promise<LoginResponse> {
    const response = await this.axiosInstance.post('/api/auth/register', {
      registrationEmail: email,
      registrationPassword: password,
      registrationFullName: fullName,
    })
    return response.data
  }

  /**
   * ログイン
   */
  async login(email: string, password: string): Promise<LoginResponse> {
    const response = await this.axiosInstance.post('/api/auth/login', {
      loginEmail: email,
      loginPassword: password,
    })
    return response.data
  }

  /**
   * 現在のユーザー情報を取得
   */
  async getCurrentUser(): Promise<User> {
    const response = await this.axiosInstance.get('/api/auth/me')
    return response.data
  }

  /**
   * 自分のタスク一覧を取得
   */
  async getMyTasks(): Promise<Task[]> {
    const response = await this.axiosInstance.get('/api/tasks/my')
    return response.data
  }

  /**
   * タスクを作成
   */
  async createTask(taskData: {
    creationTitle: string
    creationDescription: string
    creationProjectId: number
    creationSectionId?: number
    creationAssignedToUserId?: number
    creationPriority: string
    creationDueDate?: string
  }): Promise<Task> {
    const response = await this.axiosInstance.post('/api/tasks', taskData)
    return response.data
  }
}

// シングルトンインスタンスをエクスポート
export const apiClient = new ApiClient()
export type { User, LoginResponse, Task }
```

### ステップ3: ログインページの実装

`web/src/pages/LoginPage.tsx`

```typescript
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiClient } from '../api/client'

function LoginPage() {
  // 状態管理
  const [userEmailInput, setUserEmailInput] = useState('')
  const [userPasswordInput, setUserPasswordInput] = useState('')
  const [isLoadingLogin, setIsLoadingLogin] = useState(false)
  const [loginErrorMessage, setLoginErrorMessage] = useState('')
  
  const navigate = useNavigate()

  // ログインボタンクリック時の処理
  const handleLoginButtonClick = async (e: React.FormEvent) => {
    e.preventDefault()
    
    // エラーメッセージをクリア
    setLoginErrorMessage('')
    setIsLoadingLogin(true)

    try {
      // APIを呼び出してログイン
      const loginResponse = await apiClient.login(userEmailInput, userPasswordInput)
      
      // トークンをlocalStorageに保存
      localStorage.setItem('jwtToken', loginResponse.loginToken)
      
      // ダッシュボードにリダイレクト
      navigate('/dashboard')
      
    } catch (error: any) {
      // エラーハンドリング
      if (error.response?.status === 401) {
        setLoginErrorMessage('メールアドレスまたはパスワードが正しくありません')
      } else {
        setLoginErrorMessage('ログインに失敗しました。もう一度お試しください')
      }
    } finally {
      setIsLoadingLogin(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <div className="bg-white p-8 rounded-lg shadow-md w-96">
        <h1 className="text-2xl font-bold mb-6 text-center">ログイン</h1>
        
        <form onSubmit={handleLoginButtonClick}>
          {/* メールアドレス入力 */}
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2">
              メールアドレス
            </label>
            <input
              type="email"
              value={userEmailInput}
              onChange={(e) => setUserEmailInput(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="user@example.com"
              required
            />
          </div>

          {/* パスワード入力 */}
          <div className="mb-6">
            <label className="block text-gray-700 text-sm font-bold mb-2">
              パスワード
            </label>
            <input
              type="password"
              value={userPasswordInput}
              onChange={(e) => setUserPasswordInput(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="パスワード"
              required
            />
          </div>

          {/* エラーメッセージ */}
          {loginErrorMessage && (
            <div className="mb-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded">
              {loginErrorMessage}
            </div>
          )}

          {/* ログインボタン */}
          <button
            type="submit"
            disabled={isLoadingLogin}
            className="w-full bg-blue-500 text-white py-2 px-4 rounded-md hover:bg-blue-600 disabled:bg-gray-400 disabled:cursor-not-allowed"
          >
            {isLoadingLogin ? 'ログイン中...' : 'ログイン'}
          </button>
        </form>

        {/* 登録リンク */}
        <p className="mt-4 text-center text-sm text-gray-600">
          アカウントをお持ちでない方は
          <a href="/register" className="text-blue-500 hover:underline ml-1">
            新規登録
          </a>
        </p>
      </div>
    </div>
  )
}

export default LoginPage
```

### ステップ4: ダッシュボードページの実装

`web/src/pages/DashboardPage.tsx`

```typescript
import { useState, useEffect } from 'react'
import { apiClient, Task } from '../api/client'

function DashboardPage() {
  const [tasksList, setTasksList] = useState<Task[]>([])
  const [isLoadingTasks, setIsLoadingTasks] = useState(true)

  // コンポーネントのマウント時にタスクを取得
  useEffect(() => {
    const fetchTasksFromApi = async () => {
      try {
        const retrievedTasks = await apiClient.getMyTasks()
        setTasksList(retrievedTasks)
      } catch (error) {
        console.error('タスクの取得に失敗しました', error)
      } finally {
        setIsLoadingTasks(false)
      }
    }

    fetchTasksFromApi()
  }, [])

  // 優先度に応じた色を返す
  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'urgent': return 'bg-red-100 text-red-800'
      case 'high': return 'bg-orange-100 text-orange-800'
      case 'medium': return 'bg-yellow-100 text-yellow-800'
      case 'low': return 'bg-green-100 text-green-800'
      default: return 'bg-gray-100 text-gray-800'
    }
  }

  // ステータスに応じた表示名を返す
  const getStatusDisplayName = (status: string) => {
    switch (status) {
      case 'todo': return '未着手'
      case 'in_progress': return '進行中'
      case 'completed': return '完了'
      default: return status
    }
  }

  if (isLoadingTasks) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-xl">タスクを読み込み中...</div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-100 p-8">
      <div className="max-w-6xl mx-auto">
        <h1 className="text-3xl font-bold mb-8">ダッシュボード</h1>

        {/* タスク一覧 */}
        <div className="bg-white rounded-lg shadow-md p-6">
          <h2 className="text-xl font-semibold mb-4">自分のタスク</h2>

          {tasksList.length === 0 ? (
            <p className="text-gray-500">タスクがありません</p>
          ) : (
            <div className="space-y-4">
              {tasksList.map((task) => (
                <div
                  key={task.taskId}
                  className="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow"
                >
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <h3 className="text-lg font-semibold mb-2">
                        {task.taskTitle}
                      </h3>
                      <p className="text-gray-600 mb-3">
                        {task.taskDescription}
                      </p>
                      <div className="flex items-center space-x-3">
                        <span className={`px-2 py-1 rounded text-sm ${getPriorityColor(task.taskPriority)}`}>
                          {task.taskPriority}
                        </span>
                        <span className="px-2 py-1 rounded text-sm bg-blue-100 text-blue-800">
                          {getStatusDisplayName(task.taskStatus)}
                        </span>
                        {task.taskDueDate && (
                          <span className="text-sm text-gray-500">
                            期限: {task.taskDueDate}
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export default DashboardPage
```

### ステップ5: ルーティングの設定

`web/src/App.tsx`

```typescript
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useState, useEffect } from 'react'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import DashboardPage from './pages/DashboardPage'
import { apiClient, User } from './api/client'

function App() {
  const [currentAuthenticatedUser, setCurrentAuthenticatedUser] = useState<User | null>(null)
  const [isAuthenticationLoading, setIsAuthenticationLoading] = useState(true)

  // アプリ起動時に現在のユーザーを取得
  useEffect(() => {
    const loadCurrentUserFromApi = async () => {
      const storedToken = localStorage.getItem('jwtToken')
      
      if (!storedToken) {
        setIsAuthenticationLoading(false)
        return
      }

      try {
        const retrievedUser = await apiClient.getCurrentUser()
        setCurrentAuthenticatedUser(retrievedUser)
      } catch (error) {
        console.error('ユーザー情報の取得に失敗しました', error)
        localStorage.removeItem('jwtToken')
      } finally {
        setIsAuthenticationLoading(false)
      }
    }

    loadCurrentUserFromApi()
  }, [])

  if (isAuthenticationLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-xl">読み込み中...</div>
      </div>
    )
  }

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route
          path="/dashboard"
          element={
            currentAuthenticatedUser ? (
              <DashboardPage />
            ) : (
              <Navigate to="/login" />
            )
          }
        />
        <Route path="/" element={<Navigate to="/dashboard" />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
```

---

## モバイルアプリの実装

モバイルアプリは、Webフロントエンドと同様の構造で実装します。

### ステップ1: Expoプロジェクトのセットアップ

```bash
# Bazelで開発サーバーを起動
bazel run //mobile:start

# または、npmで起動
cd mobile
npm install
npm start
```

### ステップ2: APIクライアントの共有

Webフロントエンドで作成したAPIクライアントを、モバイルアプリでも使用できます。
ただし、`localStorage` の代わりに `AsyncStorage` を使用します。

---

## テストの実装

### ステップ1: ユニットテストの作成

`backend/src/test/scala/com/taskmanagement/services/AuthenticationServiceTest.scala`

```scala
package com.taskmanagement.services

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.concurrent.Await
import scala.concurrent.duration._

class AuthenticationServiceTest extends AnyFlatSpec with Matchers {
  
  "registerNewUser" should "正常にユーザーを登録できること" in {
    // テストコード
  }
  
  it should "重複したメールアドレスの場合はエラーを返すこと" in {
    // テストコード
  }
}
```

### ステップ2: テストの実行

```bash
# Bazelでテストを実行
bazel test //backend:authentication_service_test
bazel test //backend/...  # 全てのテスト
```

---

## デプロイ

### ステップ1: Dockerイメージのビルド

```bash
# バックエンドのDockerイメージをビルド
bazel build //backend:taskmanagement_backend_image

# Webフロントエンドをビルド
bazel build //web:build_production
```

### ステップ2: デプロイ

**AWS ECSへのデプロイ**、**Google Cloud Runへのデプロイ**、**Kubernetesへのデプロイ**など、
環境に応じたデプロイ手順を実行します。

---

## トラブルシューティング

### データベース接続エラー

**エラー**: `Connection refused`

**解決方法**:
1. PostgreSQLが起動しているか確認: `pg_isready`
2. 接続情報が正しいか確認: `application.conf`
3. ファイアウォールの設定を確認

### ビルドエラー

**エラー**: `bazel: command not found`

**解決方法**:
```bash
# Bazelをインストール
brew install bazelisk  # macOS
sudo apt install bazel  # Linux
```

### ポート競合エラー

**エラー**: `Address already in use`

**解決方法**:
```bash
# ポート8080を使用しているプロセスを確認
lsof -i :8080

# プロセスを終了
kill -9 <PID>
```

---

## まとめ

このガイドに従って実装することで、Asana風タスク管理アプリケーションを一から構築できます。

**実装の順序**:
1. データベース → 2. バックエンド → 3. フロントエンド → 4. テスト → 5. デプロイ

**重要なポイント**:
- わかりやすい変数名を使用する
- 型安全性を活用する
- エラーハンドリングを適切に行う
- テストを書く

このガイドを参考に、一つずつ丁寧に実装してください。
