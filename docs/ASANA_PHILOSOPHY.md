# Asanaの設計思想とこのプロジェクトへの適用

## 目次
1. [Asanaとは](#asanaとは)
2. [Asanaの核となる設計思想](#asanaの核となる設計思想)
3. [このプロジェクトでの実装方針](#このプロジェクトでの実装方針)
4. [技術スタックの選定理由](#技術スタックの選定理由)

---

## Asanaとは

Asanaは、チームのタスク管理とプロジェクト管理を効率化するためのWebアプリケーションです。2008年にFacebookの元エンジニアであるDustin MoskovitzとJustin Rosensteinによって創業されました。

### Asanaが解決する問題

1. **タスクの散在**: メール、チャット、ドキュメントなど、様々な場所に散らばったタスク情報を一元管理
2. **進捗の可視化**: プロジェクトの進行状況をリアルタイムで把握
3. **責任の明確化**: 誰が何をいつまでにやるのかを明確にする
4. **チーム間の連携**: 複数のチームやプロジェクトをまたいだ協力を促進

---

## Asanaの核となる設計思想

### 1. 階層構造の明確化 (Clear Hierarchy)

Asanaは以下の階層構造を採用しています：

```
組織 (Organization)
  └── ワークスペース (Workspace)
      └── プロジェクト (Project)
          └── セクション (Section)
              └── タスク (Task)
                  └── サブタスク (Subtask)
```

**このプロジェクトでの実装:**
- `organizations` テーブル: 会社や大きな組織単位
- `workspaces` テーブル: チームや部門単位
- `projects` テーブル: 具体的なプロジェクト
- `sections` テーブル: プロジェクト内のグループ分け（例: 「未着手」「進行中」「完了」）
- `tasks` テーブル: 実際の作業単位
- `subtasks` テーブル: タスクをさらに細分化したもの

**初心者向け解説:**
階層構造とは、大きなものから小さなものへと段階的に分けていく構造のことです。例えば、「会社」の中に「営業部」があり、「営業部」の中に「新規顧客獲得プロジェクト」があり、その中に「見積書作成」というタスクがある、という具合です。

---

### 2. タスク中心の設計 (Task-Centric Design)

Asanaでは、全ての活動が「タスク」を中心に展開されます。

**タスクの重要な属性:**
- **担当者 (Assignee)**: 誰がそのタスクを実行するか
- **期限 (Due Date)**: いつまでに完了すべきか
- **優先度 (Priority)**: どれくらい重要か
- **依存関係 (Dependencies)**: 他のどのタスクが完了してから始められるか
- **タグ (Tags)**: カテゴリ分けのためのラベル
- **添付ファイル (Attachments)**: 関連する資料
- **コメント (Comments)**: タスクに関する議論

**このプロジェクトでの実装:**
```sql
tasks テーブルの主要カラム:
- task_id: タスクの一意な識別子
- task_title: タスクの名前（例: 「見積書を作成する」）
- task_description: 詳細な説明
- assigned_user_id: 担当者のID
- due_date_time: 期限
- priority_level: 優先度（1=低、2=中、3=高、4=緊急）
- current_status: 現在の状態（未着手、進行中、完了など）
- parent_task_id: 親タスクのID（サブタスクの場合）
```

**初心者向け解説:**
タスク中心の設計とは、全ての機能がタスクを軸に作られているということです。プロジェクトを見るのも、チームメンバーを見るのも、最終的には「どのタスクがあるか」「誰がどのタスクを担当しているか」を確認するためです。

---

### 3. 柔軟なビュー (Flexible Views)

Asanaは同じデータを複数の視点から見ることができます：

1. **リストビュー (List View)**: タスクを一覧表示
2. **ボードビュー (Board View)**: カンバン方式でタスクをカード表示
3. **タイムラインビュー (Timeline View)**: ガントチャート形式で時系列表示
4. **カレンダービュー (Calendar View)**: カレンダー形式で期限を表示

**このプロジェクトでの実装:**
バックエンドでは同じデータを提供し、フロントエンドで表示方法を切り替えます。

```
GET /api/projects/{projectId}/tasks
→ 全てのタスクデータを返す

フロントエンド側で:
- ListViewコンポーネント: 縦に並べて表示
- BoardViewコンポーネント: ステータスごとに列を分けて表示
- TimelineViewコンポーネント: 期限順に横軸で表示
```

**初心者向け解説:**
同じタスクのデータでも、見せ方を変えることで理解しやすくなります。例えば、「今日やるべきこと」を見たいときはカレンダービュー、「プロジェクト全体の流れ」を見たいときはタイムラインビューが便利です。

---

### 4. リアルタイム同期 (Real-time Synchronization)

複数のユーザーが同時に作業しても、変更が即座に反映されます。

**このプロジェクトでの実装:**
- WebSocketを使用してリアルタイム通信
- タスクの更新、コメントの追加などをリアルタイムで通知
- 楽観的ロック (Optimistic Locking) で競合を防止

```scala
// WebSocketでの更新通知の例
case class TaskUpdateNotification(
  notification_type: String,  // "task_updated", "comment_added" など
  task_id: Long,
  updated_by_user_id: Long,
  update_timestamp: Timestamp,
  changed_fields: Map[String, Any]
)
```

**初心者向け解説:**
リアルタイム同期とは、誰かがタスクを更新したら、他の人の画面にも即座に反映される仕組みです。メールのように「更新ボタンを押さないと新しい情報が見えない」ということがありません。

---

### 5. 権限管理 (Permission Management)

誰が何を見られるか、何を編集できるかを細かく制御します。

**権限レベル:**
1. **組織管理者 (Organization Admin)**: 全ての権限
2. **ワークスペース管理者 (Workspace Admin)**: ワークスペース内の全ての権限
3. **プロジェクトオーナー (Project Owner)**: プロジェクトの編集・削除権限
4. **プロジェクトメンバー (Project Member)**: タスクの作成・編集権限
5. **閲覧者 (Viewer)**: 読み取り専用

**このプロジェクトでの実装:**
```sql
workspace_members テーブル:
- workspace_id: どのワークスペースか
- user_id: どのユーザーか
- member_role: 役割（admin, member, viewer）

project_members テーブル:
- project_id: どのプロジェクトか
- user_id: どのユーザーか
- member_role: 役割（owner, editor, viewer）
```

**初心者向け解説:**
権限管理とは、「誰が何をできるか」を決める仕組みです。例えば、アルバイトスタッフには自分のタスクだけを見せて、マネージャーには全員のタスクを見せる、といった制御ができます。

---

### 6. 検索とフィルタリング (Search and Filtering)

大量のタスクの中から必要な情報を素早く見つけられます。

**検索機能:**
- タスク名での検索
- 担当者での絞り込み
- 期限での絞り込み
- タグでの絞り込み
- ステータスでの絞り込み
- 複数条件の組み合わせ

**このプロジェクトでの実装:**
```scala
// 検索APIの例
GET /api/tasks/search?
  query=見積書&
  assignee=123&
  status=in_progress&
  due_before=2024-12-31&
  tags=urgent,sales
```

**初心者向け解説:**
検索とフィルタリングは、たくさんのタスクの中から「今週締め切りのタスク」や「田中さんが担当しているタスク」など、特定の条件に合うものだけを表示する機能です。

---

### 7. 通知システム (Notification System)

重要な変更を見逃さないように通知します。

**通知のタイミング:**
- 自分にタスクが割り当てられたとき
- 自分のタスクにコメントがついたとき
- 自分が関わっているタスクが更新されたとき
- 期限が近づいたとき
- 依存しているタスクが完了したとき

**このプロジェクトでの実装:**
```sql
notifications テーブル:
- notification_id: 通知の識別子
- recipient_user_id: 通知を受け取るユーザー
- notification_type: 通知の種類
- related_task_id: 関連するタスク
- notification_message: 通知メッセージ
- is_read: 既読かどうか
- created_at: 通知が作成された時刻
```

**初心者向け解説:**
通知システムは、重要な変更があったときに自動的にお知らせしてくれる機能です。LINEの通知のように、「新しいタスクが割り当てられました」といったメッセージが届きます。

---

## このプロジェクトでの実装方針

### 設計の優先順位

1. **シンプルさ (Simplicity)**: 複雑な機能よりも、基本機能の使いやすさを優先
2. **拡張性 (Scalability)**: 将来的に機能を追加しやすい設計
3. **パフォーマンス (Performance)**: 大量のタスクでも快適に動作
4. **保守性 (Maintainability)**: コードが読みやすく、修正しやすい

### コーディング規約

**変数名の命名規則:**

❌ **避けるべき一般的な名前:**
```scala
val data = ...
val result = ...
val temp = ...
val item = ...
```

✅ **推奨する具体的な名前:**
```scala
val retrievedTaskFromDatabase = ...
val calculatedNewPriorityLevel = ...
val temporaryTaskListBeforeSorting = ...
val currentlySelectedProjectItem = ...
```

**命名の原則:**
1. **役割を明確に**: 変数が何を表すのか、名前だけで分かるようにする
2. **動詞を含める**: 処理を表す変数には動詞を入れる（例: `calculatedTotal`, `filteredTasks`）
3. **出所を明記**: データの出所を含める（例: `taskFromDatabase`, `userInputText`）
4. **状態を示す**: 現在の状態を含める（例: `isTaskCompleted`, `hasUserPermission`）

**関数名の命名規則:**

✅ **推奨する関数名:**
```scala
def retrieveAllTasksAssignedToSpecificUser(userId: Long): List[Task]
def calculateUpdatedPriorityBasedOnDueDate(task: Task): Int
def validateUserHasPermissionToEditTask(userId: Long, taskId: Long): Boolean
def sendNotificationToTaskAssignee(task: Task, message: String): Unit
```

**クラス名の命名規則:**

✅ **推奨するクラス名:**
```scala
class TaskManagementService
class UserAuthenticationHandler
class ProjectMembershipValidator
class DatabaseConnectionManager
```

---

## 技術スタックの選定理由

### バックエンド: Scala + Akka HTTP

**選定理由:**
1. **型安全性**: コンパイル時にエラーを検出できる
2. **並行処理**: Akkaの Actor モデルで効率的な並行処理
3. **関数型プログラミング**: 副作用を減らし、テストしやすいコード
4. **Javaエコシステム**: 豊富なライブラリが使える

**Asanaとの関連:**
Asanaも初期はRuby on Railsでしたが、スケーラビリティのためにScalaに移行しました。大量のタスクとユーザーを処理するには、Scalaの並行処理能力が重要です。

### データベース: PostgreSQL

**選定理由:**
1. **ACID特性**: データの整合性が保証される
2. **複雑なクエリ**: JOINやサブクエリが効率的
3. **JSONB型**: 柔軟なデータ構造も扱える
4. **トランザクション**: 複数の操作をまとめて実行できる

**Asanaとの関連:**
タスク管理では、「タスクAが完了したらタスクBを開始」といった依存関係や、複数のテーブルをまたいだ検索が頻繁に発生します。PostgreSQLの強力なクエリ機能が役立ちます。

### フロントエンド: React (Web) + React Native (Mobile)

**選定理由:**
1. **コンポーネント指向**: 再利用可能なUIパーツを作れる
2. **仮想DOM**: 効率的な画面更新
3. **豊富なエコシステム**: ライブラリやツールが充実
4. **クロスプラットフォーム**: WebとMobileでコードを共有できる

**Asanaとの関連:**
Asanaは複雑なUIを持つアプリケーションです。タスクリスト、ボードビュー、タイムラインなど、様々なコンポーネントを組み合わせる必要があり、Reactのコンポーネント指向が適しています。

### ビルドツール: Bazel

**選定理由:**
1. **モノレポ対応**: Backend、Web、Mobileを一つのリポジトリで管理
2. **増分ビルド**: 変更部分だけを再ビルド
3. **再現性**: 同じコードから常に同じ結果
4. **並列ビルド**: 複数のターゲットを同時にビルド

**Asanaとの関連:**
Asanaは大規模なコードベースを持ち、複数のチームが同時に開発しています。Bazelのような強力なビルドツールで、ビルド時間を短縮し、開発効率を上げています。

---

## まとめ

このプロジェクトは、Asanaの設計思想を学びながら、実際に動くタスク管理アプリケーションを構築することを目的としています。

**学べること:**
1. 階層構造を持つデータモデルの設計
2. RESTful APIの設計と実装
3. リアルタイム通信の実装
4. 権限管理の実装
5. モノレポでの開発手法
6. Scalaでの関数型プログラミング
7. Reactでのコンポーネント設計

**次のステップ:**
1. データベーススキーマの詳細設計
2. バックエンドAPIの実装
3. フロントエンドUIの実装
4. テストの作成
5. デプロイ

このドキュメントを読んだ後は、`DEVELOPMENT.md` で実際の開発手順を確認してください。
