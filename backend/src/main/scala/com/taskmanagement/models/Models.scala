package com.taskmanagement.models

import java.sql.Timestamp
import java.time.{LocalDate, Instant}

/**
 * ドメインモデル定義
 * 
 * このファイルには、アプリケーションで使用する全てのデータモデルが定義されています。
 * 各モデルは、データベースのテーブルに対応しています。
 * 
 * 初心者向け解説:
 * - case class: データを保持するためのクラス（Javaのレコードのようなもの）
 * - Option[T]: 値があるかないか分からない場合に使う（nullの代わり）
 * - Long: 大きな整数（IDに使用）
 * - String: 文字列
 * - Timestamp: 日時
 * - Boolean: true/false
 */


/**
 * ユーザーモデル
 * 
 * アプリケーションを使用する全てのユーザーの情報を表します。
 * 
 * @param userId ユーザーの一意な識別子
 * @param userEmail ログインに使用するメールアドレス
 * @param userFullName ユーザーの本名（例: 山田太郎）
 * @param userDisplayName 表示名（例: やまだ）
 * @param hashedPasswordValue BCryptでハッシュ化されたパスワード
 * @param userProfileImageUrl プロフィール画像のURL
 * @param userTimezone ユーザーのタイムゾーン（例: Asia/Tokyo）
 * @param userLanguagePreference 言語設定（例: ja, en）
 * @param accountCreatedAt アカウント作成日時
 * @param accountLastLoginAt 最終ログイン日時
 * @param isAccountActive アカウントが有効かどうか
 * @param accountDeletedAt アカウント削除日時（論理削除）
 */
case class User(
  userId: Long,
  userEmail: String,
  userFullName: String,
  userDisplayName: Option[String],
  hashedPasswordValue: String,
  userProfileImageUrl: Option[String],
  userTimezone: String,
  userLanguagePreference: String,
  accountCreatedAt: Timestamp,
  accountLastLoginAt: Option[Timestamp],
  isAccountActive: Boolean,
  accountDeletedAt: Option[Timestamp]
)

/**
 * ユーザー登録リクエスト
 * 
 * 新規ユーザー登録時にクライアントから送信されるデータ
 */
case class UserRegistrationRequest(
  userEmail: String,
  userFullName: String,
  plainTextPassword: String,
  userDisplayName: Option[String] = None
)

/**
 * ユーザーログインリクエスト
 * 
 * ログイン時にクライアントから送信されるデータ
 */
case class UserLoginRequest(
  userEmail: String,
  plainTextPassword: String
)

/**
 * ユーザーレスポンス
 * 
 * クライアントに返すユーザー情報（パスワードは含まない）
 */
case class UserResponse(
  userId: Long,
  userEmail: String,
  userFullName: String,
  userDisplayName: Option[String],
  userProfileImageUrl: Option[String],
  userTimezone: String,
  userLanguagePreference: String
)


/**
 * 組織モデル
 * 
 * 会社や大きな組織の単位を表します。
 */
case class Organization(
  organizationId: Long,
  organizationName: String,
  organizationDescription: Option[String],
  organizationCreatedAt: Timestamp,
  organizationOwnerUserId: Long,
  isOrganizationActive: Boolean,
  organizationDeletedAt: Option[Timestamp]
)

/**
 * 組織メンバーモデル
 * 
 * どのユーザーがどの組織に所属しているかを表します。
 */
case class OrganizationMember(
  organizationMemberId: Long,
  organizationId: Long,
  memberUserId: Long,
  memberRoleInOrganization: String, // "owner", "admin", "member"
  memberJoinedOrganizationAt: Timestamp,
  memberLeftOrganizationAt: Option[Timestamp]
)

/**
 * ワークスペースモデル
 * 
 * チームや部門の単位を表します。
 */
case class Workspace(
  workspaceId: Long,
  workspaceName: String,
  workspaceDescription: Option[String],
  parentOrganizationId: Long,
  workspaceCreatedAt: Timestamp,
  workspaceCreatorUserId: Long,
  isWorkspaceActive: Boolean,
  workspaceDeletedAt: Option[Timestamp]
)

/**
 * ワークスペースメンバーモデル
 * 
 * どのユーザーがどのワークスペースに所属しているかを表します。
 */
case class WorkspaceMember(
  workspaceMemberId: Long,
  workspaceId: Long,
  memberUserId: Long,
  memberRoleInWorkspace: String, // "admin", "member", "viewer"
  memberJoinedWorkspaceAt: Timestamp,
  memberLeftWorkspaceAt: Option[Timestamp]
)


/**
 * プロジェクトモデル
 * 
 * 具体的なプロジェクト（例: 「新商品開発」「Webサイトリニューアル」）を表します。
 */
case class Project(
  projectId: Long,
  projectName: String,
  projectDescription: Option[String],
  parentWorkspaceId: Long,
  projectOwnerUserId: Long,
  projectColorCode: String,
  projectIconName: Option[String],
  projectStartDate: Option[LocalDate],
  projectDueDate: Option[LocalDate],
  projectCurrentStatus: String, // "on_track", "at_risk", "off_track", "completed", "on_hold"
  projectCreatedAt: Timestamp,
  projectUpdatedAt: Timestamp,
  isProjectArchived: Boolean,
  projectArchivedAt: Option[Timestamp],
  projectDeletedAt: Option[Timestamp]
)

/**
 * プロジェクト作成リクエスト
 */
case class ProjectCreationRequest(
  projectName: String,
  projectDescription: Option[String],
  parentWorkspaceId: Long,
  projectColorCode: Option[String] = Some("#4A90E2"),
  projectStartDate: Option[LocalDate] = None,
  projectDueDate: Option[LocalDate] = None
)

/**
 * プロジェクトメンバーモデル
 * 
 * どのユーザーがどのプロジェクトに参加しているかを表します。
 */
case class ProjectMember(
  projectMemberId: Long,
  projectId: Long,
  memberUserId: Long,
  memberRoleInProject: String, // "owner", "editor", "commenter", "viewer"
  memberJoinedProjectAt: Timestamp,
  memberLeftProjectAt: Option[Timestamp]
)

/**
 * セクションモデル
 * 
 * プロジェクト内のグループ分け（例: 「未着手」「進行中」「完了」）を表します。
 */
case class Section(
  sectionId: Long,
  sectionName: String,
  parentProjectId: Long,
  sectionDisplayOrder: Int,
  sectionCreatedAt: Timestamp,
  sectionDeletedAt: Option[Timestamp]
)


/**
 * タスクモデル
 * 
 * 実際の作業単位（例: 「見積書を作成する」「会議資料を準備する」）を表します。
 */
case class Task(
  taskId: Long,
  taskTitle: String,
  taskDescription: Option[String],
  parentProjectId: Long,
  parentSectionId: Option[Long],
  parentTaskId: Option[Long], // サブタスクの場合
  assignedUserId: Option[Long],
  taskDueDateTime: Option[Timestamp],
  taskStartDate: Option[LocalDate],
  priorityLevel: Int, // 1=低, 2=中, 3=高, 4=緊急
  currentTaskStatus: String, // "not_started", "in_progress", "waiting", "completed", "cancelled"
  isTaskCompleted: Boolean,
  taskCompletedAt: Option[Timestamp],
  taskCompletedByUserId: Option[Long],
  taskDisplayOrder: Int,
  taskCreatedAt: Timestamp,
  taskCreatedByUserId: Long,
  taskUpdatedAt: Timestamp,
  taskUpdatedByUserId: Option[Long],
  taskDeletedAt: Option[Timestamp]
)

/**
 * タスク作成リクエスト
 */
case class TaskCreationRequest(
  taskTitle: String,
  taskDescription: Option[String],
  parentProjectId: Long,
  parentSectionId: Option[Long],
  parentTaskId: Option[Long] = None,
  assignedUserId: Option[Long] = None,
  taskDueDateTime: Option[Timestamp] = None,
  priorityLevel: Int = 2
)

/**
 * タスク更新リクエスト
 */
case class TaskUpdateRequest(
  taskTitle: Option[String] = None,
  taskDescription: Option[String] = None,
  parentSectionId: Option[Long] = None,
  assignedUserId: Option[Long] = None,
  taskDueDateTime: Option[Timestamp] = None,
  priorityLevel: Option[Int] = None,
  currentTaskStatus: Option[String] = None
)

/**
 * タスクレスポンス（詳細情報付き）
 */
case class TaskDetailResponse(
  taskId: Long,
  taskTitle: String,
  taskDescription: Option[String],
  projectName: String,
  sectionName: Option[String],
  assignedUserName: Option[String],
  taskDueDateTime: Option[Timestamp],
  priorityLevel: Int,
  currentTaskStatus: String,
  isTaskCompleted: Boolean,
  taskCreatedAt: Timestamp,
  taskUpdatedAt: Timestamp
)

/**
 * タスク依存関係モデル
 * 
 * タスク間の依存関係（「タスクAが完了したらタスクBを開始」など）を表します。
 */
case class TaskDependency(
  taskDependencyId: Long,
  dependentTaskId: Long, // 依存する側のタスク（後で実行するタスク）
  prerequisiteTaskId: Long, // 前提となるタスク（先に完了すべきタスク）
  dependencyType: String, // "finish_to_start", "start_to_start", "finish_to_finish", "start_to_finish"
  dependencyCreatedAt: Timestamp
)

/**
 * タグモデル
 * 
 * タスクに付けるラベル（例: 「緊急」「営業」「デザイン」）を表します。
 */
case class Tag(
  tagId: Long,
  tagName: String,
  tagColorCode: String,
  parentWorkspaceId: Long,
  tagCreatedAt: Timestamp,
  tagDeletedAt: Option[Timestamp]
)

/**
 * タスク・タグ関連モデル
 * 
 * どのタスクにどのタグが付いているかを表します。
 */
case class TaskTag(
  taskTagId: Long,
  taskId: Long,
  tagId: Long,
  tagAttachedAt: Timestamp
)


/**
 * コメントモデル
 * 
 * タスクに対するコメント（議論や進捗報告など）を表します。
 */
case class Comment(
  commentId: Long,
  parentTaskId: Long,
  commentAuthorUserId: Long,
  commentTextContent: String,
  commentCreatedAt: Timestamp,
  commentUpdatedAt: Option[Timestamp],
  commentDeletedAt: Option[Timestamp]
)

/**
 * コメント作成リクエスト
 */
case class CommentCreationRequest(
  parentTaskId: Long,
  commentTextContent: String
)

/**
 * 添付ファイルモデル
 * 
 * タスクに添付されたファイル（画像、PDF、Excelなど）を表します。
 */
case class Attachment(
  attachmentId: Long,
  parentTaskId: Long,
  attachmentFileName: String,
  attachmentFileUrl: String,
  attachmentFileSizeBytes: Option[Long],
  attachmentMimeType: Option[String],
  uploadedByUserId: Long,
  attachmentUploadedAt: Timestamp,
  attachmentDeletedAt: Option[Timestamp]
)


/**
 * 通知モデル
 * 
 * ユーザーへの通知（タスク割り当て、コメント追加など）を表します。
 */
case class Notification(
  notificationId: Long,
  recipientUserId: Long,
  notificationType: String, // "task_assigned", "comment_added", "task_completed", etc.
  relatedTaskId: Option[Long],
  relatedProjectId: Option[Long],
  relatedCommentId: Option[Long],
  notificationMessage: String,
  isNotificationRead: Boolean,
  notificationCreatedAt: Timestamp,
  notificationReadAt: Option[Timestamp]
)

/**
 * 通知レスポンス
 */
case class NotificationResponse(
  notificationId: Long,
  notificationType: String,
  notificationMessage: String,
  isNotificationRead: Boolean,
  notificationCreatedAt: Timestamp,
  relatedTaskTitle: Option[String],
  relatedProjectName: Option[String]
)


/**
 * アクティビティログモデル
 * 
 * 全ての変更履歴を記録（誰が、いつ、何を変更したか）します。
 */
case class ActivityLog(
  activityLogId: Long,
  actorUserId: Long,
  activityType: String, // "task_created", "task_updated", "task_completed", etc.
  relatedTaskId: Option[Long],
  relatedProjectId: Option[Long],
  relatedWorkspaceId: Option[Long],
  activityDescription: String,
  activityMetadata: Option[String], // JSON形式
  activityOccurredAt: Timestamp
)


/**
 * ユーザーセッションモデル
 * 
 * ログイン中のユーザーのセッション情報を表します。
 */
case class UserSession(
  sessionId: String, // UUID
  userId: Long,
  jwtTokenValue: String,
  sessionCreatedAt: Timestamp,
  sessionExpiresAt: Timestamp,
  sessionLastAccessedAt: Timestamp,
  userIpAddress: Option[String],
  userAgentString: Option[String]
)

/**
 * ログインレスポンス
 * 
 * ログイン成功時にクライアントに返すデータ
 */
case class LoginResponse(
  jwtToken: String,
  userInfo: UserResponse,
  sessionExpiresAt: Timestamp
)

/**
 * JWTペイロード
 * 
 * JWTトークンに含まれる情報
 */
case class JwtPayload(
  userId: Long,
  userEmail: String,
  issuedAtTimestamp: Long,
  expiresAtTimestamp: Long
)


/**
 * タスク検索リクエスト
 * 
 * タスクを検索する際の条件
 */
case class TaskSearchRequest(
  searchQueryText: Option[String] = None,
  assignedUserId: Option[Long] = None,
  projectId: Option[Long] = None,
  currentTaskStatus: Option[String] = None,
  priorityLevel: Option[Int] = None,
  dueDateBefore: Option[Timestamp] = None,
  dueDateAfter: Option[Timestamp] = None,
  tagIds: Option[List[Long]] = None
)

/**
 * ページネーション用のリクエスト
 */
case class PaginationRequest(
  pageNumber: Int = 1,
  itemsPerPage: Int = 20
)

/**
 * ページネーション用のレスポンス
 */
case class PaginatedResponse[T](
  items: List[T],
  totalItemCount: Long,
  currentPageNumber: Int,
  totalPageCount: Int,
  itemsPerPage: Int
)


/**
 * エラーレスポンス
 * 
 * エラーが発生した際にクライアントに返すデータ
 */
case class ErrorResponse(
  errorCode: String,
  errorMessage: String,
  errorDetails: Option[String] = None,
  errorOccurredAt: Timestamp = Timestamp.from(Instant.now())
)
