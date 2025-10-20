package com.taskmanagement.api

import com.taskmanagement.models._
import spray.json._
import java.sql.Timestamp
import java.time.{LocalDate, Instant}

/**
 * JSON シリアライゼーションプロトコル
 * 
 * このファイルでは、ScalaのケースクラスとJSON形式の相互変換を定義しています。
 * 
 * 初心者向け解説:
 * WebAPIでは、データをJSON形式でやり取りします。
 * 例えば、ユーザー情報を送る場合:
 * 
 * Scalaのケースクラス:
 * User(id = 1, email = "user@example.com", name = "山田太郎")
 * 
 * JSON形式:
 * {"userId": 1, "userEmail": "user@example.com", "userFullName": "山田太郎"}
 * 
 * この変換を自動的に行うための設定がこのファイルです。
 */
trait JsonProtocols extends DefaultJsonProtocol {

  /**
   * Timestamp型のJSON変換
   * 
   * TimestampをUnixタイムスタンプ（ミリ秒）として扱います。
   */
  implicit object TimestampFormat extends RootJsonFormat[Timestamp] {
    def write(timestamp: Timestamp): JsValue = JsNumber(timestamp.getTime)
    def read(value: JsValue): Timestamp = value match {
      case JsNumber(millis) => new Timestamp(millis.toLong)
      case _ => deserializationError("Timestamp expected")
    }
  }

  /**
   * LocalDate型のJSON変換
   * 
   * LocalDateをISO 8601形式の文字列（例: "2024-12-31"）として扱います。
   */
  implicit object LocalDateFormat extends RootJsonFormat[LocalDate] {
    def write(date: LocalDate): JsValue = JsString(date.toString)
    def read(value: JsValue): LocalDate = value match {
      case JsString(dateStr) => LocalDate.parse(dateStr)
      case _ => deserializationError("LocalDate expected")
    }
  }


  implicit val userRegistrationRequestFormat: RootJsonFormat[UserRegistrationRequest] = jsonFormat4(UserRegistrationRequest)
  implicit val userLoginRequestFormat: RootJsonFormat[UserLoginRequest] = jsonFormat2(UserLoginRequest)
  implicit val userResponseFormat: RootJsonFormat[UserResponse] = jsonFormat7(UserResponse)
  implicit val jwtPayloadFormat: RootJsonFormat[JwtPayload] = jsonFormat4(JwtPayload)
  implicit val loginResponseFormat: RootJsonFormat[LoginResponse] = jsonFormat3(LoginResponse)


  implicit val projectCreationRequestFormat: RootJsonFormat[ProjectCreationRequest] = jsonFormat6(ProjectCreationRequest)
  implicit val projectFormat: RootJsonFormat[Project] = jsonFormat15(Project)
  implicit val projectMemberFormat: RootJsonFormat[ProjectMember] = jsonFormat6(ProjectMember)


  implicit val taskCreationRequestFormat: RootJsonFormat[TaskCreationRequest] = jsonFormat8(TaskCreationRequest)
  implicit val taskUpdateRequestFormat: RootJsonFormat[TaskUpdateRequest] = jsonFormat7(TaskUpdateRequest)
  implicit val taskFormat: RootJsonFormat[Task] = jsonFormat20(Task)
  implicit val taskDetailResponseFormat: RootJsonFormat[TaskDetailResponse] = jsonFormat12(TaskDetailResponse)
  implicit val taskSearchRequestFormat: RootJsonFormat[TaskSearchRequest] = jsonFormat8(TaskSearchRequest)


  implicit val commentCreationRequestFormat: RootJsonFormat[CommentCreationRequest] = jsonFormat2(CommentCreationRequest)
  implicit val commentFormat: RootJsonFormat[Comment] = jsonFormat7(Comment)


  implicit val notificationResponseFormat: RootJsonFormat[NotificationResponse] = jsonFormat7(NotificationResponse)


  implicit val paginationRequestFormat: RootJsonFormat[PaginationRequest] = jsonFormat2(PaginationRequest)
  
  implicit def paginatedResponseFormat[T: JsonFormat]: RootJsonFormat[PaginatedResponse[T]] = jsonFormat5(PaginatedResponse.apply[T])

  implicit val errorResponseFormat: RootJsonFormat[ErrorResponse] = jsonFormat4(ErrorResponse)


  implicit val organizationFormat: RootJsonFormat[Organization] = jsonFormat7(Organization)
  implicit val workspaceFormat: RootJsonFormat[Workspace] = jsonFormat8(Workspace)
  implicit val sectionFormat: RootJsonFormat[Section] = jsonFormat6(Section)
  implicit val tagFormat: RootJsonFormat[Tag] = jsonFormat6(Tag)
}

object JsonProtocols extends JsonProtocols
