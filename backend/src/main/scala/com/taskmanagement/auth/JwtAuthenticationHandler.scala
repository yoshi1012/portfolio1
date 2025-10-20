package com.taskmanagement.auth

import com.taskmanagement.models.JwtPayload
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import spray.json._
import spray.json.DefaultJsonProtocol._
import java.time.Instant
import scala.util.{Try, Success, Failure}

/**
 * JWT認証ハンドラー
 * 
 * このクラスは、JWT（JSON Web Token）を使った認証を処理します。
 * 
 * 初心者向け解説:
 * JWT認証とは、ユーザーがログインした後に「トークン」という文字列を発行し、
 * それ以降のリクエストでそのトークンを使って本人確認をする仕組みです。
 * 
 * 例えば:
 * 1. ユーザーがメールアドレスとパスワードでログイン
 * 2. サーバーがJWTトークンを発行（例: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."）
 * 3. ユーザーは以降のリクエストでこのトークンを送信
 * 4. サーバーはトークンを検証して、誰からのリクエストか判断
 * 
 * メリット:
 * - セッション情報をサーバーに保存しなくて良い（スケーラブル）
 * - トークンに情報を含められる（ユーザーIDなど）
 * - 有効期限を設定できる
 * 
 * @param jwtSecretKey JWTの署名に使う秘密鍵（環境変数から取得）
 * @param tokenExpirationHours トークンの有効期限（時間単位）
 */
class JwtAuthenticationHandler(
  jwtSecretKey: String,
  tokenExpirationHours: Int = 24
) {

  private val jwtAlgorithm = JwtAlgorithm.HS256

  /**
   * JWTトークンを生成する
   * 
   * ユーザーがログインに成功したときに呼び出されます。
   * 
   * @param userId ユーザーID
   * @param userEmail ユーザーのメールアドレス
   * @return 生成されたJWTトークン文字列
   * 
   * 使用例:
   * val token = jwtHandler.generateTokenForUser(123, "user@example.com")
   * // token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
   */
  def generateTokenForUser(userId: Long, userEmail: String): String = {
    val currentTimestamp = Instant.now().getEpochSecond
    val expirationTimestamp = currentTimestamp + (tokenExpirationHours * 3600)

    val payloadJson = JsObject(
      "userId" -> JsNumber(userId),
      "userEmail" -> JsString(userEmail),
      "issuedAt" -> JsNumber(currentTimestamp),
      "expiresAt" -> JsNumber(expirationTimestamp)
    ).compactPrint

    val jwtClaim = JwtClaim(
      content = payloadJson,
      issuedAt = Some(currentTimestamp),
      expiration = Some(expirationTimestamp)
    )

    Jwt.encode(jwtClaim, jwtSecretKey, jwtAlgorithm)
  }

  /**
   * JWTトークンを検証する
   * 
   * クライアントから送られてきたトークンが正しいかどうかを確認します。
   * 
   * @param tokenString 検証するJWTトークン
   * @return 検証成功時はSome(JwtPayload)、失敗時はNone
   * 
   * 検証内容:
   * 1. トークンの署名が正しいか（改ざんされていないか）
   * 2. トークンの有効期限が切れていないか
   * 3. トークンの形式が正しいか
   * 
   * 使用例:
   * jwtHandler.validateTokenAndExtractPayload(token) match {
   *   case Some(payload) => println(s"ユーザーID: ${payload.userId}")
   *   case None => println("トークンが無効です")
   * }
   */
  def validateTokenAndExtractPayload(tokenString: String): Option[JwtPayload] = {
    Try {
      val decodedClaim = Jwt.decode(tokenString, jwtSecretKey, Seq(jwtAlgorithm))
      
      decodedClaim match {
        case Success(claim) =>
          if (isTokenExpired(claim)) {
            None
          } else {
            parsePayloadFromClaim(claim.content)
          }
        case Failure(_) =>
          None
      }
    }.getOrElse(None)
  }

  /**
   * トークンが期限切れかどうかをチェック
   * 
   * @param claim JWTクレーム
   * @return 期限切れの場合true、まだ有効な場合false
   */
  private def isTokenExpired(claim: JwtClaim): Boolean = {
    claim.expiration match {
      case Some(expirationTimestamp) =>
        val currentTimestamp = Instant.now().getEpochSecond
        currentTimestamp > expirationTimestamp
      case None =>
        true
    }
  }

  /**
   * JWTクレームからペイロードを抽出
   * 
   * @param claimContent JWTクレームの内容（JSON文字列）
   * @return パース成功時はSome(JwtPayload)、失敗時はNone
   */
  private def parsePayloadFromClaim(claimContent: String): Option[JwtPayload] = {
    Try {
      val jsonAst = claimContent.parseJson.asJsObject
      val fields = jsonAst.fields

      JwtPayload(
        userId = fields("userId").convertTo[Long],
        userEmail = fields("userEmail").convertTo[String],
        issuedAtTimestamp = fields("issuedAt").convertTo[Long],
        expiresAtTimestamp = fields("expiresAt").convertTo[Long]
      )
    }.toOption
  }

  /**
   * Authorizationヘッダーからトークンを抽出
   * 
   * HTTPリクエストのAuthorizationヘッダーは通常 "Bearer <token>" という形式です。
   * この関数は "Bearer " プレフィックスを除去してトークン部分だけを取り出します。
   * 
   * @param authorizationHeaderValue Authorizationヘッダーの値
   * @return トークン文字列（Bearerプレフィックスなし）
   * 
   * 使用例:
   * val header = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
   * val token = extractTokenFromAuthorizationHeader(header)
   * // token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
   */
  def extractTokenFromAuthorizationHeader(authorizationHeaderValue: String): Option[String] = {
    if (authorizationHeaderValue.startsWith("Bearer ")) {
      Some(authorizationHeaderValue.substring(7))
    } else {
      None
    }
  }

  /**
   * トークンの残り有効時間を取得（秒単位）
   * 
   * @param tokenString JWTトークン
   * @return 残り有効時間（秒）、トークンが無効な場合は0
   */
  def getRemainingValiditySeconds(tokenString: String): Long = {
    Try {
      val decodedClaim = Jwt.decode(tokenString, jwtSecretKey, Seq(jwtAlgorithm))
      
      decodedClaim match {
        case Success(claim) =>
          claim.expiration match {
            case Some(expirationTimestamp) =>
              val currentTimestamp = Instant.now().getEpochSecond
              val remainingSeconds = expirationTimestamp - currentTimestamp
              if (remainingSeconds > 0) remainingSeconds else 0
            case None => 0
          }
        case Failure(_) => 0
      }
    }.getOrElse(0)
  }

  /**
   * トークンをリフレッシュ（新しいトークンを発行）
   * 
   * 既存のトークンが有効な場合、新しい有効期限で新しいトークンを発行します。
   * これにより、ユーザーが継続的に使用している限り、ログイン状態を維持できます。
   * 
   * @param oldTokenString 既存のJWTトークン
   * @return 新しいJWTトークン（既存のトークンが無効な場合はNone）
   * 
   * 使用例:
   * jwtHandler.refreshTokenIfValid(oldToken) match {
   *   case Some(newToken) => println("トークンを更新しました")
   *   case None => println("トークンが無効なため更新できません")
   * }
   */
  def refreshTokenIfValid(oldTokenString: String): Option[String] = {
    validateTokenAndExtractPayload(oldTokenString).map { payload =>
      generateTokenForUser(payload.userId, payload.userEmail)
    }
  }
}

/**
 * パスワードハッシュ化ユーティリティ
 * 
 * パスワードを安全に保存するためのユーティリティクラスです。
 * BCryptアルゴリズムを使用してパスワードをハッシュ化します。
 * 
 * 初心者向け解説:
 * パスワードをそのままデータベースに保存すると、データベースが漏洩したときに
 * 全てのユーザーのパスワードが知られてしまいます。
 * 
 * そこで、パスワードを「ハッシュ化」という処理で変換してから保存します。
 * ハッシュ化は一方向の変換なので、ハッシュ値から元のパスワードを復元できません。
 * 
 * 例:
 * - 元のパスワード: "password123"
 * - ハッシュ化後: "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
 * 
 * ログイン時は、入力されたパスワードをハッシュ化して、保存されているハッシュ値と
 * 比較することで、パスワードが正しいかどうかを判定します。
 */
object PasswordHashingUtility {

  /**
   * パスワードをハッシュ化する
   * 
   * @param plainTextPassword 平文のパスワード
   * @return ハッシュ化されたパスワード
   * 
   * 使用例:
   * val hashedPassword = PasswordHashingUtility.hashPassword("password123")
   * // hashedPassword = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
   */
  def hashPassword(plainTextPassword: String): String = {
    org.mindrot.jbcrypt.BCrypt.hashpw(plainTextPassword, org.mindrot.jbcrypt.BCrypt.gensalt())
  }

  /**
   * パスワードを検証する
   * 
   * 入力されたパスワードが、保存されているハッシュ値と一致するかを確認します。
   * 
   * @param plainTextPassword 検証する平文のパスワード
   * @param hashedPasswordValue 保存されているハッシュ化されたパスワード
   * @return パスワードが一致する場合true、一致しない場合false
   * 
   * 使用例:
   * val isValid = PasswordHashingUtility.verifyPassword("password123", storedHash)
   * if (isValid) {
   *   println("パスワードが正しいです")
   * } else {
   *   println("パスワードが間違っています")
   * }
   */
  def verifyPassword(plainTextPassword: String, hashedPasswordValue: String): Boolean = {
    Try {
      org.mindrot.jbcrypt.BCrypt.checkpw(plainTextPassword, hashedPasswordValue)
    }.getOrElse(false)
  }
}
