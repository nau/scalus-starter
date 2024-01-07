val scala3Version = "3.3.1"
ThisBuild / scalaVersion := scala3Version
autoCompilerPlugins := true
Global / onChangedBuildSource := ReloadOnSourceChanges

name := "scalus-starter"

version := "0.4.0"

scalaVersion := scala3Version

addCompilerPlugin("org.scalus" %% "scalus-plugin" % "0.4.0")

libraryDependencies += "org.scalus" %% "scalus" % "0.4.0"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.17" % "test"
libraryDependencies += "org.scalatestplus" %% "scalacheck-1-16" % "3.2.14.0" % "test"
