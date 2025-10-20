# コーディングガイドライン

このドキュメントでは、Asana風タスク管理アプリケーションのコーディング規約とベストプラクティスを詳細に解説します。

## 目次

1. [基本原則](#基本原則)
2. [命名規則](#命名規則)
3. [Scalaコーディング規約](#scalaコーディング規約)
4. [TypeScript/Reactコーディング規約](#typescriptreactコーディング規約)
5. [データベース規約](#データベース規約)
6. [コメント規約](#コメント規約)
7. [エラーハンドリング](#エラーハンドリング)
8. [テスト規約](#テスト規約)
9. [Git規約](#git規約)
10. [コードレビューチェックリスト](#コードレビューチェックリスト)

---

## 基本原則

### 1. わかりやすさを最優先

このプロジェクトの最も重要な原則は、**後で読む人（あなた自身を含む）がコードを理解しやすいこと**です。

**良い例**:
```scala
val retrievedUserFromDatabase = databaseInstance.run(
  usersTableQuery.filter(_.userEmail === providedEmailAddress).result.headOption
)
```

**悪い例**:
```scala
val u = db.run(users.filter(_.email === e).result.headOption)
```

### 2. 一般的な名前を避ける

以下のような一般的すぎる変数名は避けてください。

**避けるべき名前**:
- `data`, `result`, `temp`, `tmp`, `value`, `item`
- `x`, `y`, `z`, `a`, `b`, `c`
- `foo`, `bar`, `baz`
- `thing`, `stuff`, `obj`

**代わりに使うべき名前**:
- `retrievedUserData`, `calculatedTotalAmount`, `temporaryTaskList`
- `taskListFromDatabase`, `filteredProjectsByStatus`
- `authenticatedUserInformation`, `validatedInputData`

### 3. 型安全性を活用

ScalaとTypeScriptの型システムを最大限に活用し、コンパイル時にエラーを検出します。

```scala
// 良い例: 型を明示
def calculateTaskPriority(taskDueDate: LocalDate, taskCreatedDate: LocalDate): String = {
  // ...
}

// 悪い例: 型を省略しすぎ
def calc(d1, d2) = {
  // ...
}
```

### 4. イミュータブル（不変）を優先

可能な限り、変更不可能なデータ構造を使用します。

```scala
// 良い例: val（不変）
val taskTitle = "新しいタスク"
val taskList = List(task1, task2, task3)

// 悪い例: var（可変）
var taskTitle = "新しいタスク"
var taskList = ListBuffer(task1, task2, task3)
```

---

## 命名規則

### Scala命名規則

#### 1. クラス名・トレイト名

**PascalCase**（各単語の先頭を大文字）を使用します。

```scala
// 良い例
class AuthenticationService
class TaskManagementService
trait DatabaseConnection
case class UserRegistrationRequest
```

#### 2. メソッド名・変数名

**camelCase**（最初の単語は小文字、以降の単語の先頭を大文字）を使用します。

```scala
// 良い例
def registerNewUser(registrationRequest: UserRegistrationRequest): Future[Either[String, User]]
val authenticatedUserId: Long = 12345
val retrievedTaskFromDatabase: Task = ...

// 悪い例
def register_user(req: UserRegistrationRequest): Future[Either[String, User]]
val user_id: Long = 12345
val task: Task = ...
```

#### 3. 定数

**UPPER_SNAKE_CASE**（全て大文字、単語をアンダースコアで区切る）を使用します。

```scala
// 良い例
val JWT_SECRET_KEY = "your-secret-key"
val DEFAULT_PAGE_SIZE = 20
val MAX_LOGIN_ATTEMPTS = 5
```

#### 4. パッケージ名

**小文字のみ**を使用します。

```scala
// 良い例
package com.taskmanagement.services
package com.taskmanagement.auth
```

### TypeScript/React命名規則

#### 1. コンポーネント名

**PascalCase**を使用します。

```typescript
// 良い例
function LoginPage() { ... }
function TaskCard() { ... }
const DashboardPage: React.FC = () => { ... }
```

#### 2. 変数名・関数名

**camelCase**を使用します。

```typescript
// 良い例
const currentAuthenticatedUser = ...
const isLoadingTasks = true
function handleLoginButtonClick() { ... }

// 悪い例
const current_user = ...
const is_loading = true
function handle_login() { ... }
```

#### 3. 定数

**UPPER_SNAKE_CASE**を使用します。

```typescript
// 良い例
const API_BASE_URL = 'http://localhost:8080'
const MAX_FILE_SIZE = 10 * 1024 * 1024  // 10MB
```

#### 4. インターフェース・型

**PascalCase**を使用します。接頭辞 `I` は使用しません。

```typescript
// 良い例
interface User {
  userId: number
  userEmail: string
}

type TaskStatus = 'todo' | 'in_progress' | 'completed'

// 悪い例
interface IUser { ... }  // I接頭辞は不要
```

### データベース命名規則

#### 1. テーブル名

**小文字のスネークケース**、**複数形**を使用します。

```sql
-- 良い例
CREATE TABLE users (...);
CREATE TABLE task_dependencies (...);
CREATE TABLE organization_members (...);

-- 悪い例
CREATE TABLE User (...);  -- 大文字は避ける
CREATE TABLE taskDependency (...);  -- キャメルケースは避ける
CREATE TABLE user (...);  -- 単数形は避ける
```

#### 2. カラム名

**小文字のスネークケース**、**テーブル名のプレフィックス**を付けます。

```sql
-- 良い例
CREATE TABLE users (
  user_id BIGSERIAL PRIMARY KEY,
  user_email VARCHAR(255),
  user_full_name VARCHAR(255),
  user_created_at TIMESTAMP
);

-- 悪い例
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,  -- プレフィックスがない
  Email VARCHAR(255),  -- 大文字は避ける
  fullName VARCHAR(255)  -- キャメルケースは避ける
);
```

**理由**: プレフィックスを付けることで、JOINした際にカラムの所属が明確になります。

```sql
-- プレフィックスがあると明確
SELECT 
  users.user_id,
  users.user_email,
  tasks.task_id,
  tasks.task_title
FROM users
JOIN tasks ON tasks.task_assigned_to_user_id = users.user_id;

-- プレフィックスがないと混乱
SELECT 
  users.id,  -- どのテーブルのid?
  users.email,
  tasks.id,  -- どのテーブルのid?
  tasks.title
FROM users
JOIN tasks ON tasks.assigned_to = users.id;
```

### 変数名の具体例

#### データベースから取得したデータ

```scala
// 良い例
val retrievedUserFromDatabase: Option[User] = ...
val fetchedTaskListFromDatabase: List[Task] = ...
val queriedProjectsByWorkspaceId: List[Project] = ...

// 悪い例
val user: Option[User] = ...
val tasks: List[Task] = ...
val data: List[Project] = ...
```

#### 計算結果

```scala
// 良い例
val calculatedTotalTaskCount: Int = ...
val computedAveragePriority: Double = ...
val determinedTaskStatus: String = ...

// 悪い例
val total: Int = ...
val avg: Double = ...
val status: String = ...
```

#### 一時的な変数

```scala
// 良い例
val temporaryTaskListForFiltering: List[Task] = ...
val intermediateCalculationResult: Int = ...

// 悪い例
val temp: List[Task] = ...
val tmp: Int = ...
```

#### リクエスト/レスポンス

```scala
// 良い例
val userRegistrationRequest: UserRegistrationRequest = ...
val taskCreationResponse: TaskCreationResponse = ...
val loginResponseWithToken: LoginResponse = ...

// 悪い例
val req: UserRegistrationRequest = ...
val res: TaskCreationResponse = ...
val response: LoginResponse = ...
```

---

## Scalaコーディング規約

### 1. ケースクラスの使用

データモデルは、イミュータブルなケースクラスとして定義します。

```scala
// 良い例
case class User(
  userId: Long,
  userEmail: String,
  userPasswordHash: String,
  userFullName: String,
  userCreatedAt: Timestamp,
  userUpdatedAt: Timestamp
)

// 悪い例
class User {
  var userId: Long = _
  var userEmail: String = _
  var userPasswordHash: String = _
  // ...
}
```

### 2. Optionの使用

nullの代わりにOptionを使用します。

```scala
// 良い例
def findUserByEmail(emailAddress: String): Future[Option[User]] = {
  databaseInstance.run(
    usersTableQuery.filter(_.userEmail === emailAddress).result.headOption
  )
}

// 使用例
findUserByEmail("user@example.com").map {
  case Some(foundUser) => 
    println(s"ユーザーが見つかりました: ${foundUser.userFullName}")
  case None => 
    println("ユーザーが見つかりませんでした")
}

// 悪い例
def findUserByEmail(emailAddress: String): Future[User] = {
  // nullを返す可能性がある
}
```

### 3. Eitherによるエラーハンドリング

エラーを表現する際は、Either型を使用します。

```scala
// 良い例
def createNewTask(
  taskCreationRequest: TaskCreationRequest,
  creatorUserId: Long
): Future[Either[String, Task]] = {
  // バリデーション
  if (taskCreationRequest.creationTitle.isEmpty) {
    Future.successful(Left("タスクのタイトルは必須です"))
  } else {
    // タスク作成処理
    databaseInstance.run(insertTaskQuery).map { createdTask =>
      Right(createdTask)
    }
  }
}

// 使用例
createNewTask(request, userId).map {
  case Right(createdTask) => 
    println(s"タスクが作成されました: ${createdTask.taskTitle}")
  case Left(errorMessage) => 
    println(s"エラー: $errorMessage")
}
```

### 4. for式の活用

複数のFutureを扱う際は、for式を使用します。

```scala
// 良い例
def assignTaskToUser(taskId: Long, userId: Long): Future[Either[String, Task]] = {
  for {
    maybeTask <- findTaskById(taskId)
    maybeUser <- findUserById(userId)
  } yield {
    (maybeTask, maybeUser) match {
      case (Some(foundTask), Some(foundUser)) =>
        // タスクを更新
        val updatedTask = foundTask.copy(taskAssignedToUserId = Some(userId))
        Right(updatedTask)
      case (None, _) =>
        Left("タスクが見つかりません")
      case (_, None) =>
        Left("ユーザーが見つかりません")
    }
  }
}

// 悪い例（ネストが深い）
def assignTaskToUser(taskId: Long, userId: Long): Future[Either[String, Task]] = {
  findTaskById(taskId).flatMap { maybeTask =>
    findUserById(userId).map { maybeUser =>
      maybeTask match {
        case Some(foundTask) =>
          maybeUser match {
            case Some(foundUser) =>
              val updatedTask = foundTask.copy(taskAssignedToUserId = Some(userId))
              Right(updatedTask)
            case None =>
              Left("ユーザーが見つかりません")
          }
        case None =>
          Left("タスクが見つかりません")
      }
    }
  }
}
```

### 5. パターンマッチングの活用

条件分岐には、パターンマッチングを使用します。

```scala
// 良い例
def getTaskStatusDisplayName(taskStatus: String): String = taskStatus match {
  case "todo" => "未着手"
  case "in_progress" => "進行中"
  case "completed" => "完了"
  case "archived" => "アーカイブ済み"
  case _ => "不明"
}

// 悪い例
def getTaskStatusDisplayName(taskStatus: String): String = {
  if (taskStatus == "todo") {
    "未着手"
  } else if (taskStatus == "in_progress") {
    "進行中"
  } else if (taskStatus == "completed") {
    "完了"
  } else if (taskStatus == "archived") {
    "アーカイブ済み"
  } else {
    "不明"
  }
}
```

### 6. 暗黙の型変換を避ける

暗黙の型変換（implicit conversion）は、コードを理解しにくくするため避けます。

```scala
// 良い例
def processTask(task: Task): Unit = {
  println(s"タスク処理: ${task.taskTitle}")
}

// 悪い例
implicit def stringToTask(title: String): Task = {
  Task(0, title, "", 0, None, None, None, None, "todo", "medium", None, None, Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now()))
}

processTask("新しいタスク")  // 暗黙の変換が発生（わかりにくい）
```

### 7. メソッドの長さ

1つのメソッドは、30行以内に収めることを推奨します。長くなる場合は、複数のメソッドに分割します。

```scala
// 良い例
def registerNewUser(registrationRequest: UserRegistrationRequest): Future[Either[String, LoginResponse]] = {
  for {
    validationResult <- validateUserRegistrationRequest(registrationRequest)
    existingUser <- checkIfUserAlreadyExists(registrationRequest.registrationEmail)
    createdUser <- createUserInDatabase(registrationRequest)
    generatedToken <- generateJwtTokenForUser(createdUser)
  } yield {
    Right(LoginResponse(generatedToken, UserResponse.fromUser(createdUser)))
  }
}

private def validateUserRegistrationRequest(request: UserRegistrationRequest): Future[Either[String, Unit]] = {
  // バリデーションロジック
}

private def checkIfUserAlreadyExists(email: String): Future[Option[User]] = {
  // 重複チェックロジック
}

private def createUserInDatabase(request: UserRegistrationRequest): Future[User] = {
  // ユーザー作成ロジック
}

private def generateJwtTokenForUser(user: User): Future[String] = {
  // トークン生成ロジック
}
```

---

## TypeScript/Reactコーディング規約

### 1. 関数コンポーネントの使用

クラスコンポーネントではなく、関数コンポーネントを使用します。

```typescript
// 良い例
function LoginPage() {
  const [userEmailInput, setUserEmailInput] = useState('')
  const [userPasswordInput, setUserPasswordInput] = useState('')

  const handleLoginButtonClick = async () => {
    // ログイン処理
  }

  return (
    <div>
      <input 
        type="email" 
        value={userEmailInput} 
        onChange={(e) => setUserEmailInput(e.target.value)} 
      />
      {/* ... */}
    </div>
  )
}

// 悪い例（クラスコンポーネント）
class LoginPage extends React.Component {
  constructor(props) {
    super(props)
    this.state = {
      email: '',
      password: ''
    }
  }

  handleLogin() {
    // ...
  }

  render() {
    return (
      <div>
        {/* ... */}
      </div>
    )
  }
}
```

### 2. 型定義の明示

全ての変数、関数、Propsに型を明示します。

```typescript
// 良い例
interface LoginPageProps {
  redirectPathAfterLogin: string
  onLoginSuccess: (user: User) => void
}

function LoginPage({ redirectPathAfterLogin, onLoginSuccess }: LoginPageProps) {
  const [userEmailInput, setUserEmailInput] = useState<string>('')
  const [isLoadingLogin, setIsLoadingLogin] = useState<boolean>(false)

  const handleLoginButtonClick = async (): Promise<void> => {
    // ...
  }

  return (
    // ...
  )
}

// 悪い例
function LoginPage({ redirectPath, onSuccess }) {
  const [email, setEmail] = useState('')
  const [loading, setLoading] = useState(false)

  const handleLogin = async () => {
    // ...
  }

  return (
    // ...
  )
}
```

### 3. カスタムフックの活用

ロジックを再利用する際は、カスタムフックを作成します。

```typescript
// 良い例
function useAuthenticatedUser() {
  const [currentAuthenticatedUser, setCurrentAuthenticatedUser] = useState<User | null>(null)
  const [isLoadingAuthentication, setIsLoadingAuthentication] = useState<boolean>(true)

  useEffect(() => {
    const loadCurrentUserFromApi = async () => {
      try {
        const retrievedUser = await apiClient.getCurrentUser()
        setCurrentAuthenticatedUser(retrievedUser)
      } catch (error) {
        console.error('ユーザー情報の取得に失敗しました', error)
      } finally {
        setIsLoadingAuthentication(false)
      }
    }

    loadCurrentUserFromApi()
  }, [])

  return { currentAuthenticatedUser, isLoadingAuthentication }
}

// 使用例
function App() {
  const { currentAuthenticatedUser, isLoadingAuthentication } = useAuthenticatedUser()

  if (isLoadingAuthentication) {
    return <div>読み込み中...</div>
  }

  return (
    // ...
  )
}
```

### 4. useEffectの依存配列

useEffectの依存配列は、必ず正確に指定します。

```typescript
// 良い例
useEffect(() => {
  const fetchTasksFromApi = async () => {
    const retrievedTasks = await apiClient.getMyTasks()
    setTasksList(retrievedTasks)
  }

  fetchTasksFromApi()
}, [])  // 空配列 = マウント時のみ実行

useEffect(() => {
  const fetchProjectTasksFromApi = async () => {
    const retrievedTasks = await apiClient.getTasksInProject(selectedProjectId)
    setTasksList(retrievedTasks)
  }

  fetchProjectTasksFromApi()
}, [selectedProjectId])  // selectedProjectIdが変更されたら実行

// 悪い例
useEffect(() => {
  const fetchTasks = async () => {
    const tasks = await apiClient.getMyTasks()
    setTasksList(tasks)
  }

  fetchTasks()
})  // 依存配列なし = 毎回実行（無限ループの可能性）
```

### 5. 条件付きレンダリング

条件付きレンダリングは、明確に記述します。

```typescript
// 良い例
function TasksPage() {
  const [tasksList, setTasksList] = useState<Task[]>([])
  const [isLoadingTasks, setIsLoadingTasks] = useState<boolean>(true)

  if (isLoadingTasks) {
    return <div>タスクを読み込み中...</div>
  }

  if (tasksList.length === 0) {
    return <div>タスクがありません</div>
  }

  return (
    <div>
      {tasksList.map((task) => (
        <TaskCard key={task.taskId} task={task} />
      ))}
    </div>
  )
}

// 悪い例
function TasksPage() {
  const [tasks, setTasks] = useState([])
  const [loading, setLoading] = useState(true)

  return (
    <div>
      {loading ? (
        <div>読み込み中...</div>
      ) : tasks.length === 0 ? (
        <div>タスクがありません</div>
      ) : (
        tasks.map((task) => <TaskCard key={task.taskId} task={task} />)
      )}
    </div>
  )
}
```

### 6. イベントハンドラの命名

イベントハンドラは、`handle` + `要素` + `イベント` の形式で命名します。

```typescript
// 良い例
const handleLoginButtonClick = () => { ... }
const handleEmailInputChange = (e: React.ChangeEvent<HTMLInputElement>) => { ... }
const handleFormSubmit = (e: React.FormEvent) => { ... }

// 悪い例
const onClick = () => { ... }
const change = (e) => { ... }
const submit = (e) => { ... }
```

---

## データベース規約

### 1. 外部キー制約

全ての外部キーには、制約を設定します。

```sql
-- 良い例
CREATE TABLE tasks (
  task_id BIGSERIAL PRIMARY KEY,
  task_project_id BIGINT NOT NULL,
  task_assigned_to_user_id BIGINT,
  
  FOREIGN KEY (task_project_id) REFERENCES projects(project_id) ON DELETE CASCADE,
  FOREIGN KEY (task_assigned_to_user_id) REFERENCES users(user_id) ON DELETE SET NULL
);

-- 悪い例
CREATE TABLE tasks (
  task_id BIGSERIAL PRIMARY KEY,
  task_project_id BIGINT NOT NULL,
  task_assigned_to_user_id BIGINT
  -- 外部キー制約がない
);
```

### 2. インデックスの作成

頻繁に検索されるカラムには、インデックスを作成します。

```sql
-- 良い例
CREATE INDEX idx_tasks_project_id ON tasks(task_project_id);
CREATE INDEX idx_tasks_assigned_to_user_id ON tasks(task_assigned_to_user_id);
CREATE INDEX idx_tasks_status ON tasks(task_status);
CREATE INDEX idx_tasks_due_date ON tasks(task_due_date);

-- 複合インデックス（複数カラムでの検索が多い場合）
CREATE INDEX idx_tasks_project_status ON tasks(task_project_id, task_status);
```

### 3. NOT NULL制約

必須のカラムには、NOT NULL制約を設定します。

```sql
-- 良い例
CREATE TABLE users (
  user_id BIGSERIAL PRIMARY KEY,
  user_email VARCHAR(255) NOT NULL,
  user_password_hash VARCHAR(255) NOT NULL,
  user_full_name VARCHAR(255) NOT NULL,
  user_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 悪い例
CREATE TABLE users (
  user_id BIGSERIAL PRIMARY KEY,
  user_email VARCHAR(255),  -- NULLを許可してしまう
  user_password_hash VARCHAR(255),
  user_full_name VARCHAR(255)
);
```

### 4. デフォルト値の設定

適切なデフォルト値を設定します。

```sql
-- 良い例
CREATE TABLE tasks (
  task_id BIGSERIAL PRIMARY KEY,
  task_status VARCHAR(50) NOT NULL DEFAULT 'todo',
  task_priority VARCHAR(50) NOT NULL DEFAULT 'medium',
  task_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  task_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

## コメント規約

### 1. コメントの基本方針

コードは自己説明的であるべきですが、**なぜそうしたのか**を説明するコメントは有用です。

```scala
// 良い例: 「なぜ」を説明
// BCryptは計算コストが高いため、パスワード検証は非同期で実行する
Future {
  PasswordHashingUtility.verifyPassword(providedPassword, storedPasswordHash)
}

// タスクの依存関係をチェックし、依存先タスクが完了していない場合はエラーを返す
// これにより、タスクの順序を保証する
if (hasPendingDependencies(taskId)) {
  return Left("依存しているタスクが完了していません")
}

// 悪い例: 「何を」説明（コードを見ればわかる）
// パスワードを検証する
Future {
  PasswordHashingUtility.verifyPassword(providedPassword, storedPasswordHash)
}

// ifで依存関係をチェック
if (hasPendingDependencies(taskId)) {
  return Left("依存しているタスクが完了していません")
}
```

### 2. クラス・メソッドのコメント

全てのpublicクラスとメソッドには、説明コメントを付けます。

```scala
/**
 * ユーザー認証サービス
 * 
 * このサービスは、ユーザーの登録、ログイン、ログアウトを管理します。
 * JWTトークンを使用したステートレス認証を実装しています。
 * 
 * @param databaseInstance Slickデータベースインスタンス
 * @param jwtAuthHandler JWT認証ハンドラー
 */
class AuthenticationService(
  databaseInstance: Database,
  jwtAuthHandler: JwtAuthenticationHandler
) {

  /**
   * 新規ユーザーを登録します
   * 
   * このメソッドは、以下の処理を行います：
   * 1. メールアドレスの重複チェック
   * 2. パスワードのハッシュ化（BCrypt）
   * 3. データベースへのユーザー挿入
   * 4. JWTトークンの生成
   * 
   * @param registrationRequest ユーザー登録リクエスト
   * @return 成功時はLoginResponse、失敗時はエラーメッセージ
   */
  def registerNewUser(
    registrationRequest: UserRegistrationRequest
  ): Future[Either[String, LoginResponse]] = {
    // ...
  }
}
```

### 3. TODOコメント

未実装の機能や改善点には、TODOコメントを付けます。

```scala
// TODO: タスクの優先度を自動計算する機能を実装
// TODO: 通知をリアルタイムで送信する（WebSocket）
// TODO: パフォーマンス改善 - N+1問題を解決
// FIXME: タイムゾーンの処理が正しくない
```

---

## エラーハンドリング

### 1. Scalaのエラーハンドリング

Eitherを使用して、エラーを明示的に処理します。

```scala
// 良い例
def createNewTask(
  taskCreationRequest: TaskCreationRequest,
  creatorUserId: Long
): Future[Either[String, Task]] = {
  
  // バリデーション
  if (taskCreationRequest.creationTitle.trim.isEmpty) {
    return Future.successful(Left("タスクのタイトルは必須です"))
  }
  
  if (taskCreationRequest.creationTitle.length > 500) {
    return Future.successful(Left("タスクのタイトルは500文字以内で入力してください"))
  }
  
  // プロジェクトの存在確認
  findProjectById(taskCreationRequest.creationProjectId).flatMap {
    case None =>
      Future.successful(Left("指定されたプロジェクトが見つかりません"))
    case Some(foundProject) =>
      // タスク作成処理
      createTaskInDatabase(taskCreationRequest, creatorUserId).map { createdTask =>
        Right(createdTask)
      }.recover {
        case exception: Exception =>
          logger.error(s"タスク作成中にエラーが発生しました: ${exception.getMessage}", exception)
          Left("タスクの作成に失敗しました")
      }
  }
}
```

### 2. TypeScriptのエラーハンドリング

try-catchを使用して、エラーを適切に処理します。

```typescript
// 良い例
async function handleLoginButtonClick() {
  setIsLoadingLogin(true)
  setLoginErrorMessage('')

  try {
    const loginResponse = await apiClient.login(userEmailInput, userPasswordInput)
    
    // トークンを保存
    localStorage.setItem('jwtToken', loginResponse.loginToken)
    
    // ユーザー情報を設定
    setCurrentAuthenticatedUser(loginResponse.loginUser)
    
    // ダッシュボードにリダイレクト
    navigate('/dashboard')
    
  } catch (error) {
    if (axios.isAxiosError(error)) {
      if (error.response?.status === 401) {
        setLoginErrorMessage('メールアドレスまたはパスワードが正しくありません')
      } else if (error.response?.status === 500) {
        setLoginErrorMessage('サーバーエラーが発生しました。しばらくしてから再度お試しください')
      } else {
        setLoginErrorMessage('ログインに失敗しました')
      }
    } else {
      setLoginErrorMessage('予期しないエラーが発生しました')
    }
    
    console.error('ログインエラー:', error)
  } finally {
    setIsLoadingLogin(false)
  }
}
```

---

## テスト規約

### 1. テストの命名

テストメソッド名は、「何をテストするか」を明確に記述します。

```scala
// 良い例
class AuthenticationServiceTest extends AnyFlatSpec with Matchers {
  
  "registerNewUser" should "正常にユーザーを登録できること" in {
    // ...
  }
  
  it should "重複したメールアドレスの場合はエラーを返すこと" in {
    // ...
  }
  
  it should "空のパスワードの場合はエラーを返すこと" in {
    // ...
  }
  
  "authenticateUser" should "正しいパスワードでログインできること" in {
    // ...
  }
  
  it should "間違ったパスワードの場合はエラーを返すこと" in {
    // ...
  }
}
```

### 2. テストの構造（AAA パターン）

テストは、Arrange（準備）、Act（実行）、Assert（検証）の3つのセクションに分けます。

```scala
"createNewTask" should "正常にタスクを作成できること" in {
  // Arrange（準備）
  val taskCreationRequest = TaskCreationRequest(
    creationTitle = "新しいタスク",
    creationDescription = "タスクの説明",
    creationProjectId = 1,
    creationSectionId = Some(1),
    creationAssignedToUserId = Some(2),
    creationPriority = "high",
    creationDueDate = Some(LocalDate.now().plusDays(7))
  )
  val creatorUserId = 1L
  
  // Act（実行）
  val result = Await.result(
    taskManagementService.createNewTask(taskCreationRequest, creatorUserId),
    Duration.Inf
  )
  
  // Assert（検証）
  result should be a 'right
  val createdTask = result.right.get
  createdTask.taskTitle should be("新しいタスク")
  createdTask.taskPriority should be("high")
  createdTask.taskCreatedByUserId should be(creatorUserId)
}
```

---

## Git規約

### 1. コミットメッセージ

コミットメッセージは、以下の形式で記述します。

```
[種類] 簡潔な説明（50文字以内）

詳細な説明（必要に応じて）
- 変更の理由
- 影響範囲
- 関連するissue番号
```

**種類**:
- `feat`: 新機能
- `fix`: バグ修正
- `docs`: ドキュメントのみの変更
- `style`: コードの意味に影響しない変更（フォーマット、セミコロンなど）
- `refactor`: リファクタリング
- `test`: テストの追加・修正
- `chore`: ビルドプロセスやツールの変更

**例**:
```
feat: タスク検索機能を追加

タスクのタイトル、説明、タグで検索できるようにしました。
検索結果はページネーション対応です。

関連issue: #123
```

### 2. ブランチ戦略

**ブランチ命名規則**:
- `main`: 本番環境
- `develop`: 開発環境
- `feature/機能名`: 新機能開発
- `fix/バグ名`: バグ修正
- `refactor/対象`: リファクタリング

**例**:
```
feature/task-search
fix/login-validation
refactor/database-schema
```

---

## コードレビューチェックリスト

コードレビュー時は、以下の項目を確認します。

### 機能性
- [ ] 要件を満たしているか
- [ ] エッジケースを考慮しているか
- [ ] エラーハンドリングが適切か

### コード品質
- [ ] 変数名・関数名がわかりやすいか
- [ ] 一般的な名前（data, result, tempなど）を避けているか
- [ ] コードが自己説明的か
- [ ] 不要なコメントがないか
- [ ] 重複コードがないか

### 型安全性
- [ ] 型が明示されているか
- [ ] Optionを適切に使用しているか
- [ ] Eitherでエラーを表現しているか

### パフォーマンス
- [ ] N+1問題がないか
- [ ] 不要なデータベースクエリがないか
- [ ] 適切にインデックスを使用しているか

### セキュリティ
- [ ] SQLインジェクション対策がされているか
- [ ] パスワードがハッシュ化されているか
- [ ] JWTトークンが適切に検証されているか
- [ ] 機密情報がログに出力されていないか

### テスト
- [ ] ユニットテストが書かれているか
- [ ] テストケースが十分か
- [ ] エッジケースのテストがあるか

---

## まとめ

このコーディングガイドラインは、以下の目的で作成されました。

1. **可読性**: 後で読む人（あなた自身を含む）がコードを理解しやすくする
2. **保守性**: コードの変更や拡張を容易にする
3. **一貫性**: プロジェクト全体で統一されたスタイルを保つ
4. **品質**: バグを減らし、安全なコードを書く

このガイドラインを参考に、わかりやすく、保守しやすいコードを書いてください。
