name := """proj"""

version := "2.6.x"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  cache,
  ws,
  guice,
  "mysql" % "mysql-connector-java" % "5.1.34",
  "com.typesafe.play" %% "play-slick" % "3.0.0-M5",
  "com.typesafe.play" %% "play-slick-evolutions" % "3.0.0-M5",
  "com.typesafe.play" %% "play-mailer" % "6.0.0",
  "com.typesafe.play" %% "play-mailer-guice" % "6.0.0",

  "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.0" % Test

)

