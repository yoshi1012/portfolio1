package com.taskmanagement.api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.taskmanagement.services._
import com.taskmanagement.models._
import com.taskmanagement.auth.JwtAuthenticationHandler
import scala.concurrent.ExecutionContext
import java.sql.Timestamp
import java.time.Instant

/**
 * APIルート定義
 * 
 * このファイルでは、全てのHTTPエンドポイント（URL）を定義しています。
 * 
 * 初心者向け解説:
 * Webアプリケーションでは、クライアント（ブラウザやモバイルアプリ）が
 * サーバーにHTTPリクエストを送信します。
 * 
 * 例えば:
 * - POST /api/auth/register → ユーザー登録
 * - POST /api/auth/login → ログイン
 * - GET /api/tasks → タスク一覧取得
 * - POST /api/tasks → タスク作成
 * 
 * このファイルでは、各URLに対してどの処理を実行するかを定義しています。
 * 
 * @param authService 認証サービス
 * @param taskService タスク管理サービス
 * @param projectService プロジェクト管理サービス
 * @param jwtHandler JWT認証ハンドラー
 * @param executionContext 非同期処理用のコンテキスト
 */
class Routes(
  authService: AuthenticationService,
  taskService: TaskManagementService,
  projectService: ProjectManagementService,
  jwtHandler: JwtAuthenticationHandler
)(implicit executionContext: ExecutionContext) extends JsonProtocols {

  /**
   * 認証が必要なエンドポイント用のディレクティブ
   * 
   * Authorizationヘッダーからトークンを取得し、検証します。
   * トークンが有効な場合、ユーザーIDを抽出して処理を続行します。
   * 
   * 使用例:
   * authenticateUser { userId =>
   *   // ここでuserIdを使った処理を実行
   * }
   */
  def authenticateUser: Directive1[Long] = {
    optionalHeaderValueByName("Authorization").flatMap {
      case Some(authHeader) =>
        jwtHandler.extractTokenFromAuthorizationHeader(authHeader) match {
          case Some(token) =>
            jwtHandler.validateTokenAndExtractPayload(token) match {
              case Some(payload) =>
                provide(payload.userId)
              case None =>
                complete(StatusCodes.Unauthorized, ErrorResponse(
                  errorCode = "INVALID_TOKEN",
                  errorMessage = "無効なトークンです"
                ))
            }
          case None =>
            complete(StatusCodes.Unauthorized, ErrorResponse(
              errorCode = "MISSING_TOKEN",
              errorMessage = "認証トークンが必要です"
            ))
        }
      case None =>
        complete(StatusCodes.Unauthorized, ErrorResponse(
          errorCode = "MISSING_AUTH_HEADER",
          errorMessage = "Authorizationヘッダーが必要です"
        ))
    }
  }

  /**
   * 認証関連のルート
   * 
   * ユーザー登録、ログイン、ログアウトなどのエンドポイント
   */
  val authRoutes: Route = pathPrefix("auth") {
    concat(
      path("register") {
        post {
          entity(as[UserRegistrationRequest]) { request =>
            onSuccess(authService.registerNewUser(request)) {
              case Right(response) =>
                complete(StatusCodes.Created, response)
              case Left(error) =>
                complete(StatusCodes.BadRequest, ErrorResponse(
                  errorCode = "REGISTRATION_FAILED",
                  errorMessage = error
                ))
            }
          }
        }
      },
      path("login") {
        post {
          entity(as[UserLoginRequest]) { request =>
            onSuccess(authService.authenticateUser(request)) {
              case Right(response) =>
                complete(StatusCodes.OK, response)
              case Left(error) =>
                complete(StatusCodes.Unauthorized, ErrorResponse(
                  errorCode = "LOGIN_FAILED",
                  errorMessage = error
                ))
            }
          }
        }
      },
      path("logout") {
        post {
          authenticateUser { userId =>
            optionalHeaderValueByName("Authorization").flatMap {
              case Some(authHeader) =>
                jwtHandler.extractTokenFromAuthorizationHeader(authHeader) match {
                  case Some(token) =>
                    onSuccess(authService.logoutUser(token)) {
                      case Right(_) =>
                        complete(StatusCodes.OK, Map("message" -> "ログアウトしました"))
                      case Left(error) =>
                        complete(StatusCodes.BadRequest, ErrorResponse(
                          errorCode = "LOGOUT_FAILED",
                          errorMessage = error
                        ))
                    }
                  case None =>
                    complete(StatusCodes.BadRequest, ErrorResponse(
                      errorCode = "INVALID_TOKEN",
                      errorMessage = "無効なトークンです"
                    ))
                }
              case None =>
                complete(StatusCodes.BadRequest, ErrorResponse(
                  errorCode = "MISSING_AUTH_HEADER",
                  errorMessage = "Authorizationヘッダーが必要です"
                ))
            }
          }
        }
      },
      path("me") {
        get {
          authenticateUser { userId =>
            optionalHeaderValueByName("Authorization").flatMap {
              case Some(authHeader) =>
                jwtHandler.extractTokenFromAuthorizationHeader(authHeader) match {
                  case Some(token) =>
                    onSuccess(authService.getUserFromToken(token)) {
                      case Some(user) =>
                        complete(StatusCodes.OK, user)
                      case None =>
                        complete(StatusCodes.NotFound, ErrorResponse(
                          errorCode = "USER_NOT_FOUND",
                          errorMessage = "ユーザーが見つかりません"
                        ))
                    }
                  case None =>
                    complete(StatusCodes.BadRequest, ErrorResponse(
                      errorCode = "INVALID_TOKEN",
                      errorMessage = "無効なトークンです"
                    ))
                }
              case None =>
                complete(StatusCodes.BadRequest, ErrorResponse(
                  errorCode = "MISSING_AUTH_HEADER",
                  errorMessage = "Authorizationヘッダーが必要です"
                ))
            }
          }
        }
      }
    )
  }

  /**
   * プロジェクト関連のルート
   */
  val projectRoutes: Route = pathPrefix("projects") {
    concat(
      pathEnd {
        post {
          authenticateUser { userId =>
            entity(as[ProjectCreationRequest]) { request =>
              onSuccess(projectService.createNewProject(request, userId)) {
                case Right(project) =>
                  complete(StatusCodes.Created, project)
                case Left(error) =>
                  complete(StatusCodes.BadRequest, ErrorResponse(
                    errorCode = "PROJECT_CREATION_FAILED",
                    errorMessage = error
                  ))
              }
            }
          }
        }
      },
      path("workspace" / LongNumber) { workspaceId =>
        get {
          authenticateUser { userId =>
            onSuccess(projectService.getAllProjectsInWorkspace(workspaceId)) { projects =>
              complete(StatusCodes.OK, projects)
            }
          }
        }
      },
      path("my") {
        get {
          authenticateUser { userId =>
            onSuccess(projectService.getProjectsForUser(userId)) { projects =>
              complete(StatusCodes.OK, projects)
            }
          }
        }
      },
      path(LongNumber / "members") { projectId =>
        post {
          authenticateUser { userId =>
            parameters("newMemberUserId".as[Long], "memberRole".as[String]) { (newMemberUserId, memberRole) =>
              onSuccess(projectService.addMemberToProject(projectId, newMemberUserId, memberRole, userId)) {
                case Right(member) =>
                  complete(StatusCodes.Created, member)
                case Left(error) =>
                  complete(StatusCodes.BadRequest, ErrorResponse(
                    errorCode = "ADD_MEMBER_FAILED",
                    errorMessage = error
                  ))
              }
            }
          }
        }
      }
    )
  }

  /**
   * タスク関連のルート
   */
  val taskRoutes: Route = pathPrefix("tasks") {
    concat(
      pathEnd {
        post {
          authenticateUser { userId =>
            entity(as[TaskCreationRequest]) { request =>
              onSuccess(taskService.createNewTask(request, userId)) {
                case Right(task) =>
                  complete(StatusCodes.Created, task)
                case Left(error) =>
                  complete(StatusCodes.BadRequest, ErrorResponse(
                    errorCode = "TASK_CREATION_FAILED",
                    errorMessage = error
                  ))
              }
            }
          }
        } ~
        get {
          authenticateUser { userId =>
            parameters(
              "query".optional,
              "assignedUserId".as[Long].optional,
              "projectId".as[Long].optional,
              "status".optional,
              "priority".as[Int].optional
            ) { (query, assignedUserId, projectId, status, priority) =>
              val searchRequest = TaskSearchRequest(
                searchQueryText = query,
                assignedUserId = assignedUserId,
                projectId = projectId,
                currentTaskStatus = status,
                priorityLevel = priority
              )
              onSuccess(taskService.searchTasks(searchRequest)) { tasks =>
                complete(StatusCodes.OK, tasks)
              }
            }
          }
        }
      },
      path("my") {
        get {
          authenticateUser { userId =>
            onSuccess(taskService.getTasksAssignedToUser(userId)) { tasks =>
              complete(StatusCodes.OK, tasks)
            }
          }
        }
      },
      path("project" / LongNumber) { projectId =>
        get {
          authenticateUser { userId =>
            onSuccess(taskService.getAllTasksInProject(projectId)) { tasks =>
              complete(StatusCodes.OK, tasks)
            }
          }
        }
      },
      path(LongNumber) { taskId =>
        put {
          authenticateUser { userId =>
            entity(as[TaskUpdateRequest]) { request =>
              onSuccess(taskService.updateExistingTask(taskId, request, userId)) {
                case Right(task) =>
                  complete(StatusCodes.OK, task)
                case Left(error) =>
                  complete(StatusCodes.BadRequest, ErrorResponse(
                    errorCode = "TASK_UPDATE_FAILED",
                    errorMessage = error
                  ))
              }
            }
          }
        } ~
        delete {
          authenticateUser { userId =>
            onSuccess(taskService.deleteTask(taskId, userId)) {
              case Right(_) =>
                complete(StatusCodes.OK, Map("message" -> "タスクを削除しました"))
              case Left(error) =>
                complete(StatusCodes.BadRequest, ErrorResponse(
                  errorCode = "TASK_DELETION_FAILED",
                  errorMessage = error
                ))
            }
          }
        }
      },
      path(LongNumber / "complete") { taskId =>
        post {
          authenticateUser { userId =>
            onSuccess(taskService.completeTask(taskId, userId)) {
              case Right(task) =>
                complete(StatusCodes.OK, task)
              case Left(error) =>
                complete(StatusCodes.BadRequest, ErrorResponse(
                  errorCode = "TASK_COMPLETION_FAILED",
                  errorMessage = error
                ))
            }
          }
        }
      }
    )
  }

  /**
   * ヘルスチェックルート
   */
  val healthRoute: Route = path("health") {
    get {
      complete(StatusCodes.OK, Map(
        "status" -> "healthy",
        "timestamp" -> Timestamp.from(Instant.now()).toString
      ))
    }
  }

  /**
   * 全てのルートを結合
   */
  val allRoutes: Route = pathPrefix("api") {
    concat(
      authRoutes,
      projectRoutes,
      taskRoutes
    )
  } ~ healthRoute
}
