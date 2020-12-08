lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := """minesweeper""",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.13.1",
    libraryDependencies ++= Seq(
      guice,
      ws,
      filters,
      "org.postgresql" % "postgresql" % "42.2.18",
      "com.typesafe.play" %% "play-slick" % "5.0.0",
      "com.typesafe.play" %% "play-slick-evolutions" % "5.0.0",
      "io.jvm.uuid" %% "scala-uuid" % "0.3.1",
      "org.scalatest" %% "scalatest" % "3.0.8" % Test,
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test,
      "org.mockito" %% "mockito-scala-scalatest" % "1.16.3" % Test,
      "com.h2database" % "h2" % "1.4.199" % Test,
    ),
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-Xfatal-warnings"
    )
  )
