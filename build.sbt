autoCompilerPlugins := true
Global / onChangedBuildSource := ReloadOnSourceChanges

name := "scalus-starter"

version := "0.6.1"

ThisBuild / scalaVersion := "3.3.3"

addCompilerPlugin("org.scalus" %% "scalus-plugin" % "0.6.1")

libraryDependencies += "org.scalus" %% "scalus" % "0.6.1"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.17" % "test"
libraryDependencies += "org.scalatestplus" %% "scalacheck-1-16" % "3.2.14.0" % "test"
