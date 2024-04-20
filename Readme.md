# Scalus Starter Project

This is a starter project for [Scalus](https://scalus.org) -
Scala-based DApps development platform for Cardano.

It contains a simple example of a Minting Policy script written using Scalus.

## Getting Started

### Prerequisites

You'll need Java JDK 11+, [scala-cli](https://scala-cli.virtuslab.org/) or [sbt](https://www.scala-sbt.org/).

If you use Nix, you can run `nix develop` to get a shell with all the dependencies.

### Setup IntelliJ IDEA

File -> New -> Project from Existing Sources -> select the project directory -> Import project from external model ->
BSP -> Scala-cli

### Building with scala-cli

Run `scala-cli setup-ide .` to generate IDE configuration files.

Run `scala-cli .` to compile and run the project.

Run `scala-cli test .` to run the tests.

### Running tests with sbt

Run `sbt` to enter the SBT shell.

Then run `test` to run the tests.

Run `~testQuick` to run the tests automatically when a file changes.

Run `run` to run the main method, which will print the hex encoded double CBOR of Minting Policy script.

## Tutorial

[Read the tutorial](https://scalus.org/docs/Tutorial)

## Resources

Find more information about Scalus in the [Scalus repository](https://github.com/nau/scalus).

Scalus Discord: <https://discord.gg/ygwtuBybsy>

Twitter: [@Scalus3](https://twitter.com/Scalus3)
