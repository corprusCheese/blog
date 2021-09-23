version := "0.1"

scalaVersion := "2.13.6"

scalacOptions ++= Seq("-Ymacro-annotations")

lazy val core = (project in file("core"))
  .settings(
    name := "core",
    scalaVersion := "2.13.6",
    libraryDependencies ++= Seq(
      "tf.tofu" %% "derevo-core" % "0.12.6",
      "tf.tofu" %% "derevo-cats" % "0.12.6",
      "tf.tofu" %% "derevo-circe-magnolia" % "0.12.6",
      "dev.optics" %% "monocle-core" % "3.0.0",
      "dev.optics" %% "monocle-macro" % "3.0.0",
      "io.estatico" %% "newtype" % "0.4.4",
      "eu.timepit" %% "refined" % "0.9.15"
    )
  )

lazy val storages = (project in file("storages"))
  .settings(
    name := "storages",
    scalaVersion := "2.13.6",
    libraryDependencies ++= Seq(
      "org.tpolecat" %% "doobie-core" % "1.0.0-RC1",
      "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC1",
      "org.tpolecat" %% "doobie-postgres-circe" % "1.0.0-RC1",
      "com.disneystreaming" %% "weaver-cats" % "0.7.6" % Test,
      "com.disneystreaming" %% "weaver-scalacheck" % "0.7.6" % Test,
      "io.chrisdavenport" %% "log4cats-core" % "1.1.1"
    )
  ).dependsOn(core)

lazy val http = (project in file("httpApi"))
  .settings(
    name := "httpApi",
    scalaVersion := "2.13.6",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-dsl" % "0.23.1",
      "org.http4s" %% "http4s-ember-server" % "0.23.1",
      "org.http4s" %% "http4s-ember-client" % "0.23.1",
      "org.http4s" %% "http4s-circe" % "0.23.1",
      "org.typelevel" %% "cats-core" % "2.6.1",
      "org.typelevel" %% "cats-effect" % "3.2.8",
      "com.github.cb372" %% "cats-retry" % "3.1.0",
      "com.disneystreaming" %% "weaver-cats" % "0.7.6" % Test,
      "com.disneystreaming" %% "weaver-scalacheck" % "0.7.6" % Test,
      "io.chrisdavenport" %% "log4cats-core" % "1.1.1",
      "co.fs2" %% "fs2-core" % "3.1.2"
    )
  ).dependsOn(core, storages)
