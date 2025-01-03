scalaVersion := "3.3.4"

scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked")

val scalusVersion = "0.8.3+7-70400d11+20250103-2110-SNAPSHOT"

// Add the Scalus compiler plugin
addCompilerPlugin("org.scalus" %% "scalus-plugin" % scalusVersion)

// Add the Scalus library and the Cardano Client library integration
libraryDependencies ++= Seq(
  // Scalus
  "org.scalus" % "scalus_3" % scalusVersion,
  "org.scalus" % "scalus-bloxbean-cardano-client-lib_3" % scalusVersion,
  // Cardano Client library
  "com.bloxbean.cardano" % "cardano-client-lib" % "0.6.3",
  "com.bloxbean.cardano" % "cardano-client-backend-blockfrost" % "0.6.3",
  "org.bouncycastle" % "bcprov-jdk18on" % "1.79",
  "com.softwaremill.sttp.tapir" %% "tapir-netty-server-sync" % "1.11.11",
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % "1.11.11",
  // Argument parsing
  "com.monovore" %% "decline" % "2.4.1",
  "org.slf4j" % "slf4j-simple" % "2.0.16"
)

// Test dependencies
testFrameworks += new TestFramework("munit.Framework")
libraryDependencies ++= Seq(
  "org.scalameta" %% "munit" % "1.0.3" % Test,
  "org.scalameta" %% "munit-scalacheck" % "1.0.0" % Test,
  "org.scalacheck" %% "scalacheck" % "1.18.1" % Test
)
