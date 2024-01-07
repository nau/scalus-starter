# Scalus Starter Project

This is a starter project for [Scalus](https://scalus.org) -
Scala-based DApps development platform for Cardano.

It contains a simple example of a Minting Policy script written using Scalus.

## Getting Started

### Prerequisites

Install [Nix](https://nixos.org/nix/) with Flakes support.

Or make sure you have Java JDK 11, sbt, and Cardano Plutus uplc tool in your PATH.

### Scala-cli

Install [scala-cli](https://scala-cli.virtuslab.org/).

Then you can run

```shell
scala-cli src/main/scala/starter/MintingPolicy.scala
```

to compile and run the example.

### Working with Nix

Clone this repository and run `nix develop` to enter a shell with all the dependencies.
It may take a while to download all the dependencies the first time.

The shell will have Java JDK 11, sbt, uplc, cardano-node and cardano-cli available.

### Running tests

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
