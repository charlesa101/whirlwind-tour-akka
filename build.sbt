// *****************************************************************************
// Projects
// *****************************************************************************

lazy val `whirlwind-tour-akka` =
  project
    .in(file("."))
    .enablePlugins(AutomateHeaderPlugin, GitVersioning, DockerPlugin, JavaAppPackaging)
    .settings(settings)
    .settings(
      libraryDependencies ++= Seq(
        library.akkaClusterShardingTyped,
        library.akkaHttp,
        library.akkaHttpCirce,
        library.akkaLog4j,
        library.akkaManagement,
        library.akkaPersistenceCassandra,
        library.akkaPersistenceQuery,
        library.akkaPersistenceTyped,
        library.akkaStream,
        library.catsCore,
        library.circeGeneric,
        library.circeRefined,
        library.disruptor,
        library.log4jApiScala,
        library.log4jCore,
        library.pureConfig,
        library.refined,
        library.refinedCats,
        library.refinedPureconfig,
        library.scalapbRuntime          % "protobuf",
        library.akkaHttpTestkit         % Test,
        library.akkaPersistenceInmemory % Test,
        library.akkaTestkitTyped        % Test,
        library.circeParser             % Test,
        library.scalaCheck              % Test,
        library.utest                   % Test
      )
    )

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val akka                     = "2.5.9"
      val akkaHttp                 = "10.1.0-RC1"
      val akkaHttpJson             = "1.20.0-RC1"
      val akkaLog4j                = "1.6.0"
      val akkaManagement           = "0.6"
      val akkaPersistenceCassandra = "0.80"
      val akkaPersistenceInmemory  = "2.5.1.1"
      val cats                     = "1.0.1"
      val circe                    = "0.9.1"
      val disruptor                = "3.3.7"
      val log4j                    = "2.10.0"
      val log4jApiScala            = "11.0"
      val pureConfig               = "0.9.0"
      val refined                  = "0.8.7"
      val scalaCheck               = "1.13.5"
      val scalapb                  = com.trueaccord.scalapb.compiler.Version.scalapbVersion
      val utest                    = "0.6.3"
    }
    val akkaClusterShardingTyped = "com.typesafe.akka"        %% "akka-cluster-sharding-typed"  % Version.akka
    val akkaHttp                 = "com.typesafe.akka"        %% "akka-http"                    % Version.akkaHttp
    val akkaHttpCirce            = "de.heikoseeberger"        %% "akka-http-circe"              % Version.akkaHttpJson
    val akkaHttpTestkit          = "com.typesafe.akka"        %% "akka-http-testkit"            % Version.akkaHttp
    val akkaLog4j                = "de.heikoseeberger"        %% "akka-log4j"                   % Version.akkaLog4j
    val akkaManagement           = "com.lightbend.akka"       %% "akka-management-cluster-http" % Version.akkaManagement
    val akkaPersistenceCassandra = "com.typesafe.akka"        %% "akka-persistence-cassandra"   % Version.akkaPersistenceCassandra
    val akkaPersistenceInmemory  = "com.github.dnvriend"      %% "akka-persistence-inmemory"    % Version.akkaPersistenceInmemory
    val akkaPersistenceQuery     = "com.typesafe.akka"        %% "akka-persistence-query"       % Version.akka
    val akkaPersistenceTyped     = "com.typesafe.akka"        %% "akka-persistence-typed"       % Version.akka
    val akkaStream               = "com.typesafe.akka"        %% "akka-stream"                  % Version.akka
    val akkaTestkitTyped         = "com.typesafe.akka"        %% "akka-testkit-typed"           % Version.akka
    val catsCore                 = "org.typelevel"            %% "cats-core"                    % Version.cats
    val circeGeneric             = "io.circe"                 %% "circe-generic"                % Version.circe
    val circeParser              = "io.circe"                 %% "circe-parser"                 % Version.circe
    val circeRefined             = "io.circe"                 %% "circe-refined"                % Version.circe
    val disruptor                = "com.lmax"                 %  "disruptor"                    % Version.disruptor
    val log4jApiScala            = "org.apache.logging.log4j" %% "log4j-api-scala"              % Version.log4jApiScala
    val log4jCore                = "org.apache.logging.log4j" %  "log4j-core"                   % Version.log4j
    val pureConfig               = "com.github.pureconfig"    %% "pureconfig"                   % Version.pureConfig
    val refined                  = "eu.timepit"               %% "refined"                      % Version.refined
    val refinedCats              = "eu.timepit"               %% "refined-cats"                 % Version.refined
    val refinedPureconfig        = "eu.timepit"               %% "refined-pureconfig"           % Version.refined
    val scalaCheck               = "org.scalacheck"           %% "scalacheck"                   % Version.scalaCheck
    val scalapbRuntime           = "com.trueaccord.scalapb"   %% "scalapb-runtime"              % Version.scalapb
    val utest                    = "com.lihaoyi"              %% "utest"                        % Version.utest
  }

// *****************************************************************************
// Settings
// *****************************************************************************

lazy val settings =
  commonSettings ++
    gitSettings ++
    scalafmtSettings ++
    protoSettings ++
    dockerSettings ++
    commandAliases

lazy val commonSettings =
  Seq(
    // scalaVersion from .travis.yml via sbt-travisci
    // scalaVersion := "2.12.4",
    organization := "rocks.heikoseeberger",
    organizationName := "Heiko Seeberger",
    startYear := Some(2018),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-language:_",
      "-target:jvm-1.8",
      "-encoding", "UTF-8",
      "-Ypartial-unification",
      "-Ywarn-unused-import"
    ),
    Compile / unmanagedSourceDirectories := Seq((Compile / scalaSource).value),
    Test / unmanagedSourceDirectories := Seq((Test / scalaSource).value),
    Compile / packageDoc / publishArtifact := false,
    Compile / packageSrc / publishArtifact := false,
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val gitSettings =
  Seq(
    git.useGitDescribe := true
  )

lazy val scalafmtSettings =
  Seq(
    scalafmtOnCompile := true
  )

lazy val protoSettings =
  Seq(
    Compile / PB.targets :=
      Seq(scalapb.gen(flatPackage = true) -> (Compile / sourceManaged).value)
  )

lazy val dockerSettings =
  Seq(
    Docker / daemonUser := "root",
    Docker / maintainer := "Heiko Seeberger",
    Docker / version := "latest",
    dockerBaseImage := "openjdk:8u151-slim",
    dockerExposedPorts := Vector(8000),
    dockerRepository := Some("hseeberger")
  )

lazy val commandAliases =
  addCommandAlias(
    "r0",
    """|reStart
       |---
       |-Dwta.use-cluster-bootstrap=off
       |-Dcassandra-journal.contact-points.0=127.0.0.1:9042
       |-Dcassandra-snapshot-store.contact-points.0=127.0.0.1:9042
       |-Dakka.remote.netty.tcp.hostname=127.0.0.1
       |-Dakka.remote.netty.tcp.port=2550
       |-Dakka.cluster.seed-nodes.0=akka.tcp://wta@127.0.0.1:2550""".stripMargin
  ) ++
    addCommandAlias(
      "r1",
      """|reStart
         |---
         |-Dwta.use-cluster-bootstrap=off
         |-Dwta.api.port=8001
         |-Dcassandra-journal.contact-points.0=127.0.0.1:9042
         |-Dcassandra-snapshot-store.contact-points.0=127.0.0.1:9042
         |-Dakka.remote.netty.tcp.hostname=127.0.0.1
         |-Dakka.remote.netty.tcp.port=2551
         |-Dakka.cluster.seed-nodes.0=akka.tcp://wta@127.0.0.1:2550""".stripMargin
    )
