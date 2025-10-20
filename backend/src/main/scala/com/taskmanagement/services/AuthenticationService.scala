package com.taskmanagement.services

import com.taskmanagement.models._
import com.taskmanagement.db.DatabaseSchema._
import com.taskmanagement.auth.{JwtAuthenticationHandler, PasswordHashingUtility}
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{Future, ExecutionContext}
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * 認証サービス
 * 
 * ユーザーの登録、ログイン、ログアウトなどの認証関連の処理を担当します。
 * 
 * 初心者向け解説:
 * サービスクラスは、ビジネスロジック（業務処理）を実装する場所です。
 * データベースへのアクセスやJWT認証の処理など、複数の処理を組み合わせて
 * 一つの機能を実現します。
 * 
 * 例えば、ユーザー登録の処理は以下のステップで構成されます:
 * 1. メールアドレスが既に使われていないかチェック
 * 2. パスワードをハッシュ化
 * 3. ユーザー情報をデータベースに保存
 * 4. JWTトークンを生成
 * 5. セッション情報を保存
 * 6. レスポンスを返す
 * 
 * @param database データベース接続
 * @param jwtHandler JWT認証ハンドラー
 * @param executionContext 非同期処理用のコンテキスト
 */
class AuthenticationService(
  database: Database,
  jwtHandler: JwtAuthenticationHandler
)(implicit executionContext: ExecutionContext) {

  /**
   * ユーザー登録
   * 
   * 新しいユーザーアカウントを作成します。
   * 
   * @param registrationRequest ユーザー登録リクエスト
   * @return 登録成功時はRight(LoginResponse)、失敗時はLeft(エラーメッセージ)
   * 
   * 処理の流れ:
   * 1. メールアドレスの重複チェック
   * 2. パスワードのハッシュ化
   * 3. ユーザー情報の保存
   * 4. JWTトークンの生成
   * 5. セッション情報の保存
   * 
   * 使用例:
   * val request = UserRegistrationRequest("user@example.com", "山田太郎", "password123")
   * authService.registerNewUser(request).map {
   *   case Right(response) => println(s"登録成功: ${response.userInfo.userFullName}")
   *   case Left(error) => println(s"登録失敗: $error")
   * }
   */
  def registerNewUser(registrationRequest: UserRegistrationRequest): Future[Either[String, LoginResponse]] = {
    val checkEmailQuery = usersTable
      .filter(_.userEmail === registrationRequest.userEmail)
      .filter(_.accountDeletedAt.isEmpty)
      .result
      .headOption

    database.run(checkEmailQuery).flatMap {
      case Some(_) =>
        Future.successful(Left("このメールアドレスは既に登録されています"))

      case None =>
        val hashedPassword = PasswordHashingUtility.hashPassword(registrationRequest.plainTextPassword)
        val currentTimestamp = Timestamp.from(Instant.now())

        val newUser = User(
          userId = 0L, // AutoIncなので0を指定
          userEmail = registrationRequest.userEmail,
          userFullName = registrationRequest.userFullName,
          userDisplayName = registrationRequest.userDisplayName,
          hashedPasswordValue = hashedPassword,
          userProfileImageUrl = None,
          userTimezone = "UTC",
          userLanguagePreference = "ja",
          accountCreatedAt = currentTimestamp,
          accountLastLoginAt = Some(currentTimestamp),
          isAccountActive = true,
          accountDeletedAt = None
        )

        val insertUserAction = (usersTable returning usersTable.map(_.userId)) += newUser

        database.run(insertUserAction).flatMap { insertedUserId =>
          val jwtToken = jwtHandler.generateTokenForUser(insertedUserId, registrationRequest.userEmail)
          val expirationTimestamp = Timestamp.from(Instant.now().plusSeconds(24 * 3600))

          val sessionId = UUID.randomUUID().toString
          val newSession = UserSession(
            sessionId = sessionId,
            userId = insertedUserId,
            jwtTokenValue = jwtToken,
            sessionCreatedAt = currentTimestamp,
            sessionExpiresAt = expirationTimestamp,
            sessionLastAccessedAt = currentTimestamp,
            userIpAddress = None,
            userAgentString = None
          )

          val insertSessionAction = userSessionsTable += newSession

          database.run(insertSessionAction).map { _ =>
            val userResponse = UserResponse(
              userId = insertedUserId,
              userEmail = registrationRequest.userEmail,
              userFullName = registrationRequest.userFullName,
              userDisplayName = registrationRequest.userDisplayName,
              userProfileImageUrl = None,
              userTimezone = "UTC",
              userLanguagePreference = "ja"
            )

            val loginResponse = LoginResponse(
              jwtToken = jwtToken,
              userInfo = userResponse,
              sessionExpiresAt = expirationTimestamp
            )

            Right(loginResponse)
          }
        }.recover {
          case ex: Exception =>
            Left(s"ユーザー登録中にエラーが発生しました: ${ex.getMessage}")
        }
    }
  }

  /**
   * ユーザーログイン
   * 
   * メールアドレスとパスワードでログインします。
   * 
   * @param loginRequest ログインリクエスト
   * @return ログイン成功時はRight(LoginResponse)、失敗時はLeft(エラーメッセージ)
   * 
   * 処理の流れ:
   * 1. メールアドレスでユーザーを検索
   * 2. パスワードの検証
   * 3. アカウントの有効性チェック
   * 4. JWTトークンの生成
   * 5. セッション情報の保存
   * 6. 最終ログイン日時の更新
   * 
   * 使用例:
   * val request = UserLoginRequest("user@example.com", "password123")
   * authService.authenticateUser(request).map {
   *   case Right(response) => println(s"ログイン成功: ${response.jwtToken}")
   *   case Left(error) => println(s"ログイン失敗: $error")
   * }
   */
  def authenticateUser(loginRequest: UserLoginRequest): Future[Either[String, LoginResponse]] = {
    val findUserQuery = usersTable
      .filter(_.userEmail === loginRequest.userEmail)
      .filter(_.accountDeletedAt.isEmpty)
      .result
      .headOption

    database.run(findUserQuery).flatMap {
      case None =>
        Future.successful(Left("メールアドレスまたはパスワードが正しくありません"))

      case Some(user) =>
        val isPasswordValid = PasswordHashingUtility.verifyPassword(
          loginRequest.plainTextPassword,
          user.hashedPasswordValue
        )

        if (!isPasswordValid) {
          Future.successful(Left("メールアドレスまたはパスワードが正しくありません"))
        } else if (!user.isAccountActive) {
          Future.successful(Left("このアカウントは無効化されています"))
        } else {
          val currentTimestamp = Timestamp.from(Instant.now())
          val jwtToken = jwtHandler.generateTokenForUser(user.userId, user.userEmail)
          val expirationTimestamp = Timestamp.from(Instant.now().plusSeconds(24 * 3600))

          val sessionId = UUID.randomUUID().toString
          val newSession = UserSession(
            sessionId = sessionId,
            userId = user.userId,
            jwtTokenValue = jwtToken,
            sessionCreatedAt = currentTimestamp,
            sessionExpiresAt = expirationTimestamp,
            sessionLastAccessedAt = currentTimestamp,
            userIpAddress = None,
            userAgentString = None
          )

          val updateLastLoginAction = usersTable
            .filter(_.userId === user.userId)
            .map(_.accountLastLoginAt)
            .update(Some(currentTimestamp))

          val combinedAction = for {
            _ <- userSessionsTable += newSession
            _ <- updateLastLoginAction
          } yield ()

          database.run(combinedAction.transactionally).map { _ =>
            val userResponse = UserResponse(
              userId = user.userId,
              userEmail = user.userEmail,
              userFullName = user.userFullName,
              userDisplayName = user.userDisplayName,
              userProfileImageUrl = user.userProfileImageUrl,
              userTimezone = user.userTimezone,
              userLanguagePreference = user.userLanguagePreference
            )

            val loginResponse = LoginResponse(
              jwtToken = jwtToken,
              userInfo = userResponse,
              sessionExpiresAt = expirationTimestamp
            )

            Right(loginResponse)
          }.recover {
            case ex: Exception =>
              Left(s"ログイン処理中にエラーが発生しました: ${ex.getMessage}")
          }
        }
    }
  }

  /**
   * ユーザーログアウト
   * 
   * セッションを無効化します。
   * 
   * @param jwtToken ログアウトするユーザーのJWTトークン
   * @return ログアウト成功時はRight(true)、失敗時はLeft(エラーメッセージ)
   * 
   * 処理の流れ:
   * 1. JWTトークンを検証
   * 2. セッション情報を削除
   * 
   * 使用例:
   * authService.logoutUser(token).map {
   *   case Right(_) => println("ログアウトしました")
   *   case Left(error) => println(s"ログアウト失敗: $error")
   * }
   */
  def logoutUser(jwtToken: String): Future[Either[String, Boolean]] = {
    jwtHandler.validateTokenAndExtractPayload(jwtToken) match {
      case None =>
        Future.successful(Left("無効なトークンです"))

      case Some(payload) =>
        val deleteSessionAction = userSessionsTable
          .filter(_.userId === payload.userId)
          .filter(_.jwtTokenValue === jwtToken)
          .delete

        database.run(deleteSessionAction).map { deletedCount =>
          if (deletedCount > 0) {
            Right(true)
          } else {
            Left("セッションが見つかりませんでした")
          }
        }.recover {
          case ex: Exception =>
            Left(s"ログアウト処理中にエラーが発生しました: ${ex.getMessage}")
        }
    }
  }

  /**
   * トークンからユーザー情報を取得
   * 
   * JWTトークンを検証し、対応するユーザー情報を取得します。
   * 
   * @param jwtToken JWTトークン
   * @return ユーザー情報（取得失敗時はNone）
   * 
   * 使用例:
   * authService.getUserFromToken(token).map {
   *   case Some(user) => println(s"ユーザー: ${user.userFullName}")
   *   case None => println("無効なトークンです")
   * }
   */
  def getUserFromToken(jwtToken: String): Future[Option[UserResponse]] = {
    jwtHandler.validateTokenAndExtractPayload(jwtToken) match {
      case None =>
        Future.successful(None)

      case Some(payload) =>
        val findUserQuery = usersTable
          .filter(_.userId === payload.userId)
          .filter(_.accountDeletedAt.isEmpty)
          .filter(_.isAccountActive === true)
          .result
          .headOption

        database.run(findUserQuery).map {
          case Some(user) =>
            Some(UserResponse(
              userId = user.userId,
              userEmail = user.userEmail,
              userFullName = user.userFullName,
              userDisplayName = user.userDisplayName,
              userProfileImageUrl = user.userProfileImageUrl,
              userTimezone = user.userTimezone,
              userLanguagePreference = user.userLanguagePreference
            ))
          case None =>
            None
        }
    }
  }

  /**
   * セッションの有効性をチェック
   * 
   * JWTトークンとセッション情報の両方をチェックします。
   * 
   * @param jwtToken JWTトークン
   * @return セッションが有効な場合true、無効な場合false
   */
  def isSessionValid(jwtToken: String): Future[Boolean] = {
    jwtHandler.validateTokenAndExtractPayload(jwtToken) match {
      case None =>
        Future.successful(false)

      case Some(payload) =>
        val currentTimestamp = Timestamp.from(Instant.now())
        val findSessionQuery = userSessionsTable
          .filter(_.userId === payload.userId)
          .filter(_.jwtTokenValue === jwtToken)
          .filter(_.sessionExpiresAt > currentTimestamp)
          .result
          .headOption

        database.run(findSessionQuery).map(_.isDefined)
    }
  }
}
