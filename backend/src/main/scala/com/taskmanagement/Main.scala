package com.taskmanagement

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.taskmanagement.api.Routes
import com.taskmanagement.services._
import com.taskmanagement.auth.JwtAuthenticationHandler
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn
import scala.util.{Success, Failure}

/**
 * アプリケーションのメインエントリーポイント
 * 
 * このファイルは、アプリケーションを起動するためのメインクラスです。
 * 
 * 初心者向け解説:
 * このファイルが実行されると、以下の処理が順番に行われます:
 * 
 * 1. 設定の読み込み（データベース接続情報、JWTシークレットなど）
 * 2. データベースへの接続
 * 3. サービスクラスの初期化（認証、タスク管理、プロジェクト管理）
 * 4. HTTPサーバーの起動
 * 5. リクエストの待ち受け
 * 
 * サーバーが起動すると、クライアント（ブラウザやモバイルアプリ）からの
 * HTTPリクエストを受け付けるようになります。
 */
object Main extends App {
  
  implicit val actorSystem: ActorSystem = ActorSystem("taskmanagement-system")
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  println("=" * 80)
  println("Asana風タスク管理アプリケーション")
  println("=" * 80)
  println()

  
  println("設定を読み込んでいます...")
  
  val serverInterface = sys.env.getOrElse("SERVER_INTERFACE", "0.0.0.0")
  val serverPort = sys.env.getOrElse("SERVER_PORT", "8080").toInt
  
  val databaseUrl = sys.env.getOrElse("DATABASE_URL", "jdbc:postgresql://localhost:5432/taskmanagement_db")
  val databaseUser = sys.env.getOrElse("DATABASE_USER", "postgres")
  val databasePassword = sys.env.getOrElse("DATABASE_PASSWORD", "postgres")
  
  val jwtSecretKey = sys.env.getOrElse("JWT_SECRET", "your-secret-key-change-this-in-production")
  val jwtExpirationHours = sys.env.getOrElse("JWT_EXPIRATION_HOURS", "24").toInt

  println(s"サーバーアドレス: $serverInterface:$serverPort")
  println(s"データベースURL: $databaseUrl")
  println(s"JWTトークン有効期限: ${jwtExpirationHours}時間")
  println()

  
  println("データベースに接続しています...")
  
  val database = Database.forURL(
    url = databaseUrl,
    user = databaseUser,
    password = databasePassword,
    driver = "org.postgresql.Driver",
    executor = AsyncExecutor(
      name = "taskmanagement-db-executor",
      minThreads = 20,
      maxThreads = 20,
      queueSize = 1000,
      maxConnections = 20
    )
  )

  val connectionTest = database.run(sql"SELECT 1".as[Int])
  connectionTest.onComplete {
    case Success(_) =>
      println("✓ データベース接続成功")
    case Failure(ex) =>
      println(s"✗ データベース接続失敗: ${ex.getMessage}")
      println("データベースが起動しているか、接続情報が正しいか確認してください。")
  }

  
  println("サービスを初期化しています...")
  
  val jwtHandler = new JwtAuthenticationHandler(jwtSecretKey, jwtExpirationHours)
  val authService = new AuthenticationService(database, jwtHandler)
  val taskService = new TaskManagementService(database)
  val projectService = new ProjectManagementService(database)
  
  println("✓ サービス初期化完了")
  println()

  
  val routes = new Routes(authService, taskService, projectService, jwtHandler)
  val allRoutes: Route = routes.allRoutes

  
  println("HTTPサーバーを起動しています...")
  println()
  
  val bindingFuture: Future[Http.ServerBinding] = Http()
    .newServerAt(serverInterface, serverPort)
    .bind(allRoutes)

  bindingFuture.onComplete {
    case Success(binding) =>
      val address = binding.localAddress
      println("=" * 80)
      println(s"✓ サーバーが起動しました: http://${address.getHostString}:${address.getPort}/")
      println("=" * 80)
      println()
      println("利用可能なエンドポイント:")
      println("  - GET  /health                    : ヘルスチェック")
      println("  - POST /api/auth/register         : ユーザー登録")
      println("  - POST /api/auth/login            : ログイン")
      println("  - POST /api/auth/logout           : ログアウト")
      println("  - GET  /api/auth/me               : 現在のユーザー情報")
      println("  - POST /api/projects              : プロジェクト作成")
      println("  - GET  /api/projects/my           : 自分のプロジェクト一覧")
      println("  - POST /api/tasks                 : タスク作成")
      println("  - GET  /api/tasks/my              : 自分のタスク一覧")
      println("  - PUT  /api/tasks/{id}            : タスク更新")
      println("  - POST /api/tasks/{id}/complete   : タスク完了")
      println("  - DELETE /api/tasks/{id}          : タスク削除")
      println()
      println("サーバーを停止するには Ctrl+C を押してください")
      println("=" * 80)

    case Failure(ex) =>
      println("=" * 80)
      println(s"✗ サーバーの起動に失敗しました: ${ex.getMessage}")
      println("=" * 80)
      println()
      println("考えられる原因:")
      println(s"  - ポート $serverPort が既に使用されている")
      println("  - ファイアウォールでポートがブロックされている")
      println("  - 権限が不足している")
      println()
      actorSystem.terminate()
  }

  
  sys.addShutdownHook {
    println()
    println("=" * 80)
    println("サーバーをシャットダウンしています...")
    println("=" * 80)
    
    bindingFuture
      .flatMap(_.unbind())
      .onComplete { _ =>
        println("✓ HTTPサーバーを停止しました")
        database.close()
        println("✓ データベース接続を閉じました")
        actorSystem.terminate()
        println("✓ ActorSystemを終了しました")
        println()
        println("シャットダウン完了")
      }
  }
}
