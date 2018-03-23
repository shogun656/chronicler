import sbt.Keys.{crossScalaVersions, organization, publishArtifact, version}
import sbt.url

lazy val commonSettings = Seq(
  version := "0.2.0",
  organization := "com.github.fsanaulla",
  crossScalaVersions := Seq("2.11.8", "2.12.4"),
  homepage := Some(url("https://github.com/fsanaulla/chronicler")),
  licenses += "Apache-2.0" -> url("https://opensource.org/licenses/Apache-2.0"),
  developers += Developer(id = "fsanaulla", name = "Faiaz Sanaulla", email = "fayaz.sanaulla@gmail.com", url = url("https://github.com/fsanaulla")),
  parallelExecution := false
)

lazy val publishSettings = Seq(
  useGpg := true,
  publishArtifact in Test := false,
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/fsanaulla/chronicler"),
      "https://github.com/fsanaulla/chronicler.git"
    )
  ),
  pomIncludeRepository := (_ => false),
  publishTo := Some(
    if (isSnapshot.value)
      Opts.resolver.sonatypeSnapshots
    else
      Opts.resolver.sonatypeStaging
  )
)

lazy val chronicler = (project in file("."))
  .settings(publishArtifact := false)
  .aggregate(
    core,
    akkaHttp,
    asyncHttp,
    udp,
    macros
  )

lazy val core = project
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := "chronicler-core",
    scalaVersion := "2.12.4",
    publishArtifact in (Test, packageBin) := true,
      scalacOptions ++= Seq(
        "-feature",
        "-language:implicitConversions",
        "-language:postfixOps"),
    libraryDependencies ++= Dependencies.coreDep
  )

lazy val akkaHttp = (project in file("akka-http"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := "chronicler-akka-http",
    scalaVersion := "2.12.4",
    libraryDependencies += Dependencies.akkaHttp
  ).dependsOn(core % "compile->compile;test->test")

lazy val asyncHttp = (project in file("async-http"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := "chronicler-async-http",
    scalaVersion := "2.12.4",
    libraryDependencies += Dependencies.asyncHttp
  ).dependsOn(core % "compile->compile;test->test")

lazy val udp = project
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := "chronicler-udp",
    scalaVersion := "2.12.4")
  .dependsOn(core)
  .dependsOn(asyncHttp % "test->test")

lazy val macros = project
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := "chronicler-macros",
    scalaVersion := "2.12.4",
    scalacOptions ++= Seq("-deprecation", "-feature"),
    libraryDependencies += Dependencies.scalaReflect
  ).dependsOn(core % "compile->compile;test->test")

addCommandAlias("fullTest", ";clean;compile;test:compile;coverage;test;coverageReport")

addCommandAlias("fullRelease", ";clean;publishSigned;sonatypeRelease")
// build all project in one task, for combining coverage reports and decreasing CI jobs
addCommandAlias(
  "universeTest",
  ";project core;+fullTest;" +
  "project akkaHttp;+fullTest;" +
  "project asyncHttp;+fullTest;" +
  "project udp;+fullTest;" +
  "project macros;+fullTest"
)
