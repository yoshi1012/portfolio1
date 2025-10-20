name := "taskmanagement-backend"

version := "1.0.0"

scalaVersion := "2.13.12"

// Pekko HTTP dependencies (Apache-licensed fork of Akka)
libraryDependencies ++= Seq(
  "org.apache.pekko" %% "pekko-actor-typed" % "1.0.3",
  "org.apache.pekko" %% "pekko-stream" % "1.0.3",
  "org.apache.pekko" %% "pekko-http" % "1.0.1",
  "org.apache.pekko" %% "pekko-http-spray-json" % "1.0.1"
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
  "org.apache.pekko" %% "pekko-http-testkit" % "1.0.1" % Test,
  "org.apache.pekko" %% "pekko-actor-testkit-typed" % "1.0.3" % Test
)

// Assembly plugin settings (for creating fat JAR)
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case "reference.conf" => MergeStrategy.concat
  case x => MergeStrategy.first
}

assembly / mainClass := Some("com.taskmanagement.Main")
assembly / assemblyJarName := "taskmanagement-server.jar"
