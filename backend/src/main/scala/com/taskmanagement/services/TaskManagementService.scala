package com.taskmanagement.services

import com.taskmanagement.models._
import com.taskmanagement.db.DatabaseSchema._
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{Future, ExecutionContext}
import java.sql.Timestamp
import java.time.Instant

/**
 * タスク管理サービス
 * 
 * タスクの作成、更新、削除、検索などの処理を担当します。
 * これがこのアプリケーションの中核となるサービスです。
 * 
 * 初心者向け解説:
 * このサービスは、Asanaの最も重要な機能である「タスク管理」を実装しています。
 * タスクとは、「見積書を作成する」「会議資料を準備する」といった具体的な作業のことです。
 * 
 * 主な機能:
 * - タスクの作成（誰が、何を、いつまでにやるかを記録）
 * - タスクの更新（担当者変更、期限変更、ステータス変更など）
 * - タスクの完了（タスクを完了としてマーク）
 * - タスクの検索（条件に合うタスクを探す）
 * - サブタスクの管理（大きなタスクを小さなタスクに分割）
 * 
 * @param database データベース接続
 * @param executionContext 非同期処理用のコンテキスト
 */
class TaskManagementService(
  database: Database
)(implicit executionContext: ExecutionContext) {

  /**
   * タスクを作成する
   * 
   * 新しいタスクをプロジェクトに追加します。
   * 
   * @param creationRequest タスク作成リクエスト
   * @param creatorUserId タスクを作成するユーザーのID
   * @return 作成されたタスク（作成失敗時はLeft(エラーメッセージ)）
   * 
   * 処理の流れ:
   * 1. プロジェクトの存在確認
   * 2. ユーザーがプロジェクトのメンバーかどうか確認
   * 3. タスク情報をデータベースに保存
   * 4. アクティビティログを記録
   * 5. 担当者に通知を送信（担当者が設定されている場合）
   * 
   * 使用例:
   * val request = TaskCreationRequest(
   *   taskTitle = "見積書を作成する",
   *   taskDescription = Some("来週の会議用の見積書を作成"),
   *   parentProjectId = 1,
   *   parentSectionId = Some(2),
   *   assignedUserId = Some(3),
   *   priorityLevel = 3
   * )
   * taskService.createNewTask(request, creatorUserId = 1).map {
   *   case Right(task) => println(s"タスク作成成功: ${task.taskTitle}")
   *   case Left(error) => println(s"タスク作成失敗: $error")
   * }
   */
  def createNewTask(
    creationRequest: TaskCreationRequest,
    creatorUserId: Long
  ): Future[Either[String, Task]] = {
    val currentTimestamp = Timestamp.from(Instant.now())

    val checkProjectQuery = projectsTable
      .filter(_.projectId === creationRequest.parentProjectId)
      .filter(_.projectDeletedAt.isEmpty)
      .result
      .headOption

    database.run(checkProjectQuery).flatMap {
      case None =>
        Future.successful(Left("指定されたプロジェクトが見つかりません"))

      case Some(project) =>
        val checkMembershipQuery = projectMembersTable
          .filter(_.projectId === creationRequest.parentProjectId)
          .filter(_.memberUserId === creatorUserId)
          .filter(_.memberLeftProjectAt.isEmpty)
          .result
          .headOption

        database.run(checkMembershipQuery).flatMap {
          case None =>
            Future.successful(Left("このプロジェクトにタスクを作成する権限がありません"))

          case Some(_) =>
            val newTask = Task(
              taskId = 0L, // AutoIncなので0を指定
              taskTitle = creationRequest.taskTitle,
              taskDescription = creationRequest.taskDescription,
              parentProjectId = creationRequest.parentProjectId,
              parentSectionId = creationRequest.parentSectionId,
              parentTaskId = creationRequest.parentTaskId,
              assignedUserId = creationRequest.assignedUserId,
              taskDueDateTime = creationRequest.taskDueDateTime,
              taskStartDate = None,
              priorityLevel = creationRequest.priorityLevel,
              currentTaskStatus = "not_started",
              isTaskCompleted = false,
              taskCompletedAt = None,
              taskCompletedByUserId = None,
              taskDisplayOrder = 0,
              taskCreatedAt = currentTimestamp,
              taskCreatedByUserId = creatorUserId,
              taskUpdatedAt = currentTimestamp,
              taskUpdatedByUserId = Some(creatorUserId),
              taskDeletedAt = None
            )

            val insertTaskAction = (tasksTable returning tasksTable.map(_.taskId)) += newTask

            database.run(insertTaskAction).flatMap { insertedTaskId =>
              val activityLog = ActivityLog(
                activityLogId = 0L,
                actorUserId = creatorUserId,
                activityType = "task_created",
                relatedTaskId = Some(insertedTaskId),
                relatedProjectId = Some(creationRequest.parentProjectId),
                relatedWorkspaceId = Some(project.parentWorkspaceId),
                activityDescription = s"タスク「${creationRequest.taskTitle}」を作成しました",
                activityMetadata = None,
                activityOccurredAt = currentTimestamp
              )

              val insertActivityLogAction = activityLogsTable += activityLog

              val notificationAction = creationRequest.assignedUserId match {
                case Some(assigneeId) if assigneeId != creatorUserId =>
                  val notification = Notification(
                    notificationId = 0L,
                    recipientUserId = assigneeId,
                    notificationType = "task_assigned",
                    relatedTaskId = Some(insertedTaskId),
                    relatedProjectId = Some(creationRequest.parentProjectId),
                    relatedCommentId = None,
                    notificationMessage = s"新しいタスク「${creationRequest.taskTitle}」が割り当てられました",
                    isNotificationRead = false,
                    notificationCreatedAt = currentTimestamp,
                    notificationReadAt = None
                  )
                  notificationsTable += notification
                case _ =>
                  DBIO.successful(0)
              }

              val combinedAction = for {
                _ <- insertActivityLogAction
                _ <- notificationAction
              } yield ()

              database.run(combinedAction.transactionally).map { _ =>
                Right(newTask.copy(taskId = insertedTaskId))
              }
            }.recover {
              case ex: Exception =>
                Left(s"タスク作成中にエラーが発生しました: ${ex.getMessage}")
            }
        }
    }
  }

  /**
   * タスクを更新する
   * 
   * 既存のタスクの情報を更新します。
   * 
   * @param taskId 更新するタスクのID
   * @param updateRequest タスク更新リクエスト
   * @param updaterUserId 更新を行うユーザーのID
   * @return 更新されたタスク（更新失敗時はLeft(エラーメッセージ)）
   * 
   * 処理の流れ:
   * 1. タスクの存在確認
   * 2. ユーザーがタスクを更新する権限があるか確認
   * 3. タスク情報を更新
   * 4. アクティビティログを記録
   * 5. 変更内容に応じて通知を送信
   * 
   * 使用例:
   * val request = TaskUpdateRequest(
   *   taskTitle = Some("見積書を作成する（修正版）"),
   *   currentTaskStatus = Some("in_progress"),
   *   priorityLevel = Some(4)
   * )
   * taskService.updateExistingTask(taskId = 123, request, updaterUserId = 1).map {
   *   case Right(task) => println("タスク更新成功")
   *   case Left(error) => println(s"タスク更新失敗: $error")
   * }
   */
  def updateExistingTask(
    taskId: Long,
    updateRequest: TaskUpdateRequest,
    updaterUserId: Long
  ): Future[Either[String, Task]] = {
    val currentTimestamp = Timestamp.from(Instant.now())

    val findTaskQuery = tasksTable
      .filter(_.taskId === taskId)
      .filter(_.taskDeletedAt.isEmpty)
      .result
      .headOption

    database.run(findTaskQuery).flatMap {
      case None =>
        Future.successful(Left("指定されたタスクが見つかりません"))

      case Some(existingTask) =>
        val checkMembershipQuery = projectMembersTable
          .filter(_.projectId === existingTask.parentProjectId)
          .filter(_.memberUserId === updaterUserId)
          .filter(_.memberLeftProjectAt.isEmpty)
          .result
          .headOption

        database.run(checkMembershipQuery).flatMap {
          case None =>
            Future.successful(Left("このタスクを更新する権限がありません"))

          case Some(_) =>
            val updatedTask = existingTask.copy(
              taskTitle = updateRequest.taskTitle.getOrElse(existingTask.taskTitle),
              taskDescription = updateRequest.taskDescription.orElse(existingTask.taskDescription),
              parentSectionId = updateRequest.parentSectionId.orElse(existingTask.parentSectionId),
              assignedUserId = updateRequest.assignedUserId.orElse(existingTask.assignedUserId),
              taskDueDateTime = updateRequest.taskDueDateTime.orElse(existingTask.taskDueDateTime),
              priorityLevel = updateRequest.priorityLevel.getOrElse(existingTask.priorityLevel),
              currentTaskStatus = updateRequest.currentTaskStatus.getOrElse(existingTask.currentTaskStatus),
              taskUpdatedAt = currentTimestamp,
              taskUpdatedByUserId = Some(updaterUserId)
            )

            val updateTaskAction = tasksTable
              .filter(_.taskId === taskId)
              .update(updatedTask)

            val changedFields = scala.collection.mutable.ListBuffer[String]()
            if (updateRequest.taskTitle.isDefined) changedFields += "タイトル"
            if (updateRequest.assignedUserId.isDefined) changedFields += "担当者"
            if (updateRequest.taskDueDateTime.isDefined) changedFields += "期限"
            if (updateRequest.priorityLevel.isDefined) changedFields += "優先度"
            if (updateRequest.currentTaskStatus.isDefined) changedFields += "ステータス"

            val activityDescription = if (changedFields.nonEmpty) {
              s"タスク「${existingTask.taskTitle}」の${changedFields.mkString("、")}を変更しました"
            } else {
              s"タスク「${existingTask.taskTitle}」を更新しました"
            }

            val activityLog = ActivityLog(
              activityLogId = 0L,
              actorUserId = updaterUserId,
              activityType = "task_updated",
              relatedTaskId = Some(taskId),
              relatedProjectId = Some(existingTask.parentProjectId),
              relatedWorkspaceId = None,
              activityDescription = activityDescription,
              activityMetadata = None,
              activityOccurredAt = currentTimestamp
            )

            val insertActivityLogAction = activityLogsTable += activityLog

            val notificationAction = updateRequest.assignedUserId match {
              case Some(newAssigneeId) if newAssigneeId != existingTask.assignedUserId.getOrElse(0L) && newAssigneeId != updaterUserId =>
                val notification = Notification(
                  notificationId = 0L,
                  recipientUserId = newAssigneeId,
                  notificationType = "task_assigned",
                  relatedTaskId = Some(taskId),
                  relatedProjectId = Some(existingTask.parentProjectId),
                  relatedCommentId = None,
                  notificationMessage = s"タスク「${existingTask.taskTitle}」が割り当てられました",
                  isNotificationRead = false,
                  notificationCreatedAt = currentTimestamp,
                  notificationReadAt = None
                )
                notificationsTable += notification
              case _ =>
                DBIO.successful(0)
            }

            val combinedAction = for {
              _ <- updateTaskAction
              _ <- insertActivityLogAction
              _ <- notificationAction
            } yield ()

            database.run(combinedAction.transactionally).map { _ =>
              Right(updatedTask)
            }.recover {
              case ex: Exception =>
                Left(s"タスク更新中にエラーが発生しました: ${ex.getMessage}")
            }
        }
    }
  }

  /**
   * タスクを完了する
   * 
   * タスクを完了状態にします。
   * 
   * @param taskId 完了するタスクのID
   * @param completedByUserId タスクを完了したユーザーのID
   * @return 完了したタスク（失敗時はLeft(エラーメッセージ)）
   */
  def completeTask(
    taskId: Long,
    completedByUserId: Long
  ): Future[Either[String, Task]] = {
    val currentTimestamp = Timestamp.from(Instant.now())

    val findTaskQuery = tasksTable
      .filter(_.taskId === taskId)
      .filter(_.taskDeletedAt.isEmpty)
      .result
      .headOption

    database.run(findTaskQuery).flatMap {
      case None =>
        Future.successful(Left("指定されたタスクが見つかりません"))

      case Some(existingTask) =>
        if (existingTask.isTaskCompleted) {
          Future.successful(Left("このタスクは既に完了しています"))
        } else {
          val completedTask = existingTask.copy(
            currentTaskStatus = "completed",
            isTaskCompleted = true,
            taskCompletedAt = Some(currentTimestamp),
            taskCompletedByUserId = Some(completedByUserId),
            taskUpdatedAt = currentTimestamp,
            taskUpdatedByUserId = Some(completedByUserId)
          )

          val updateTaskAction = tasksTable
            .filter(_.taskId === taskId)
            .update(completedTask)

          val activityLog = ActivityLog(
            activityLogId = 0L,
            actorUserId = completedByUserId,
            activityType = "task_completed",
            relatedTaskId = Some(taskId),
            relatedProjectId = Some(existingTask.parentProjectId),
            relatedWorkspaceId = None,
            activityDescription = s"タスク「${existingTask.taskTitle}」を完了しました",
            activityMetadata = None,
            activityOccurredAt = currentTimestamp
          )

          val insertActivityLogAction = activityLogsTable += activityLog

          val findDependentTasksQuery = taskDependenciesTable
            .filter(_.prerequisiteTaskId === taskId)
            .join(tasksTable).on(_.dependentTaskId === _.taskId)
            .filter(_._2.taskDeletedAt.isEmpty)
            .filter(_._2.isTaskCompleted === false)
            .map(_._2)
            .result

          database.run(findDependentTasksQuery).flatMap { dependentTasks =>
            val notificationActions = dependentTasks.flatMap { dependentTask =>
              dependentTask.assignedUserId.map { assigneeId =>
                val notification = Notification(
                  notificationId = 0L,
                  recipientUserId = assigneeId,
                  notificationType = "dependency_completed",
                  relatedTaskId = Some(dependentTask.taskId),
                  relatedProjectId = Some(dependentTask.parentProjectId),
                  relatedCommentId = None,
                  notificationMessage = s"タスク「${existingTask.taskTitle}」が完了したため、「${dependentTask.taskTitle}」を開始できます",
                  isNotificationRead = false,
                  notificationCreatedAt = currentTimestamp,
                  notificationReadAt = None
                )
                notificationsTable += notification
              }
            }

            val combinedAction = DBIO.sequence(
              updateTaskAction :: insertActivityLogAction :: notificationActions.toList
            )

            database.run(combinedAction.transactionally).map { _ =>
              Right(completedTask)
            }
          }.recover {
            case ex: Exception =>
              Left(s"タスク完了処理中にエラーが発生しました: ${ex.getMessage}")
          }
        }
    }
  }

  /**
   * タスクを削除する（論理削除）
   * 
   * タスクを削除します。実際にはデータベースから削除せず、削除フラグを立てます。
   * 
   * @param taskId 削除するタスクのID
   * @param deleterUserId 削除を行うユーザーのID
   * @return 削除成功時はRight(true)、失敗時はLeft(エラーメッセージ)
   */
  def deleteTask(
    taskId: Long,
    deleterUserId: Long
  ): Future[Either[String, Boolean]] = {
    val currentTimestamp = Timestamp.from(Instant.now())

    val findTaskQuery = tasksTable
      .filter(_.taskId === taskId)
      .filter(_.taskDeletedAt.isEmpty)
      .result
      .headOption

    database.run(findTaskQuery).flatMap {
      case None =>
        Future.successful(Left("指定されたタスクが見つかりません"))

      case Some(existingTask) =>
        val checkMembershipQuery = projectMembersTable
          .filter(_.projectId === existingTask.parentProjectId)
          .filter(_.memberUserId === deleterUserId)
          .filter(_.memberLeftProjectAt.isEmpty)
          .result
          .headOption

        database.run(checkMembershipQuery).flatMap {
          case None =>
            Future.successful(Left("このタスクを削除する権限がありません"))

          case Some(_) =>
            val deleteTaskAction = tasksTable
              .filter(_.taskId === taskId)
              .map(_.taskDeletedAt)
              .update(Some(currentTimestamp))

            val activityLog = ActivityLog(
              activityLogId = 0L,
              actorUserId = deleterUserId,
              activityType = "task_deleted",
              relatedTaskId = Some(taskId),
              relatedProjectId = Some(existingTask.parentProjectId),
              relatedWorkspaceId = None,
              activityDescription = s"タスク「${existingTask.taskTitle}」を削除しました",
              activityMetadata = None,
              activityOccurredAt = currentTimestamp
            )

            val insertActivityLogAction = activityLogsTable += activityLog

            val combinedAction = for {
              _ <- deleteTaskAction
              _ <- insertActivityLogAction
            } yield ()

            database.run(combinedAction.transactionally).map { _ =>
              Right(true)
            }.recover {
              case ex: Exception =>
                Left(s"タスク削除中にエラーが発生しました: ${ex.getMessage}")
            }
        }
    }
  }

  /**
   * タスクを検索する
   * 
   * 指定された条件に合うタスクを検索します。
   * 
   * @param searchRequest 検索条件
   * @return 検索結果のタスクリスト
   */
  def searchTasks(searchRequest: TaskSearchRequest): Future[List[Task]] = {
    var query = tasksTable.filter(_.taskDeletedAt.isEmpty)

    searchRequest.searchQueryText.foreach { queryText =>
      query = query.filter(_.taskTitle.like(s"%$queryText%"))
    }

    searchRequest.assignedUserId.foreach { userId =>
      query = query.filter(_.assignedUserId === userId)
    }

    searchRequest.projectId.foreach { projectId =>
      query = query.filter(_.parentProjectId === projectId)
    }

    searchRequest.currentTaskStatus.foreach { status =>
      query = query.filter(_.currentTaskStatus === status)
    }

    searchRequest.priorityLevel.foreach { priority =>
      query = query.filter(_.priorityLevel === priority)
    }

    searchRequest.dueDateBefore.foreach { beforeDate =>
      query = query.filter(_.taskDueDateTime < beforeDate)
    }

    searchRequest.dueDateAfter.foreach { afterDate =>
      query = query.filter(_.taskDueDateTime > afterDate)
    }

    database.run(query.result).map(_.toList)
  }

  /**
   * プロジェクトの全タスクを取得する
   * 
   * @param projectId プロジェクトID
   * @return タスクリスト
   */
  def getAllTasksInProject(projectId: Long): Future[List[Task]] = {
    val query = tasksTable
      .filter(_.parentProjectId === projectId)
      .filter(_.taskDeletedAt.isEmpty)
      .sortBy(_.taskDisplayOrder.asc)
      .result

    database.run(query).map(_.toList)
  }

  /**
   * ユーザーに割り当てられたタスクを取得する
   * 
   * @param userId ユーザーID
   * @return タスクリスト
   */
  def getTasksAssignedToUser(userId: Long): Future[List[Task]] = {
    val query = tasksTable
      .filter(_.assignedUserId === userId)
      .filter(_.taskDeletedAt.isEmpty)
      .filter(_.isTaskCompleted === false)
      .sortBy(_.taskDueDateTime.asc.nullsLast)
      .result

    database.run(query).map(_.toList)
  }
}
