ThisBuild / scalaVersion := "2.13.13"

lazy val root = (project in file("."))
  .settings(
    name := "root-4-soc",
    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chisel3"    % "3.6.1",
      "edu.berkeley.cs" %% "chiseltest" % "0.6.1" % Test
    ),
    addCompilerPlugin(
      "edu.berkeley.cs" %% "chisel3-plugin" % "3.6.1" cross CrossVersion.full
    )
  )