ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.0"

Global / excludeLintKeys += idePackagePrefix

lazy val zioVersion = "2.0.16"

lazy val root = (project in file("."))
  .settings(
    name := "bghq",
    idePackagePrefix := Some("it.carlodepieri.bghq"),
    libraryDependencies ++= Seq(
      // core
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-streams" % zioVersion,
      // testing
      "dev.zio" %% "zio-test" % zioVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
      "dev.zio" %% "zio-test-magnolia" % zioVersion % Test,
      "dev.zio" %% "zio-test-junit" % zioVersion % "test",
      "dev.zio" %% "zio-mock" % "1.0.0-RC11" % Test,
      "com.github.sbt" % "junit-interface" % "0.13.3" % Test,
      "org.mockito" % "mockito-core" % "5.2.0" % Test,
      // libs
      "net.ruippeixotog" %% "scala-scraper" % "3.1.0",
      "dev.zio" %% "zio-json" % "0.6.1",
      "io.lemonlabs" %% "scala-uri" % "4.0.3",
      // redis
      "dev.zio" %% "zio-redis" % "0.2.0",
      "dev.zio" %% "zio-schema-protobuf" % "0.4.11",
      "dev.zio" %% "zio-redis-embedded" % "0.2.0" % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
