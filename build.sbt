import scoverage.ScoverageSbtPlugin.ScoverageKeys

organization := "com.gilt"

name := "calatrava-client-library"

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.11.7", "2.10.5")

libraryDependencies ++= Seq(
  "com.google.inject" % "guice" % "4.0",
  "com.typesafe.play" %% "play-json" % "2.4.3",
  "com.gilt" %% "gfc-logging" % "0.0.3",
  "org.slf4j" % "slf4j-simple" % "1.7.12",
  "com.gilt" %% "gfc-util" % "0.1.1",
  "com.amazonaws" % "aws-java-sdk" % "1.10.20",
  "com.amazonaws" % "amazon-kinesis-client" % "1.6.1",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "org.mockito" % "mockito-core" % "1.8.5" % "test"
)

ScoverageKeys.coverageExcludedPackages := "com.gilt.calatrava.v0.models"

releaseCrossBuild := true

releasePublishArtifactsAction := PgpKeys.publishSigned.value

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

licenses := Seq("Apache-style" -> url("https://raw.githubusercontent.com/gilt/calatrava-client-library/master/LICENSE"))

homepage := Some(url("https://github.com/gilt/calatrava-client-library"))

pomExtra := <scm>
  <url>https://github.com/gilt/calatrava-client-library.git</url>
  <connection>scm:git:git@github.com:gilt/calatrava-client-library.git</connection>
</scm>
  <developers>
    <developer>
      <id>gheine</id>
      <name>Gregor Heine</name>
      <url>https://github.com/gheine</url>
    </developer>
    <developer>
      <id>ebowman</id>
      <name>Eric Bowman</name>
      <url>https://github.com/ebowman</url>
    </developer>
    <developer>
      <id>andreyk0</id>
      <name>Andrey Kartashov</name>
      <url>https://github.com/andreyk0</url>
    </developer>
  </developers>

