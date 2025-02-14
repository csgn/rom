import Dependencies._

/* Project settings */
ThisBuild / name := "rom"
ThisBuild / scalaVersion := "2.13.15"
ThisBuild / version := "1.0.0-SNAPSHOT"
ThisBuild / description := "In-Memory MapReduce implementation in Scala."
ThisBuild / licenses := List(("MIT", url("https://opensource.org/license/mit")))
ThisBuild / developers ++= List(
  Developer(
    id = "csgn",
    name = "Sergen Cepoglu",
    email = "dev.csgn@gmail.com",
    url = url("https://github.com/csgn")
  )
)

/* Test settings */
ThisBuild / testFrameworks += new TestFramework("munit.Framework")

/* Scalafix settings */
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

/* Compiler settings */
ThisBuild / scalacOptions ++= Seq(
  // "-Wunused",
)

lazy val examples = project
  .in(file("examples"))
  .settings(
    name := "examples",
    moduleName := "examples",
  )
  .settings(
    libraryDependencies ++= {
      Seq(
        cats,
        catsEffect,
        betterMonadicFor,
      )
    }
  )
  .dependsOn(core)
  .enablePlugins(ScalafixPlugin)

lazy val core = project
  .in(file("modules/core"))
  .settings(
    name := "core",
    moduleName := "rom-core",
  )
  .settings(
    libraryDependencies ++= {
      Seq(
        munit,
        cats,
        catsEffect,
        betterMonadicFor,
      )
    }
  )
  .enablePlugins(ScalafixPlugin)

lazy val rom = project
  .in(file("."))
  .settings(
    name := "rom",
  )
  .aggregate(core, examples)
