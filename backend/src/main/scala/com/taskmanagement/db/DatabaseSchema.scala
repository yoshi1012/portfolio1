package com.taskmanagement.db

import com.taskmanagement.models._
import slick.jdbc.PostgresProfile.api._
import java.sql.Timestamp
import java.time.LocalDate

/**
 * データベーススキーマ定義
 * 
 * このファイルでは、Slick ORM を使ってデータベースのテーブルを定義しています。
 * 
 * 初心者向け解説:
 * ORM (Object-Relational Mapping) とは、データベースのテーブルとScalaのクラスを
 * 対応付ける仕組みです。これにより、SQLを直接書かなくても、Scalaのコードで
 * データベース操作ができます。
 * 
 * 例:
 * - SQLで書く場合: SELECT * FROM users WHERE user_id = 123
 * - Slickで書く場合: usersTable.filter(_.userId === 123).result
 * 
 * メリット:
 * - 型安全（コンパイル時にエラーを検出できる）
 * - SQLインジェクション攻撃を防げる
 * - データベースの種類を変更しやすい
 */

/**
 * ユーザーテーブル定義
 */
class UsersTable(tag: Tag) extends Table[User](tag, "users") {
  def userId = column[Long]("user_id", O.PrimaryKey, O.AutoInc)
  def userEmail = column[String]("user_email")
  def userFullName = column[String]("user_full_name")
  def userDisplayName = column[Option[String]]("user_display_name")
  def hashedPasswordValue = column[String]("hashed_password_value")
  def userProfileImageUrl = column[Option[String]]("user_profile_image_url")
  def userTimezone = column[String]("user_timezone")
  def userLanguagePreference = column[String]("user_language_preference")
  def accountCreatedAt = column[Timestamp]("account_created_at")
  def accountLastLoginAt = column[Option[Timestamp]]("account_last_login_at")
  def isAccountActive = column[Boolean]("is_account_active")
  def accountDeletedAt = column[Option[Timestamp]]("account_deleted_at")

  def * = (userId, userEmail, userFullName, userDisplayName, hashedPasswordValue,
    userProfileImageUrl, userTimezone, userLanguagePreference, accountCreatedAt,
    accountLastLoginAt, isAccountActive, accountDeletedAt) <> (User.tupled, User.unapply)
}

/**
 * 組織テーブル定義
 */
class OrganizationsTable(tag: Tag) extends Table[Organization](tag, "organizations") {
  def organizationId = column[Long]("organization_id", O.PrimaryKey, O.AutoInc)
  def organizationName = column[String]("organization_name")
  def organizationDescription = column[Option[String]]("organization_description")
  def organizationCreatedAt = column[Timestamp]("organization_created_at")
  def organizationOwnerUserId = column[Long]("organization_owner_user_id")
  def isOrganizationActive = column[Boolean]("is_organization_active")
  def organizationDeletedAt = column[Option[Timestamp]]("organization_deleted_at")

  def * = (organizationId, organizationName, organizationDescription, organizationCreatedAt,
    organizationOwnerUserId, isOrganizationActive, organizationDeletedAt) <> (Organization.tupled, Organization.unapply)
}

/**
 * 組織メンバーテーブル定義
 */
class OrganizationMembersTable(tag: Tag) extends Table[OrganizationMember](tag, "organization_members") {
  def organizationMemberId = column[Long]("organization_member_id", O.PrimaryKey, O.AutoInc)
  def organizationId = column[Long]("organization_id")
  def memberUserId = column[Long]("member_user_id")
  def memberRoleInOrganization = column[String]("member_role_in_organization")
  def memberJoinedOrganizationAt = column[Timestamp]("member_joined_organization_at")
  def memberLeftOrganizationAt = column[Option[Timestamp]]("member_left_organization_at")

  def * = (organizationMemberId, organizationId, memberUserId, memberRoleInOrganization,
    memberJoinedOrganizationAt, memberLeftOrganizationAt) <> (OrganizationMember.tupled, OrganizationMember.unapply)
}

/**
 * ワークスペーステーブル定義
 */
class WorkspacesTable(tag: Tag) extends Table[Workspace](tag, "workspaces") {
  def workspaceId = column[Long]("workspace_id", O.PrimaryKey, O.AutoInc)
  def workspaceName = column[String]("workspace_name")
  def workspaceDescription = column[Option[String]]("workspace_description")
  def parentOrganizationId = column[Long]("parent_organization_id")
  def workspaceCreatedAt = column[Timestamp]("workspace_created_at")
  def workspaceCreatorUserId = column[Long]("workspace_creator_user_id")
  def isWorkspaceActive = column[Boolean]("is_workspace_active")
  def workspaceDeletedAt = column[Option[Timestamp]]("workspace_deleted_at")

  def * = (workspaceId, workspaceName, workspaceDescription, parentOrganizationId,
    workspaceCreatedAt, workspaceCreatorUserId, isWorkspaceActive, workspaceDeletedAt) <> (Workspace.tupled, Workspace.unapply)
}

/**
 * ワークスペースメンバーテーブル定義
 */
class WorkspaceMembersTable(tag: Tag) extends Table[WorkspaceMember](tag, "workspace_members") {
  def workspaceMemberId = column[Long]("workspace_member_id", O.PrimaryKey, O.AutoInc)
  def workspaceId = column[Long]("workspace_id")
  def memberUserId = column[Long]("member_user_id")
  def memberRoleInWorkspace = column[String]("member_role_in_workspace")
  def memberJoinedWorkspaceAt = column[Timestamp]("member_joined_workspace_at")
  def memberLeftWorkspaceAt = column[Option[Timestamp]]("member_left_workspace_at")

  def * = (workspaceMemberId, workspaceId, memberUserId, memberRoleInWorkspace,
    memberJoinedWorkspaceAt, memberLeftWorkspaceAt) <> (WorkspaceMember.tupled, WorkspaceMember.unapply)
}

/**
 * プロジェクトテーブル定義
 */
class ProjectsTable(tag: Tag) extends Table[Project](tag, "projects") {
  def projectId = column[Long]("project_id", O.PrimaryKey, O.AutoInc)
  def projectName = column[String]("project_name")
  def projectDescription = column[Option[String]]("project_description")
  def parentWorkspaceId = column[Long]("parent_workspace_id")
  def projectOwnerUserId = column[Long]("project_owner_user_id")
  def projectColorCode = column[String]("project_color_code")
  def projectIconName = column[Option[String]]("project_icon_name")
  def projectStartDate = column[Option[LocalDate]]("project_start_date")
  def projectDueDate = column[Option[LocalDate]]("project_due_date")
  def projectCurrentStatus = column[String]("project_current_status")
  def projectCreatedAt = column[Timestamp]("project_created_at")
  def projectUpdatedAt = column[Timestamp]("project_updated_at")
  def isProjectArchived = column[Boolean]("is_project_archived")
  def projectArchivedAt = column[Option[Timestamp]]("project_archived_at")
  def projectDeletedAt = column[Option[Timestamp]]("project_deleted_at")

  def * = (projectId, projectName, projectDescription, parentWorkspaceId, projectOwnerUserId,
    projectColorCode, projectIconName, projectStartDate, projectDueDate, projectCurrentStatus,
    projectCreatedAt, projectUpdatedAt, isProjectArchived, projectArchivedAt, projectDeletedAt) <> (Project.tupled, Project.unapply)
}

/**
 * プロジェクトメンバーテーブル定義
 */
class ProjectMembersTable(tag: Tag) extends Table[ProjectMember](tag, "project_members") {
  def projectMemberId = column[Long]("project_member_id", O.PrimaryKey, O.AutoInc)
  def projectId = column[Long]("project_id")
  def memberUserId = column[Long]("member_user_id")
  def memberRoleInProject = column[String]("member_role_in_project")
  def memberJoinedProjectAt = column[Timestamp]("member_joined_project_at")
  def memberLeftProjectAt = column[Option[Timestamp]]("member_left_project_at")

  def * = (projectMemberId, projectId, memberUserId, memberRoleInProject,
    memberJoinedProjectAt, memberLeftProjectAt) <> (ProjectMember.tupled, ProjectMember.unapply)
}

/**
 * セクションテーブル定義
 */
class SectionsTable(tag: Tag) extends Table[Section](tag, "sections") {
  def sectionId = column[Long]("section_id", O.PrimaryKey, O.AutoInc)
  def sectionName = column[String]("section_name")
  def parentProjectId = column[Long]("parent_project_id")
  def sectionDisplayOrder = column[Int]("section_display_order")
  def sectionCreatedAt = column[Timestamp]("section_created_at")
  def sectionDeletedAt = column[Option[Timestamp]]("section_deleted_at")

  def * = (sectionId, sectionName, parentProjectId, sectionDisplayOrder,
    sectionCreatedAt, sectionDeletedAt) <> (Section.tupled, Section.unapply)
}

/**
 * タスクテーブル定義
 */
class TasksTable(tag: Tag) extends Table[Task](tag, "tasks") {
  def taskId = column[Long]("task_id", O.PrimaryKey, O.AutoInc)
  def taskTitle = column[String]("task_title")
  def taskDescription = column[Option[String]]("task_description")
  def parentProjectId = column[Long]("parent_project_id")
  def parentSectionId = column[Option[Long]]("parent_section_id")
  def parentTaskId = column[Option[Long]]("parent_task_id")
  def assignedUserId = column[Option[Long]]("assigned_user_id")
  def taskDueDateTime = column[Option[Timestamp]]("task_due_date_time")
  def taskStartDate = column[Option[LocalDate]]("task_start_date")
  def priorityLevel = column[Int]("priority_level")
  def currentTaskStatus = column[String]("current_task_status")
  def isTaskCompleted = column[Boolean]("is_task_completed")
  def taskCompletedAt = column[Option[Timestamp]]("task_completed_at")
  def taskCompletedByUserId = column[Option[Long]]("task_completed_by_user_id")
  def taskDisplayOrder = column[Int]("task_display_order")
  def taskCreatedAt = column[Timestamp]("task_created_at")
  def taskCreatedByUserId = column[Long]("task_created_by_user_id")
  def taskUpdatedAt = column[Timestamp]("task_updated_at")
  def taskUpdatedByUserId = column[Option[Long]]("task_updated_by_user_id")
  def taskDeletedAt = column[Option[Timestamp]]("task_deleted_at")

  def * = (taskId, taskTitle, taskDescription, parentProjectId, parentSectionId, parentTaskId,
    assignedUserId, taskDueDateTime, taskStartDate, priorityLevel, currentTaskStatus,
    isTaskCompleted, taskCompletedAt, taskCompletedByUserId, taskDisplayOrder,
    taskCreatedAt, taskCreatedByUserId, taskUpdatedAt, taskUpdatedByUserId, taskDeletedAt) <> (Task.tupled, Task.unapply)
}

/**
 * タスク依存関係テーブル定義
 */
class TaskDependenciesTable(tag: Tag) extends Table[TaskDependency](tag, "task_dependencies") {
  def taskDependencyId = column[Long]("task_dependency_id", O.PrimaryKey, O.AutoInc)
  def dependentTaskId = column[Long]("dependent_task_id")
  def prerequisiteTaskId = column[Long]("prerequisite_task_id")
  def dependencyType = column[String]("dependency_type")
  def dependencyCreatedAt = column[Timestamp]("dependency_created_at")

  def * = (taskDependencyId, dependentTaskId, prerequisiteTaskId, dependencyType,
    dependencyCreatedAt) <> (TaskDependency.tupled, TaskDependency.unapply)
}

/**
 * タグテーブル定義
 */
class TagsTable(tag: Tag) extends Table[Tag](tag, "tags") {
  def tagId = column[Long]("tag_id", O.PrimaryKey, O.AutoInc)
  def tagName = column[String]("tag_name")
  def tagColorCode = column[String]("tag_color_code")
  def parentWorkspaceId = column[Long]("parent_workspace_id")
  def tagCreatedAt = column[Timestamp]("tag_created_at")
  def tagDeletedAt = column[Option[Timestamp]]("tag_deleted_at")

  def * = (tagId, tagName, tagColorCode, parentWorkspaceId, tagCreatedAt, tagDeletedAt) <> (Tag.tupled, Tag.unapply)
}

/**
 * タスク・タグ関連テーブル定義
 */
class TaskTagsTable(tag: Tag) extends Table[TaskTag](tag, "task_tags") {
  def taskTagId = column[Long]("task_tag_id", O.PrimaryKey, O.AutoInc)
  def taskId = column[Long]("task_id")
  def tagId = column[Long]("tag_id")
  def tagAttachedAt = column[Timestamp]("tag_attached_at")

  def * = (taskTagId, taskId, tagId, tagAttachedAt) <> (TaskTag.tupled, TaskTag.unapply)
}

/**
 * コメントテーブル定義
 */
class CommentsTable(tag: Tag) extends Table[Comment](tag, "comments") {
  def commentId = column[Long]("comment_id", O.PrimaryKey, O.AutoInc)
  def parentTaskId = column[Long]("parent_task_id")
  def commentAuthorUserId = column[Long]("comment_author_user_id")
  def commentTextContent = column[String]("comment_text_content")
  def commentCreatedAt = column[Timestamp]("comment_created_at")
  def commentUpdatedAt = column[Option[Timestamp]]("comment_updated_at")
  def commentDeletedAt = column[Option[Timestamp]]("comment_deleted_at")

  def * = (commentId, parentTaskId, commentAuthorUserId, commentTextContent,
    commentCreatedAt, commentUpdatedAt, commentDeletedAt) <> (Comment.tupled, Comment.unapply)
}

/**
 * 添付ファイルテーブル定義
 */
class AttachmentsTable(tag: Tag) extends Table[Attachment](tag, "attachments") {
  def attachmentId = column[Long]("attachment_id", O.PrimaryKey, O.AutoInc)
  def parentTaskId = column[Long]("parent_task_id")
  def attachmentFileName = column[String]("attachment_file_name")
  def attachmentFileUrl = column[String]("attachment_file_url")
  def attachmentFileSizeBytes = column[Option[Long]]("attachment_file_size_bytes")
  def attachmentMimeType = column[Option[String]]("attachment_mime_type")
  def uploadedByUserId = column[Long]("uploaded_by_user_id")
  def attachmentUploadedAt = column[Timestamp]("attachment_uploaded_at")
  def attachmentDeletedAt = column[Option[Timestamp]]("attachment_deleted_at")

  def * = (attachmentId, parentTaskId, attachmentFileName, attachmentFileUrl,
    attachmentFileSizeBytes, attachmentMimeType, uploadedByUserId,
    attachmentUploadedAt, attachmentDeletedAt) <> (Attachment.tupled, Attachment.unapply)
}

/**
 * 通知テーブル定義
 */
class NotificationsTable(tag: Tag) extends Table[Notification](tag, "notifications") {
  def notificationId = column[Long]("notification_id", O.PrimaryKey, O.AutoInc)
  def recipientUserId = column[Long]("recipient_user_id")
  def notificationType = column[String]("notification_type")
  def relatedTaskId = column[Option[Long]]("related_task_id")
  def relatedProjectId = column[Option[Long]]("related_project_id")
  def relatedCommentId = column[Option[Long]]("related_comment_id")
  def notificationMessage = column[String]("notification_message")
  def isNotificationRead = column[Boolean]("is_notification_read")
  def notificationCreatedAt = column[Timestamp]("notification_created_at")
  def notificationReadAt = column[Option[Timestamp]]("notification_read_at")

  def * = (notificationId, recipientUserId, notificationType, relatedTaskId, relatedProjectId,
    relatedCommentId, notificationMessage, isNotificationRead, notificationCreatedAt,
    notificationReadAt) <> (Notification.tupled, Notification.unapply)
}

/**
 * アクティビティログテーブル定義
 */
class ActivityLogsTable(tag: Tag) extends Table[ActivityLog](tag, "activity_logs") {
  def activityLogId = column[Long]("activity_log_id", O.PrimaryKey, O.AutoInc)
  def actorUserId = column[Long]("actor_user_id")
  def activityType = column[String]("activity_type")
  def relatedTaskId = column[Option[Long]]("related_task_id")
  def relatedProjectId = column[Option[Long]]("related_project_id")
  def relatedWorkspaceId = column[Option[Long]]("related_workspace_id")
  def activityDescription = column[String]("activity_description")
  def activityMetadata = column[Option[String]]("activity_metadata")
  def activityOccurredAt = column[Timestamp]("activity_occurred_at")

  def * = (activityLogId, actorUserId, activityType, relatedTaskId, relatedProjectId,
    relatedWorkspaceId, activityDescription, activityMetadata, activityOccurredAt) <> (ActivityLog.tupled, ActivityLog.unapply)
}

/**
 * ユーザーセッションテーブル定義
 */
class UserSessionsTable(tag: Tag) extends Table[UserSession](tag, "user_sessions") {
  def sessionId = column[String]("session_id", O.PrimaryKey)
  def userId = column[Long]("user_id")
  def jwtTokenValue = column[String]("jwt_token_value")
  def sessionCreatedAt = column[Timestamp]("session_created_at")
  def sessionExpiresAt = column[Timestamp]("session_expires_at")
  def sessionLastAccessedAt = column[Timestamp]("session_last_accessed_at")
  def userIpAddress = column[Option[String]]("user_ip_address")
  def userAgentString = column[Option[String]]("user_agent_string")

  def * = (sessionId, userId, jwtTokenValue, sessionCreatedAt, sessionExpiresAt,
    sessionLastAccessedAt, userIpAddress, userAgentString) <> (UserSession.tupled, UserSession.unapply)
}

/**
 * データベーススキーマオブジェクト
 * 
 * 全てのテーブルのインスタンスをここで定義します。
 * これらのインスタンスを使ってデータベース操作を行います。
 */
object DatabaseSchema {
  val usersTable = TableQuery[UsersTable]
  val organizationsTable = TableQuery[OrganizationsTable]
  val organizationMembersTable = TableQuery[OrganizationMembersTable]
  val workspacesTable = TableQuery[WorkspacesTable]
  val workspaceMembersTable = TableQuery[WorkspaceMembersTable]
  val projectsTable = TableQuery[ProjectsTable]
  val projectMembersTable = TableQuery[ProjectMembersTable]
  val sectionsTable = TableQuery[SectionsTable]
  val tasksTable = TableQuery[TasksTable]
  val taskDependenciesTable = TableQuery[TaskDependenciesTable]
  val tagsTable = TableQuery[TagsTable]
  val taskTagsTable = TableQuery[TaskTagsTable]
  val commentsTable = TableQuery[CommentsTable]
  val attachmentsTable = TableQuery[AttachmentsTable]
  val notificationsTable = TableQuery[NotificationsTable]
  val activityLogsTable = TableQuery[ActivityLogsTable]
  val userSessionsTable = TableQuery[UserSessionsTable]
}
