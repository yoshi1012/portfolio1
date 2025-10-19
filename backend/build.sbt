name := "taskmanagement-backend"

version := "1.0.0"

scalaVersion := "2.13.12"

// Akka HTTP dependencies
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % "2.8.5",
  "com.typesafe.akka" %% "akka-stream" % "2.8.5",
  "com.typesafe.akka" %% "akka-http" % "10.5.3",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.5.3"
)

// Slick (database ORM)
libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.5.0",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.5.0"
)

// PostgreSQL driver
libraryDependencies += "org.postgresql" % "postgresql" % "42.7.1"

// JWT authentication
libraryDependencies += "com.github.jwt-scala" %% "jwt-core" % "9.4.5"

// BCrypt for password hashing
libraryDependencies += "org.mindrot" % "jbcrypt" % "0.4"

// Logging
libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.4.14",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5"
)

// Testing
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.17" % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % "10.5.3" % Test,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.8.5" % Test
)

// Assembly plugin settings (for creating fat JAR)
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case "reference.conf" => MergeStrategy.concat
  case x => MergeStrategy.first
}

assembly / mainClass := Some("com.taskmanagement.Main")
assembly / assemblyJarName := "taskmanagement-server.jar"
