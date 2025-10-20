# アーキテクチャドキュメント

このドキュメントでは、Asana風タスク管理アプリケーションの全体的なアーキテクチャと設計思想を詳細に解説します。

## 目次

1. [システム全体像](#システム全体像)
2. [モノレポ構造](#モノレポ構造)
3. [バックエンドアーキテクチャ](#バックエンドアーキテクチャ)
4. [フロントエンドアーキテクチャ](#フロントエンドアーキテクチャ)
5. [データベース設計](#データベース設計)
6. [認証・認可システム](#認証認可システム)
7. [API設計](#api設計)
8. [ビルドシステム](#ビルドシステム)
9. [デプロイメント戦略](#デプロイメント戦略)
10. [スケーラビリティ](#スケーラビリティ)

---

## システム全体像

このアプリケーションは、3層アーキテクチャ（プレゼンテーション層、ビジネスロジック層、データアクセス層）を採用したモノレポ構成のフルスタックアプリケーションです。

```
┌─────────────────────────────────────────────────────────────┐
│                        クライアント層                          │
├──────────────────────┬──────────────────────────────────────┤
│   Webブラウザ         │   モバイルアプリ                       │
│   (React + Vite)     │   (React Native + Expo)              │
└──────────────────────┴──────────────────────────────────────┘
                            ↓ HTTPS/REST API
┌─────────────────────────────────────────────────────────────┐
│                      アプリケーション層                        │
├─────────────────────────────────────────────────────────────┤
│   Akka HTTP Server (Scala)                                  │
│   ├─ API Routes (ルーティング)                               │
│   ├─ Authentication Service (認証サービス)                    │
│   ├─ Task Management Service (タスク管理サービス)             │
│   └─ Project Management Service (プロジェクト管理サービス)     │
└─────────────────────────────────────────────────────────────┘
                            ↓ Slick ORM
┌─────────────────────────────────────────────────────────────┐
│                       データベース層                           │
├─────────────────────────────────────────────────────────────┤
│   PostgreSQL 15+                                            │
│   ├─ 17テーブル（users, tasks, projects, etc.）              │
│   ├─ インデックス（検索最適化）                                │
│   └─ トリガー（自動更新）                                      │
└─────────────────────────────────────────────────────────────┘
```

### 技術スタック

| 層 | 技術 | 理由 |
|---|------|------|
| **バックエンド** | Scala 2.13 + Akka HTTP | 型安全性、並行処理、関数型プログラミング |
| **データベース** | PostgreSQL 15+ | ACID準拠、複雑なクエリ対応、信頼性 |
| **ORM** | Slick 3.5 | 型安全なクエリ、非同期処理、Scala統合 |
| **認証** | JWT + BCrypt | ステートレス認証、セキュアなパスワード保存 |
| **Webフロントエンド** | React 18 + TypeScript + Vite | コンポーネントベース、型安全、高速ビルド |
| **モバイル** | React Native + Expo | クロスプラットフォーム、コード共有、迅速な開発 |
| **ビルドシステム** | Bazel | モノレポ対応、増分ビルド、再現性 |

---

## モノレポ構造

このプロジェクトは、モノレポ（単一リポジトリ）構成を採用しています。全てのコンポーネント（バックエンド、Web、モバイル、データベース、ドキュメント）が1つのリポジトリで管理されます。

### ディレクトリ構造

```
portfolio1/
├── WORKSPACE                    # Bazelのルート設定ファイル
├── .bazelrc                     # Bazelの設定オプション
├── README.md                    # プロジェクト概要
│
├── backend/                     # Scalaバックエンド
│   ├── BUILD                    # Bazelビルド定義
│   ├── build.sbt                # SBT設定（参考用）
│   └── src/
│       ├── main/
│       │   ├── scala/
│       │   │   └── com/taskmanagement/
│       │   │       ├── Main.scala                    # エントリーポイント
│       │   │       ├── auth/                         # 認証関連
│       │   │       │   ├── JwtAuthenticationHandler.scala
│       │   │       │   └── PasswordHashingUtility.scala
│       │   │       ├── db/                           # データベース
│       │   │       │   └── DatabaseSchema.scala
│       │   │       ├── models/                       # データモデル
│       │   │       │   └── Models.scala
│       │   │       ├── services/                     # ビジネスロジック
│       │   │       │   ├── AuthenticationService.scala
│       │   │       │   ├── TaskManagementService.scala
│       │   │       │   └── ProjectManagementService.scala
│       │   │       └── api/                          # API層
│       │   │           ├── Routes.scala
│       │   │           └── JsonProtocols.scala
│       │   └── resources/
│       │       └── application.conf                  # 設定ファイル
│       └── test/                                     # テストコード
│
├── web/                         # React Webフロントエンド
│   ├── BUILD                    # Bazelビルド定義
│   ├── package.json             # npm依存関係
│   ├── vite.config.ts           # Vite設定
│   ├── tsconfig.json            # TypeScript設定
│   ├── tailwind.config.js       # Tailwind CSS設定
│   └── src/
│       ├── main.tsx             # エントリーポイント
│       ├── App.tsx              # ルートコンポーネント
│       ├── api/
│       │   └── client.ts        # APIクライアント
│       └── pages/               # ページコンポーネント
│           ├── LoginPage.tsx
│           ├── RegisterPage.tsx
│           ├── DashboardPage.tsx
│           ├── ProjectsPage.tsx
│           └── TasksPage.tsx
│
├── mobile/                      # React Nativeモバイルアプリ
│   ├── BUILD                    # Bazelビルド定義
│   ├── package.json             # npm依存関係
│   ├── app.json                 # Expo設定
│   └── src/                     # ソースコード（将来実装）
│
├── database/                    # データベーススキーマ
│   └── schema.sql               # PostgreSQLスキーマ定義
│
└── docs/                        # ドキュメント
    ├── ASANA_PHILOSOPHY.md      # Asanaの設計思想
    ├── DEVELOPMENT.md           # 開発ガイド
    ├── ARCHITECTURE.md          # このファイル
    ├── CODING_GUIDELINES.md     # コーディング規約
    └── IMPLEMENTATION_GUIDE.md  # 実装ガイド
```

### モノレポのメリット

1. **コード共有**: 型定義やユーティリティ関数を複数のプロジェクト間で共有できます
2. **一貫性**: 全てのコンポーネントが同じバージョンの依存関係を使用します
3. **アトミックな変更**: バックエンドとフロントエンドの変更を1つのコミットで行えます
4. **簡単なリファクタリング**: 全体的な変更を一度に適用できます
5. **統一されたビルドシステム**: Bazelで全てのコンポーネントを統一的にビルドできます

---

## バックエンドアーキテクチャ

バックエンドは、レイヤードアーキテクチャ（層状アーキテクチャ）を採用しています。各層は明確な責任を持ち、上位層は下位層にのみ依存します。

### レイヤー構成

```
┌─────────────────────────────────────────────────────────────┐
│                        API層 (Routes)                        │
│  役割: HTTPリクエストの受付、レスポンスの返却                   │
│  技術: Akka HTTP、Spray JSON                                 │
│  ファイル: api/Routes.scala, api/JsonProtocols.scala         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    サービス層 (Services)                      │
│  役割: ビジネスロジックの実装、トランザクション管理              │
│  技術: Scala Future、Either型（エラーハンドリング）            │
│  ファイル: services/AuthenticationService.scala など          │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                  データアクセス層 (Database)                   │
│  役割: データベースとのやり取り、クエリ実行                      │
│  技術: Slick、PostgreSQL JDBC Driver                         │
│  ファイル: db/DatabaseSchema.scala                           │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    モデル層 (Models)                          │
│  役割: データ構造の定義、型安全性の提供                          │
│  技術: Scalaケースクラス                                       │
│  ファイル: models/Models.scala                               │
└─────────────────────────────────────────────────────────────┘
```

### 各層の詳細

#### 1. API層 (Routes.scala)

**責任**: HTTPリクエストを受け取り、適切なサービスメソッドを呼び出し、レスポンスを返す

**主要コンポーネント**:
- `Routes` オブジェクト: 全てのAPIエンドポイントを定義
- `authenticateUser` ディレクティブ: JWTトークンを検証し、ユーザーIDを抽出
- JSONシリアライズ/デシリアライズ: `JsonProtocols` を使用

**エンドポイント設計**:
```scala
// 認証API
POST   /api/auth/register     // ユーザー登録
POST   /api/auth/login        // ログイン
POST   /api/auth/logout       // ログアウト
GET    /api/auth/me           // 現在のユーザー情報取得

// プロジェクトAPI
POST   /api/projects          // プロジェクト作成
GET    /api/projects/my       // 自分のプロジェクト一覧
GET    /api/projects/workspace/{id}  // ワークスペース内のプロジェクト一覧
POST   /api/projects/{id}/members    // プロジェクトメンバー追加

// タスクAPI
POST   /api/tasks             // タスク作成
GET    /api/tasks/my          // 自分のタスク一覧
GET    /api/tasks/project/{id}  // プロジェクト内のタスク一覧
PUT    /api/tasks/{id}        // タスク更新
DELETE /api/tasks/{id}        // タスク削除
POST   /api/tasks/{id}/complete  // タスク完了
POST   /api/tasks/search      // タスク検索
```

#### 2. サービス層 (Services)

**責任**: ビジネスロジックの実装、データの検証、トランザクション管理

**主要サービス**:

##### AuthenticationService
- `registerNewUser`: 新規ユーザー登録（パスワードハッシュ化、重複チェック）
- `authenticateUser`: ログイン認証（パスワード検証、JWTトークン生成）
- `logoutUser`: ログアウト（セッション無効化）
- `getUserFromToken`: トークンからユーザー情報取得
- `isSessionValid`: セッションの有効性確認

##### TaskManagementService
- `createNewTask`: タスク作成（権限チェック、通知送信）
- `updateExistingTask`: タスク更新（変更履歴記録）
- `completeTask`: タスク完了（依存タスクチェック）
- `deleteTask`: タスク削除（関連データのクリーンアップ）
- `searchTasks`: タスク検索（複雑な条件対応）
- `getAllTasksInProject`: プロジェクト内の全タスク取得
- `getTasksAssignedToUser`: ユーザーに割り当てられたタスク取得

##### ProjectManagementService
- `createNewProject`: プロジェクト作成（ワークスペース権限チェック）
- `addMemberToProject`: メンバー追加（役割設定）
- `getAllProjectsInWorkspace`: ワークスペース内のプロジェクト一覧
- `getProjectsForUser`: ユーザーが参加しているプロジェクト一覧

**エラーハンドリング**:
全てのサービスメソッドは `Future[Either[String, T]]` を返します。
- `Left(errorMessage)`: エラーが発生した場合
- `Right(result)`: 成功した場合

```scala
// 使用例
taskManagementService.createNewTask(request, userId).map {
  case Right(createdTask) => 
    // 成功: タスクが作成された
    complete(StatusCodes.Created, createdTask)
  case Left(errorMessage) => 
    // 失敗: エラーメッセージを返す
    complete(StatusCodes.BadRequest, ErrorResponse(errorMessage))
}
```

#### 3. データアクセス層 (DatabaseSchema.scala)

**責任**: データベーステーブルの定義、クエリの実行

**Slickテーブル定義**:
各PostgreSQLテーブルに対応するSlickテーブルクラスを定義します。

```scala
// 例: Usersテーブル
class UsersTable(tag: Tag) extends Table[User](tag, "users") {
  def userId = column[Long]("user_id", O.PrimaryKey, O.AutoInc)
  def userEmail = column[String]("user_email", O.Unique)
  def userPasswordHash = column[String]("user_password_hash")
  def userFullName = column[String]("user_full_name")
  def userCreatedAt = column[Timestamp]("user_created_at")
  def userUpdatedAt = column[Timestamp]("user_updated_at")
  
  def * = (userId, userEmail, userPasswordHash, userFullName, 
           userCreatedAt, userUpdatedAt) <> (User.tupled, User.unapply)
}
```

**クエリ実行パターン**:
```scala
// 単一レコード取得
val queryFindUserByEmail = usersTableQuery
  .filter(_.userEmail === emailAddress)
  .result
  .headOption

databaseInstance.run(queryFindUserByEmail)

// 複数レコード取得
val queryGetAllTasksInProject = tasksTableQuery
  .filter(_.taskProjectId === projectId)
  .result

databaseInstance.run(queryGetAllTasksInProject)

// 挿入
val queryInsertNewUser = usersTableQuery returning usersTableQuery.map(_.userId) += newUser
databaseInstance.run(queryInsertNewUser)

// 更新
val queryUpdateTask = tasksTableQuery
  .filter(_.taskId === taskId)
  .update(updatedTask)

databaseInstance.run(queryUpdateTask)
```

#### 4. モデル層 (Models.scala)

**責任**: データ構造の定義、型安全性の提供

**ケースクラス設計**:
全てのデータモデルはイミュータブル（不変）なケースクラスとして定義されます。

```scala
// ドメインモデル
case class User(
  userId: Long,
  userEmail: String,
  userPasswordHash: String,
  userFullName: String,
  userCreatedAt: Timestamp,
  userUpdatedAt: Timestamp
)

// リクエストDTO（Data Transfer Object）
case class UserRegistrationRequest(
  registrationEmail: String,
  registrationPassword: String,
  registrationFullName: String
)

// レスポンスDTO
case class UserResponse(
  responseUserId: Long,
  responseUserEmail: String,
  responseUserFullName: String,
  responseUserCreatedAt: Timestamp
)
```

**命名規則**:
- ドメインモデル: `user_id` → `userId`（データベースのカラム名に対応）
- リクエストDTO: `registrationEmail`（用途を明確に）
- レスポンスDTO: `responseUserId`（レスポンス用であることを明示）

### 並行処理とスレッドモデル

Akka HTTPは、非同期・ノンブロッキングなアーキテクチャを採用しています。

```scala
// 全ての処理はFutureで非同期実行される
implicit val executionContext: ExecutionContext = system.dispatcher

// リクエストハンドラ
path("api" / "tasks" / "my") {
  get {
    authenticateUser { authenticatedUserId =>
      // この処理は非同期で実行される
      val futureTasksList = taskManagementService.getTasksAssignedToUser(authenticatedUserId)
      
      onComplete(futureTasksList) {
        case Success(tasksList) => complete(tasksList)
        case Failure(exception) => complete(StatusCodes.InternalServerError)
      }
    }
  }
}
```

**スレッドプール設定**:
```conf
akka {
  actor {
    default-dispatcher {
      type = Dispatcher
      executor = "fork-join-executor"
      fork-join-executor {
        parallelism-min = 8
        parallelism-factor = 3.0
        parallelism-max = 64
      }
    }
  }
}
```

---

## フロントエンドアーキテクチャ

フロントエンドは、コンポーネントベースのアーキテクチャを採用しています。

### Web（React + TypeScript）

#### ディレクトリ構造

```
web/src/
├── main.tsx              # エントリーポイント
├── App.tsx               # ルートコンポーネント（ルーティング定義）
├── index.css             # グローバルスタイル
├── api/
│   └── client.ts         # APIクライアント（axios）
├── pages/                # ページコンポーネント
│   ├── LoginPage.tsx
│   ├── RegisterPage.tsx
│   ├── DashboardPage.tsx
│   ├── ProjectsPage.tsx
│   └── TasksPage.tsx
├── components/           # 再利用可能なコンポーネント（将来実装）
│   ├── TaskCard.tsx
│   ├── ProjectCard.tsx
│   └── Header.tsx
└── hooks/                # カスタムフック（将来実装）
    ├── useAuth.ts
    └── useTasks.ts
```

#### 状態管理

現在は、Reactの組み込み状態管理（useState、useEffect）を使用しています。

```typescript
// App.tsx - グローバル認証状態
const [currentAuthenticatedUser, setCurrentAuthenticatedUser] = useState<User | null>(null);
const [isAuthenticationLoading, setIsAuthenticationLoading] = useState(true);

// ページコンポーネント - ローカル状態
const [tasksList, setTasksList] = useState<Task[]>([]);
const [isLoadingTasks, setIsLoadingTasks] = useState(true);
```

**将来の拡張**: プロジェクトが大規模化した場合、以下の状態管理ライブラリの導入を検討できます。
- Redux Toolkit: グローバル状態管理
- React Query: サーバー状態管理（キャッシュ、再取得）
- Zustand: 軽量な状態管理

#### ルーティング

React Routerを使用したクライアントサイドルーティング。

```typescript
<BrowserRouter>
  <Routes>
    <Route path="/login" element={<LoginPage />} />
    <Route path="/register" element={<RegisterPage />} />
    <Route path="/dashboard" element={
      currentAuthenticatedUser ? <DashboardPage /> : <Navigate to="/login" />
    } />
    <Route path="/projects" element={
      currentAuthenticatedUser ? <ProjectsPage /> : <Navigate to="/login" />
    } />
    <Route path="/tasks" element={
      currentAuthenticatedUser ? <TasksPage /> : <Navigate to="/login" />
    } />
  </Routes>
</BrowserRouter>
```

#### APIクライアント

axiosを使用したHTTPクライアント。JWTトークンの自動付与とエラーハンドリングを実装。

```typescript
class ApiClient {
  private axiosInstance: AxiosInstance;

  constructor() {
    this.axiosInstance = axios.create({
      baseURL: 'http://localhost:8080',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // リクエストインターセプター: JWTトークンを自動付与
    this.axiosInstance.interceptors.request.use((config) => {
      const storedToken = localStorage.getItem('jwtToken');
      if (storedToken) {
        config.headers.Authorization = `Bearer ${storedToken}`;
      }
      return config;
    });

    // レスポンスインターセプター: 401エラー時にログアウト
    this.axiosInstance.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401) {
          localStorage.removeItem('jwtToken');
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    );
  }

  async login(email: string, password: string): Promise<LoginResponse> {
    const response = await this.axiosInstance.post('/api/auth/login', {
      loginEmail: email,
      loginPassword: password,
    });
    return response.data;
  }

  // 他のメソッド...
}
```

### Mobile（React Native + Expo）

モバイルアプリは、Webと同じAPIクライアントを共有できます。

**将来の実装計画**:
- ナビゲーション: React Navigation
- 状態管理: Webと同じアプローチ
- ストレージ: AsyncStorage（JWTトークン保存）
- オフライン対応: Redux Persist + SQLite

---

## データベース設計

PostgreSQLを使用した正規化されたリレーショナルデータベース設計。

### ER図（Entity-Relationship Diagram）

```
┌─────────────┐
│   users     │
└──────┬──────┘
       │
       ├─────────────────────────────────────────┐
       │                                         │
       ↓                                         ↓
┌──────────────────┐                    ┌─────────────────┐
│ organizations    │                    │ user_sessions   │
└────────┬─────────┘                    └─────────────────┘
         │
         ↓
┌──────────────────────┐
│ organization_members │
└──────────────────────┘
         │
         ↓
┌─────────────┐
│ workspaces  │
└──────┬──────┘
       │
       ↓
┌────────────────────┐
│ workspace_members  │
└────────────────────┘
       │
       ↓
┌─────────────┐
│  projects   │
└──────┬──────┘
       │
       ├──────────────────────┐
       │                      │
       ↓                      ↓
┌──────────────┐      ┌─────────────────┐
│   sections   │      │ project_members │
└──────┬───────┘      └─────────────────┘
       │
       ↓
┌─────────────┐
│    tasks    │◄──────┐
└──────┬──────┘       │
       │              │
       ├──────────────┼──────────────────────┐
       │              │                      │
       ↓              ↓                      ↓
┌──────────────┐ ┌──────────────────┐ ┌─────────────┐
│   comments   │ │ task_dependencies│ │  task_tags  │
└──────────────┘ └──────────────────┘ └──────┬──────┘
       │                                      │
       ↓                                      ↓
┌──────────────┐                      ┌─────────────┐
│ attachments  │                      │    tags     │
└──────────────┘                      └─────────────┘

       ↓
┌──────────────────┐
│  notifications   │
└──────────────────┘

       ↓
┌──────────────────┐
│  activity_logs   │
└──────────────────┘
```

### テーブル設計の詳細

#### 1. users（ユーザー）

**目的**: システムを使用する全てのユーザーの情報を保存

| カラム名 | 型 | 制約 | 説明 |
|---------|---|------|------|
| user_id | BIGSERIAL | PRIMARY KEY | ユーザーID（自動採番） |
| user_email | VARCHAR(255) | UNIQUE, NOT NULL | メールアドレス（ログインID） |
| user_password_hash | VARCHAR(255) | NOT NULL | BCryptでハッシュ化されたパスワード |
| user_full_name | VARCHAR(255) | NOT NULL | ユーザーのフルネーム |
| user_created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| user_updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新日時 |

**インデックス**:
- `idx_users_email`: メールアドレスでの高速検索

#### 2. organizations（組織）

**目的**: 企業や団体などの組織を表現

| カラム名 | 型 | 制約 | 説明 |
|---------|---|------|------|
| organization_id | BIGSERIAL | PRIMARY KEY | 組織ID |
| organization_name | VARCHAR(255) | NOT NULL | 組織名 |
| organization_owner_user_id | BIGINT | FOREIGN KEY → users | 組織のオーナー |
| organization_created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 作成日時 |

#### 3. workspaces（ワークスペース）

**目的**: 組織内のプロジェクトをグループ化する単位

| カラム名 | 型 | 制約 | 説明 |
|---------|---|------|------|
| workspace_id | BIGSERIAL | PRIMARY KEY | ワークスペースID |
| workspace_name | VARCHAR(255) | NOT NULL | ワークスペース名 |
| workspace_organization_id | BIGINT | FOREIGN KEY → organizations | 所属組織 |
| workspace_created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 作成日時 |

**Asanaとの対応**: Asanaの「チーム」に相当

#### 4. projects（プロジェクト）

**目的**: タスクをまとめる単位

| カラム名 | 型 | 制約 | 説明 |
|---------|---|------|------|
| project_id | BIGSERIAL | PRIMARY KEY | プロジェクトID |
| project_name | VARCHAR(255) | NOT NULL | プロジェクト名 |
| project_description | TEXT | | プロジェクトの説明 |
| project_workspace_id | BIGINT | FOREIGN KEY → workspaces | 所属ワークスペース |
| project_color | VARCHAR(7) | | プロジェクトの色（#RRGGBB） |
| project_status | VARCHAR(50) | DEFAULT 'active' | ステータス（active/archived） |
| project_created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 作成日時 |

**インデックス**:
- `idx_projects_workspace`: ワークスペースIDでの高速検索

#### 5. sections（セクション）

**目的**: プロジェクト内でタスクをグループ化する単位

| カラム名 | 型 | 制約 | 説明 |
|---------|---|------|------|
| section_id | BIGSERIAL | PRIMARY KEY | セクションID |
| section_name | VARCHAR(255) | NOT NULL | セクション名 |
| section_project_id | BIGINT | FOREIGN KEY → projects | 所属プロジェクト |
| section_display_order | INTEGER | DEFAULT 0 | 表示順序 |
| section_created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 作成日時 |

**Asanaとの対応**: Asanaの「セクション」に相当（例: "To Do", "In Progress", "Done"）

#### 6. tasks（タスク）

**目的**: 実際の作業単位を表現

| カラム名 | 型 | 制約 | 説明 |
|---------|---|------|------|
| task_id | BIGSERIAL | PRIMARY KEY | タスクID |
| task_title | VARCHAR(500) | NOT NULL | タスクのタイトル |
| task_description | TEXT | | タスクの詳細説明 |
| task_project_id | BIGINT | FOREIGN KEY → projects | 所属プロジェクト |
| task_section_id | BIGINT | FOREIGN KEY → sections | 所属セクション |
| task_assigned_to_user_id | BIGINT | FOREIGN KEY → users | 担当者 |
| task_created_by_user_id | BIGINT | FOREIGN KEY → users | 作成者 |
| task_parent_task_id | BIGINT | FOREIGN KEY → tasks | 親タスク（サブタスク用） |
| task_status | VARCHAR(50) | DEFAULT 'todo' | ステータス（todo/in_progress/completed） |
| task_priority | VARCHAR(50) | DEFAULT 'medium' | 優先度（low/medium/high/urgent） |
| task_due_date | DATE | | 期限日 |
| task_completed_at | TIMESTAMP | | 完了日時 |
| task_created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| task_updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新日時 |

**インデックス**:
- `idx_tasks_project`: プロジェクトIDでの高速検索
- `idx_tasks_assigned_to`: 担当者IDでの高速検索
- `idx_tasks_status`: ステータスでの高速検索
- `idx_tasks_due_date`: 期限日でのソート

#### 7. task_dependencies（タスク依存関係）

**目的**: タスク間の依存関係を表現（「タスクAが完了しないとタスクBを開始できない」）

| カラム名 | 型 | 制約 | 説明 |
|---------|---|------|------|
| dependency_id | BIGSERIAL | PRIMARY KEY | 依存関係ID |
| dependency_task_id | BIGINT | FOREIGN KEY → tasks | 依存するタスク |
| dependency_depends_on_task_id | BIGINT | FOREIGN KEY → tasks | 依存先タスク |
| dependency_created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 作成日時 |

**制約**: `UNIQUE(dependency_task_id, dependency_depends_on_task_id)` - 同じ依存関係の重複を防ぐ

#### 8. tags（タグ）

**目的**: タスクを横断的に分類するためのラベル

| カラム名 | 型 | 制約 | 説明 |
|---------|---|------|------|
| tag_id | BIGSERIAL | PRIMARY KEY | タグID |
| tag_name | VARCHAR(100) | NOT NULL | タグ名 |
| tag_color | VARCHAR(7) | | タグの色（#RRGGBB） |
| tag_workspace_id | BIGINT | FOREIGN KEY → workspaces | 所属ワークスペース |
| tag_created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 作成日時 |

#### 9. task_tags（タスク-タグ関連）

**目的**: タスクとタグの多対多関係を表現

| カラム名 | 型 | 制約 | 説明 |
|---------|---|------|------|
| task_tag_id | BIGSERIAL | PRIMARY KEY | 関連ID |
| task_tag_task_id | BIGINT | FOREIGN KEY → tasks | タスクID |
| task_tag_tag_id | BIGINT | FOREIGN KEY → tags | タグID |
| task_tag_created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 作成日時 |

**制約**: `UNIQUE(task_tag_task_id, task_tag_tag_id)` - 同じタグの重複付与を防ぐ

#### 10. comments（コメント）

**目的**: タスクに対するコメントやディスカッション

| カラム名 | 型 | 制約 | 説明 |
|---------|---|------|------|
| comment_id | BIGSERIAL | PRIMARY KEY | コメントID |
| comment_task_id | BIGINT | FOREIGN KEY → tasks | 対象タスク |
| comment_author_user_id | BIGINT | FOREIGN KEY → users | コメント作成者 |
| comment_content | TEXT | NOT NULL | コメント内容 |
| comment_created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| comment_updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新日時 |

#### 11. attachments（添付ファイル）

**目的**: タスクに添付されたファイルの情報

| カラム名 | 型 | 制約 | 説明 |
|---------|---|------|------|
| attachment_id | BIGSERIAL | PRIMARY KEY | 添付ファイルID |
| attachment_task_id | BIGINT | FOREIGN KEY → tasks | 対象タスク |
| attachment_uploaded_by_user_id | BIGINT | FOREIGN KEY → users | アップロード者 |
| attachment_file_name | VARCHAR(255) | NOT NULL | ファイル名 |
| attachment_file_url | TEXT | NOT NULL | ファイルのURL |
| attachment_file_size | BIGINT | | ファイルサイズ（バイト） |
| attachment_mime_type | VARCHAR(100) | | MIMEタイプ |
| attachment_created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 作成日時 |

#### 12. notifications（通知）

**目的**: ユーザーへの通知を管理

| カラム名 | 型 | 制約 | 説明 |
|---------|---|------|------|
| notification_id | BIGSERIAL | PRIMARY KEY | 通知ID |
| notification_user_id | BIGINT | FOREIGN KEY → users | 通知先ユーザー |
| notification_type | VARCHAR(50) | NOT NULL | 通知タイプ |
| notification_title | VARCHAR(255) | NOT NULL | 通知タイトル |
| notification_message | TEXT | NOT NULL | 通知メッセージ |
| notification_related_task_id | BIGINT | FOREIGN KEY → tasks | 関連タスク |
| notification_is_read | BOOLEAN | DEFAULT FALSE | 既読フラグ |
| notification_created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 作成日時 |

**通知タイプ**:
- `task_assigned`: タスクが割り当てられた
- `task_completed`: タスクが完了した
- `task_commented`: タスクにコメントがついた
- `task_due_soon`: タスクの期限が近い

#### 13. activity_logs（アクティビティログ）

**目的**: 全ての変更履歴を記録（監査証跡）

| カラム名 | 型 | 制約 | 説明 |
|---------|---|------|------|
| activity_log_id | BIGSERIAL | PRIMARY KEY | ログID |
| activity_log_user_id | BIGINT | FOREIGN KEY → users | 操作者 |
| activity_log_action_type | VARCHAR(50) | NOT NULL | アクションタイプ |
| activity_log_entity_type | VARCHAR(50) | NOT NULL | エンティティタイプ |
| activity_log_entity_id | BIGINT | NOT NULL | エンティティID |
| activity_log_details | JSONB | | 詳細情報（JSON） |
| activity_log_created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 作成日時 |

**アクションタイプ**:
- `created`: 作成
- `updated`: 更新
- `deleted`: 削除
- `completed`: 完了

### ビューとトリガー

#### ビュー: active_tasks

アクティブなタスクのみを表示するビュー。

```sql
CREATE VIEW active_tasks AS
SELECT * FROM tasks
WHERE task_status != 'completed'
  AND task_status != 'archived';
```

#### トリガー: update_updated_at_timestamp

`updated_at` カラムを自動更新するトリガー。

```sql
CREATE OR REPLACE FUNCTION update_timestamp_function()
RETURNS TRIGGER AS $$
BEGIN
  NEW.user_updated_at = CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_users_timestamp
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION update_timestamp_function();
```

---

## 認証・認可システム

JWT（JSON Web Token）ベースのステートレス認証を採用。

### 認証フロー

```
1. ユーザー登録
   ┌──────────┐                    ┌──────────┐
   │ Client   │                    │ Backend  │
   └────┬─────┘                    └────┬─────┘
        │                               │
        │ POST /api/auth/register       │
        │ { email, password, name }     │
        ├──────────────────────────────>│
        │                               │
        │                               │ 1. メール重複チェック
        │                               │ 2. パスワードをBCryptでハッシュ化
        │                               │ 3. DBにユーザー挿入
        │                               │ 4. JWTトークン生成
        │                               │
        │ { token, user }               │
        │<──────────────────────────────┤
        │                               │
        │ トークンをlocalStorageに保存   │
        │                               │

2. ログイン
   ┌──────────┐                    ┌──────────┐
   │ Client   │                    │ Backend  │
   └────┬─────┘                    └────┬─────┘
        │                               │
        │ POST /api/auth/login          │
        │ { email, password }           │
        ├──────────────────────────────>│
        │                               │
        │                               │ 1. メールでユーザー検索
        │                               │ 2. BCryptでパスワード検証
        │                               │ 3. JWTトークン生成
        │                               │ 4. セッションをDBに保存
        │                               │
        │ { token, user }               │
        │<──────────────────────────────┤
        │                               │
        │ トークンをlocalStorageに保存   │
        │                               │

3. 認証が必要なAPIリクエスト
   ┌──────────┐                    ┌──────────┐
   │ Client   │                    │ Backend  │
   └────┬─────┘                    └────┬─────┘
        │                               │
        │ GET /api/tasks/my             │
        │ Authorization: Bearer <token> │
        ├──────────────────────────────>│
        │                               │
        │                               │ 1. トークンを抽出
        │                               │ 2. トークンを検証（署名、有効期限）
        │                               │ 3. ユーザーIDを抽出
        │                               │ 4. セッションの有効性確認
        │                               │ 5. タスク取得
        │                               │
        │ [ tasks ]                     │
        │<──────────────────────────────┤
        │                               │
```

### JWTトークンの構造

JWTトークンは、3つの部分から構成されます。

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEyMzQ1LCJ1c2VyRW1haWwiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNzAwMDAwMDAwLCJleHAiOjE3MDAwODY0MDB9.signature
└────────────────────────────────┘ └──────────────────────────────────────────────────────────────────────────────────────────────┘ └────────┘
         Header                                                    Payload                                                          Signature
```

**Header（ヘッダー）**:
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**Payload（ペイロード）**:
```json
{
  "userId": 12345,
  "userEmail": "user@example.com",
  "iat": 1700000000,  // 発行日時（Issued At）
  "exp": 1700086400   // 有効期限（Expiration）
}
```

**Signature（署名）**:
```
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret_key
)
```

### パスワードハッシュ化

BCryptを使用してパスワードを安全にハッシュ化します。

```scala
object PasswordHashingUtility {
  // パスワードをハッシュ化
  def hashPassword(plainTextPassword: String): String = {
    BCrypt.hashpw(plainTextPassword, BCrypt.gensalt())
  }

  // パスワードを検証
  def verifyPassword(plainTextPassword: String, hashedPassword: String): Boolean = {
    BCrypt.checkpw(plainTextPassword, hashedPassword)
  }
}
```

**BCryptの特徴**:
- ソルト（ランダムな文字列）を自動生成
- 計算コストを調整可能（デフォルト: 10ラウンド）
- レインボーテーブル攻撃に強い
- 同じパスワードでも毎回異なるハッシュ値が生成される

### セッション管理

JWTトークンに加えて、データベースにセッション情報を保存します。

```scala
case class UserSession(
  sessionId: Long,
  sessionUserId: Long,
  sessionToken: String,
  sessionExpiresAt: Timestamp,
  sessionCreatedAt: Timestamp
)
```

**セッションの有効性確認**:
1. JWTトークンの署名を検証
2. JWTトークンの有効期限を確認
3. データベースのセッションテーブルを確認
4. セッションが無効化されていないか確認

### 認可（Authorization）

認証（Authentication）とは別に、認可（Authorization）も実装します。

**権限レベル**:
- `owner`: 組織のオーナー（全ての権限）
- `admin`: 管理者（メンバー管理、プロジェクト作成）
- `member`: メンバー（タスクの作成・編集）
- `viewer`: 閲覧者（読み取りのみ）

**権限チェックの例**:
```scala
def canUserEditTask(userId: Long, taskId: Long): Future[Boolean] = {
  // 1. タスクを取得
  // 2. タスクのプロジェクトを取得
  // 3. ユーザーがプロジェクトメンバーか確認
  // 4. メンバーの役割を確認
  // 5. 役割に応じて編集権限を判定
}
```

---

## API設計

RESTful APIの設計原則に従ったAPI設計。

### RESTful原則

1. **リソース指向**: URLはリソース（名詞）を表現
   - 良い例: `/api/tasks`, `/api/projects`
   - 悪い例: `/api/getTasks`, `/api/createProject`

2. **HTTPメソッドの適切な使用**:
   - `GET`: リソースの取得（冪等性あり）
   - `POST`: リソースの作成
   - `PUT`: リソースの完全更新
   - `PATCH`: リソースの部分更新
   - `DELETE`: リソースの削除（冪等性あり）

3. **ステータスコードの適切な使用**:
   - `200 OK`: 成功
   - `201 Created`: リソース作成成功
   - `400 Bad Request`: リクエストが不正
   - `401 Unauthorized`: 認証が必要
   - `403 Forbidden`: 権限がない
   - `404 Not Found`: リソースが見つからない
   - `500 Internal Server Error`: サーバーエラー

### APIエンドポイント一覧

#### 認証API

| メソッド | エンドポイント | 説明 | 認証 |
|---------|---------------|------|------|
| POST | `/api/auth/register` | ユーザー登録 | 不要 |
| POST | `/api/auth/login` | ログイン | 不要 |
| POST | `/api/auth/logout` | ログアウト | 必要 |
| GET | `/api/auth/me` | 現在のユーザー情報取得 | 必要 |

#### プロジェクトAPI

| メソッド | エンドポイント | 説明 | 認証 |
|---------|---------------|------|------|
| POST | `/api/projects` | プロジェクト作成 | 必要 |
| GET | `/api/projects/my` | 自分のプロジェクト一覧 | 必要 |
| GET | `/api/projects/workspace/{id}` | ワークスペース内のプロジェクト一覧 | 必要 |
| GET | `/api/projects/{id}` | プロジェクト詳細取得 | 必要 |
| PUT | `/api/projects/{id}` | プロジェクト更新 | 必要 |
| DELETE | `/api/projects/{id}` | プロジェクト削除 | 必要 |
| POST | `/api/projects/{id}/members` | メンバー追加 | 必要 |

#### タスクAPI

| メソッド | エンドポイント | 説明 | 認証 |
|---------|---------------|------|------|
| POST | `/api/tasks` | タスク作成 | 必要 |
| GET | `/api/tasks/my` | 自分のタスク一覧 | 必要 |
| GET | `/api/tasks/project/{id}` | プロジェクト内のタスク一覧 | 必要 |
| GET | `/api/tasks/{id}` | タスク詳細取得 | 必要 |
| PUT | `/api/tasks/{id}` | タスク更新 | 必要 |
| DELETE | `/api/tasks/{id}` | タスク削除 | 必要 |
| POST | `/api/tasks/{id}/complete` | タスク完了 | 必要 |
| POST | `/api/tasks/search` | タスク検索 | 必要 |

### リクエスト/レスポンス例

#### POST /api/auth/register

**リクエスト**:
```json
{
  "registrationEmail": "user@example.com",
  "registrationPassword": "SecurePassword123!",
  "registrationFullName": "山田太郎"
}
```

**レスポンス（成功）**:
```json
{
  "loginToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "loginUser": {
    "responseUserId": 12345,
    "responseUserEmail": "user@example.com",
    "responseUserFullName": "山田太郎",
    "responseUserCreatedAt": "2024-01-15T10:30:00Z"
  }
}
```

**レスポンス（エラー）**:
```json
{
  "errorMessage": "このメールアドレスは既に登録されています"
}
```

#### POST /api/tasks

**リクエスト**:
```json
{
  "creationTitle": "新しいタスク",
  "creationDescription": "タスクの詳細説明",
  "creationProjectId": 100,
  "creationSectionId": 10,
  "creationAssignedToUserId": 12345,
  "creationPriority": "high",
  "creationDueDate": "2024-12-31"
}
```

**レスポンス（成功）**:
```json
{
  "taskId": 5678,
  "taskTitle": "新しいタスク",
  "taskDescription": "タスクの詳細説明",
  "taskProjectId": 100,
  "taskSectionId": 10,
  "taskAssignedToUserId": 12345,
  "taskCreatedByUserId": 12345,
  "taskParentTaskId": null,
  "taskStatus": "todo",
  "taskPriority": "high",
  "taskDueDate": "2024-12-31",
  "taskCompletedAt": null,
  "taskCreatedAt": "2024-01-15T10:30:00Z",
  "taskUpdatedAt": "2024-01-15T10:30:00Z"
}
```

### ページネーション

大量のデータを返すAPIには、ページネーションを実装します。

**リクエストパラメータ**:
- `page`: ページ番号（1から開始）
- `pageSize`: 1ページあたりの件数（デフォルト: 20）

**レスポンス**:
```json
{
  "paginatedData": [ /* データの配列 */ ],
  "paginatedTotalCount": 150,
  "paginatedCurrentPage": 1,
  "paginatedPageSize": 20,
  "paginatedTotalPages": 8
}
```

### エラーハンドリング

全てのエラーレスポンスは、統一されたフォーマットを使用します。

```json
{
  "errorMessage": "エラーの説明",
  "errorCode": "ERROR_CODE",
  "errorDetails": {
    "field": "問題のあるフィールド",
    "reason": "詳細な理由"
  }
}
```

---

## ビルドシステム

Bazelを使用したモノレポビルドシステム。

### Bazelの特徴

1. **増分ビルド**: 変更されたファイルのみを再ビルド
2. **並列ビルド**: 複数のターゲットを並列にビルド
3. **再現性**: 同じ入力から常に同じ出力を生成
4. **キャッシュ**: ビルド結果をキャッシュして高速化
5. **クロスプラットフォーム**: Linux、macOS、Windowsで動作

### ビルドコマンド

#### バックエンド

```bash
# ビルド
bazel build //backend:taskmanagement_backend

# 実行
bazel run //backend:taskmanagement_backend

# テスト
bazel test //backend:authentication_service_test
bazel test //backend/...  # 全てのテスト

# Dockerイメージビルド
bazel build //backend:taskmanagement_backend_image
```

#### Webフロントエンド

```bash
# 開発サーバー起動
bazel run //web:dev_server

# プロダクションビルド
bazel build //web:build_production

# 型チェック
bazel run //web:typecheck

# Lint
bazel run //web:lint
```

#### モバイル

```bash
# Expo開発サーバー起動
bazel run //mobile:start

# Android
bazel run //mobile:android

# iOS（macOSのみ）
bazel run //mobile:ios
```

### ビルド設定（.bazelrc）

`.bazelrc` ファイルで、ビルドの動作をカスタマイズできます。

```bash
# デバッグビルド
bazel build --config=debug //backend:taskmanagement_backend

# リリースビルド（最適化有効）
bazel build --config=release //backend:taskmanagement_backend

# 開発用高速ビルド
bazel build --config=dev //backend:taskmanagement_backend
```

---

## デプロイメント戦略

### 開発環境

**ローカル開発**:
- バックエンド: `bazel run //backend:taskmanagement_backend`
- Web: `bazel run //web:dev_server`
- データベース: Docker Compose

```yaml
# docker-compose.yml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: taskmanagement
      POSTGRES_USER: taskuser
      POSTGRES_PASSWORD: taskpass
    ports:
      - "5432:5432"
    volumes:
      - ./database/schema.sql:/docker-entrypoint-initdb.d/schema.sql
```

### ステージング環境

**Dockerコンテナ化**:
```bash
# バックエンドイメージビルド
bazel build //backend:taskmanagement_backend_image

# イメージをDockerにロード
bazel run //backend:taskmanagement_backend_image

# コンテナ起動
docker run -p 8080:8080 \
  -e DATABASE_URL=postgresql://... \
  -e JWT_SECRET=... \
  bazel/backend:taskmanagement_backend_image
```

### プロダクション環境

**推奨デプロイ先**:
- バックエンド: AWS ECS、Google Cloud Run、Kubernetes
- フロントエンド: Vercel、Netlify、AWS S3 + CloudFront
- データベース: AWS RDS、Google Cloud SQL

**CI/CDパイプライン**:
```yaml
# .github/workflows/deploy.yml
name: Deploy
on:
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run tests
        run: bazel test //...

  build:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Build backend
        run: bazel build //backend:taskmanagement_backend_image
      - name: Build web
        run: bazel build //web:build_production

  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to production
        run: |
          # デプロイスクリプト
```

---

## スケーラビリティ

### 水平スケーリング

**バックエンド**:
- ステートレス設計（JWTトークン使用）
- 複数のインスタンスを起動可能
- ロードバランサーで負荷分散

**データベース**:
- リードレプリカの追加（読み取り専用）
- 接続プーリング（HikariCP）
- インデックスの最適化

### キャッシング戦略

**将来の実装**:
- Redis: セッション情報、頻繁にアクセスされるデータ
- CDN: 静的ファイル（画像、CSS、JS）
- ブラウザキャッシュ: APIレスポンスのキャッシュ

### パフォーマンス最適化

**データベース**:
- インデックスの適切な使用
- N+1問題の回避（Slickのjoinを使用）
- クエリの最適化

**バックエンド**:
- 非同期処理（Scala Future）
- 並列処理（Akka Streams）
- コネクションプーリング

**フロントエンド**:
- コード分割（React.lazy）
- 画像の最適化（WebP、遅延読み込み）
- バンドルサイズの削減

---

## まとめ

このアーキテクチャは、以下の原則に基づいて設計されています。

1. **分離**: 各層が明確な責任を持つ
2. **型安全性**: ScalaとTypeScriptで型エラーを防ぐ
3. **スケーラビリティ**: 水平スケーリングが可能
4. **保守性**: コードが理解しやすく、変更しやすい
5. **セキュリティ**: JWT認証、BCryptハッシュ化、SQLインジェクション対策

このドキュメントを参考に、アプリケーションの全体像を理解し、後で清書する際の指針としてください。
