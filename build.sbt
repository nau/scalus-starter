scalaVersion := "3.3.4"

scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked")

// Add the Scalus compiler plugin
addCompilerPlugin("org.scalus" %% "scalus-plugin" % "0.8.3")

// Add the Scalus library and the Cardano Client library integration
libraryDependencies ++= Seq(
  "org.scalus" % "scalus_3" % "0.8.3",
  "org.scalus" % "scalus-bloxbean-cardano-client-lib_3" % "0.8.3"
)

// Test dependencies
testFrameworks += new TestFramework("munit.Framework")
libraryDependencies ++= Seq(
  "org.scalameta" %% "munit" % "1.0.3" % Test,
  "org.scalameta" %% "munit-scalacheck" % "1.0.0" % Test,
  "org.scalacheck" %% "scalacheck" % "1.18.1" % Test
)
