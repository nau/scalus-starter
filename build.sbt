val scala3Version = "3.2.2"
ThisBuild / scalaVersion := scala3Version
autoCompilerPlugins := true


name := "scalus-starter"

version := "0.1.0"

scalaVersion := scala3Version

addCompilerPlugin("org.scalus" %% "scalus-plugin" % "0.1.0")

libraryDependencies += "org.scalus" %% "scalus" % "0.1.0"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.15" % "test"
libraryDependencies += "org.scalatestplus" %% "scalacheck-1-16" % "3.2.14.0" % "test"
