val scala3Version = "3.2.0"
val AkkaVersion = "2.6.19"

lazy val root = project
  .in(file("."))
  .settings(
    name := "Test mqtt",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "0.7.29" % Test,
      ("com.lightbend.akka" %% "akka-stream-alpakka-mqtt" % "4.0.0").cross(CrossVersion.for3Use2_13),
      ("com.typesafe.akka" %% "akka-stream" % AkkaVersion).cross(CrossVersion.for3Use2_13)
    ),

    scalacOptions ++= Seq( // use ++= to add to existing options
      "-Werror",
      "-explain",
    ) // for "trailing comma", the closing paren must be on the next line

  )
