package com.taskmanagement.services

import com.taskmanagement.models._
import com.taskmanagement.db.DatabaseSchema._
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{Future, ExecutionContext}
import java.sql.Timestamp
import java.time.Instant

/**
 * プロジェクト管理サービス
 * 
 * プロジェクトの作成、更新、削除、メンバー管理などの処理を担当します。
 * 
 * 初心者向け解説:
 * プロジェクトとは、複数のタスクをまとめた単位です。
 * 例えば、「Webサイトリニューアル」というプロジェクトの中に、
 * 「デザイン作成」「コーディング」「テスト」といったタスクがあります。
 * 
 * @param database データベース接続
 * @param executionContext 非同期処理用のコンテキスト
 */
class ProjectManagementService(
  database: Database
)(implicit executionContext: ExecutionContext) {

  /**
   * プロジェクトを作成する
   * 
   * @param creationRequest プロジェクト作成リクエスト
   * @param creatorUserId プロジェクトを作成するユーザーのID
   * @return 作成されたプロジェクト
   */
  def createNewProject(
    creationRequest: ProjectCreationRequest,
    creatorUserId: Long
  ): Future[Either[String, Project]] = {
    val currentTimestamp = Timestamp.from(Instant.now())

    val checkWorkspaceQuery = workspacesTable
      .filter(_.workspaceId === creationRequest.parentWorkspaceId)
      .filter(_.workspaceDeletedAt.isEmpty)
      .result
      .headOption

    database.run(checkWorkspaceQuery).flatMap {
      case None =>
        Future.successful(Left("指定されたワークスペースが見つかりません"))

      case Some(workspace) =>
        val checkMembershipQuery = workspaceMembersTable
          .filter(_.workspaceId === creationRequest.parentWorkspaceId)
          .filter(_.memberUserId === creatorUserId)
          .filter(_.memberLeftWorkspaceAt.isEmpty)
          .result
          .headOption

        database.run(checkMembershipQuery).flatMap {
          case None =>
            Future.successful(Left("このワークスペースにプロジェクトを作成する権限がありません"))

          case Some(_) =>
            val newProject = Project(
              projectId = 0L,
              projectName = creationRequest.projectName,
              projectDescription = creationRequest.projectDescription,
              parentWorkspaceId = creationRequest.parentWorkspaceId,
              projectOwnerUserId = creatorUserId,
              projectColorCode = creationRequest.projectColorCode.getOrElse("#4A90E2"),
              projectIconName = None,
              projectStartDate = creationRequest.projectStartDate,
              projectDueDate = creationRequest.projectDueDate,
              projectCurrentStatus = "on_track",
              projectCreatedAt = currentTimestamp,
              projectUpdatedAt = currentTimestamp,
              isProjectArchived = false,
              projectArchivedAt = None,
              projectDeletedAt = None
            )

            val insertProjectAction = (projectsTable returning projectsTable.map(_.projectId)) += newProject

            database.run(insertProjectAction).flatMap { insertedProjectId =>
              val projectMember = ProjectMember(
                projectMemberId = 0L,
                projectId = insertedProjectId,
                memberUserId = creatorUserId,
                memberRoleInProject = "owner",
                memberJoinedProjectAt = currentTimestamp,
                memberLeftProjectAt = None
              )

              val insertMemberAction = projectMembersTable += projectMember

              val defaultSections = List(
                Section(0L, "未着手", insertedProjectId, 1, currentTimestamp, None),
                Section(0L, "進行中", insertedProjectId, 2, currentTimestamp, None),
                Section(0L, "完了", insertedProjectId, 3, currentTimestamp, None)
              )

              val insertSectionsAction = sectionsTable ++= defaultSections

              val activityLog = ActivityLog(
                activityLogId = 0L,
                actorUserId = creatorUserId,
                activityType = "project_created",
                relatedTaskId = None,
                relatedProjectId = Some(insertedProjectId),
                relatedWorkspaceId = Some(creationRequest.parentWorkspaceId),
                activityDescription = s"プロジェクト「${creationRequest.projectName}」を作成しました",
                activityMetadata = None,
                activityOccurredAt = currentTimestamp
              )

              val insertActivityLogAction = activityLogsTable += activityLog

              val combinedAction = for {
                _ <- insertMemberAction
                _ <- insertSectionsAction
                _ <- insertActivityLogAction
              } yield ()

              database.run(combinedAction.transactionally).map { _ =>
                Right(newProject.copy(projectId = insertedProjectId))
              }
            }.recover {
              case ex: Exception =>
                Left(s"プロジェクト作成中にエラーが発生しました: ${ex.getMessage}")
            }
        }
    }
  }

  /**
   * プロジェクトにメンバーを追加する
   * 
   * @param projectId プロジェクトID
   * @param newMemberUserId 追加するユーザーのID
   * @param memberRole メンバーの役割（"owner", "editor", "commenter", "viewer"）
   * @param adderUserId メンバーを追加するユーザーのID
   * @return 追加されたプロジェクトメンバー
   */
  def addMemberToProject(
    projectId: Long,
    newMemberUserId: Long,
    memberRole: String,
    adderUserId: Long
  ): Future[Either[String, ProjectMember]] = {
    val currentTimestamp = Timestamp.from(Instant.now())

    val checkProjectQuery = projectsTable
      .filter(_.projectId === projectId)
      .filter(_.projectDeletedAt.isEmpty)
      .result
      .headOption

    database.run(checkProjectQuery).flatMap {
      case None =>
        Future.successful(Left("指定されたプロジェクトが見つかりません"))

      case Some(project) =>
        val checkWorkspaceMembershipQuery = workspaceMembersTable
          .filter(_.workspaceId === project.parentWorkspaceId)
          .filter(_.memberUserId === newMemberUserId)
          .filter(_.memberLeftWorkspaceAt.isEmpty)
          .result
          .headOption

        database.run(checkWorkspaceMembershipQuery).flatMap {
          case None =>
            Future.successful(Left("追加しようとしているユーザーはワークスペースのメンバーではありません"))

          case Some(_) =>
            val checkExistingMemberQuery = projectMembersTable
              .filter(_.projectId === projectId)
              .filter(_.memberUserId === newMemberUserId)
              .filter(_.memberLeftProjectAt.isEmpty)
              .result
              .headOption

            database.run(checkExistingMemberQuery).flatMap {
              case Some(_) =>
                Future.successful(Left("このユーザーは既にプロジェクトのメンバーです"))

              case None =>
                val newMember = ProjectMember(
                  projectMemberId = 0L,
                  projectId = projectId,
                  memberUserId = newMemberUserId,
                  memberRoleInProject = memberRole,
                  memberJoinedProjectAt = currentTimestamp,
                  memberLeftProjectAt = None
                )

                val insertMemberAction = (projectMembersTable returning projectMembersTable.map(_.projectMemberId)) += newMember

                database.run(insertMemberAction).flatMap { insertedMemberId =>
                  val notification = Notification(
                    notificationId = 0L,
                    recipientUserId = newMemberUserId,
                    notificationType = "member_added",
                    relatedTaskId = None,
                    relatedProjectId = Some(projectId),
                    relatedCommentId = None,
                    notificationMessage = s"プロジェクト「${project.projectName}」に追加されました",
                    isNotificationRead = false,
                    notificationCreatedAt = currentTimestamp,
                    notificationReadAt = None
                  )

                  val insertNotificationAction = notificationsTable += notification

                  database.run(insertNotificationAction).map { _ =>
                    Right(newMember.copy(projectMemberId = insertedMemberId))
                  }
                }.recover {
                  case ex: Exception =>
                    Left(s"メンバー追加中にエラーが発生しました: ${ex.getMessage}")
                }
            }
        }
    }
  }

  /**
   * ワークスペースの全プロジェクトを取得する
   * 
   * @param workspaceId ワークスペースID
   * @return プロジェクトリスト
   */
  def getAllProjectsInWorkspace(workspaceId: Long): Future[List[Project]] = {
    val query = projectsTable
      .filter(_.parentWorkspaceId === workspaceId)
      .filter(_.projectDeletedAt.isEmpty)
      .filter(_.isProjectArchived === false)
      .sortBy(_.projectCreatedAt.desc)
      .result

    database.run(query).map(_.toList)
  }

  /**
   * ユーザーが参加しているプロジェクトを取得する
   * 
   * @param userId ユーザーID
   * @return プロジェクトリスト
   */
  def getProjectsForUser(userId: Long): Future[List[Project]] = {
    val query = projectMembersTable
      .filter(_.memberUserId === userId)
      .filter(_.memberLeftProjectAt.isEmpty)
      .join(projectsTable).on(_.projectId === _.projectId)
      .filter(_._2.projectDeletedAt.isEmpty)
      .filter(_._2.isProjectArchived === false)
      .map(_._2)
      .sortBy(_.projectUpdatedAt.desc)
      .result

    database.run(query).map(_.toList)
  }
}
