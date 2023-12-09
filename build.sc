// mill plugins
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.4.0`
// Run integration tests with mill
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest::0.7.1`

import mill.define.{Command, Task, TaskModule}

import java.nio.file.attribute.PosixFilePermission

import de.tobiasroeser.mill.integrationtest._
import de.tobiasroeser.mill.vcs.version._

import mill.{Agg, PathRef, T}
import mill.define.{Cross, Module, Target}
import mill.modules.Util
import mill.scalalib._
import mill.scalalib.publish._

import os.Path

val millPlatforms = Seq(
  // ("0.11.0","2.13.12"),
  ("0.10.0", "2.13.7"),
  ("0.9.8", "2.13.7")
)

trait MillMDocBaseModule extends ScalaModule with PublishModule {

  def millPlatform: T[String]

  def scalaVersion: T[String]

  def scalaArtefactVersion: T[String] = scalaVersion.map(_.split("\\.").take(2).mkString("."))

  def ivyDeps = super.ivyDeps() ++ Agg(ivy"org.scala-lang:scala-library:${scalaVersion}")

  def artifactSuffix = s"_mill${millPlatform}_${scalaArtefactVersion}"

  def javacOptions = Seq("-source", "8", "-target", "8", "-encoding", "UTF-8")
  def scalacOptions = Seq("-target:jvm-1.8", "-encoding", "UTF-8")

  def publishVersion = VcsVersion.vcsState().format()

  override def artifactName = "mill.mdoc"

  override def pomSettings = T {
    PomSettings(
      description = "Mill module to execute Scalameta MDoc",
      organization = "io.github.quafadas",
      url = "https://github.com/atooni/mill-mdoc",
      licenses = Seq(License.`Apache-2.0`),
      versionControl = VersionControl.github("atooni", "mill-mdoc"),
      developers = Seq(Developer("atooni", "Andreas Gies", "https://github.com/atooni"))
    )
  }
}

object core extends Cross[MillMDocModule](millPlatforms)
trait MillMDocModule extends Cross.Module2[String, String] with MillMDocBaseModule {
  val (millVersion, scalaVersionIn) = (crossValue, crossValue2)
  override def scalaVersion = "2.13.7"
  override def millPlatform: T[String] = millVersion.split("\\.").take(2).mkString(".")

  override def compileIvyDeps = Agg(
    ivy"com.lihaoyi::mill-main:${millVersion}",
    ivy"com.lihaoyi::mill-scalalib:${millVersion}"
  )

  override def generatedSources: Target[Seq[PathRef]] = T {
    val dest = T.dest
    val infoClass =
      s"""// Generated with mill from build.sc
         |package de.wayofquality.mill.mdoc.internal
         |
         |object BuildInfo {
         |  def millMdocVerison = "${publishVersion()}"
         |  def millVersion = "${millPlatform}"
         |}
         |""".stripMargin
    os.write(dest / "BuildInfo.scala", infoClass)
    super.generatedSources() ++ Seq(PathRef(dest))
  }
}

object testsupport extends Cross[TestSupport](millPlatforms)
trait TestSupport extends Cross.Module2[String, String] with MillMDocBaseModule {
  val (millVersion, scalaVersionIn) = (crossValue, crossValue2)
  override def millPlatform: T[String] = millVersion.split("\\.").take(2).mkString(".")

  override def scalaVersion = scalaVersionIn

  override def millSourcePath: Path = super.millSourcePath / os.up

  override def compileIvyDeps = Agg(
    ivy"com.lihaoyi::mill-main:${millVersion}",
    ivy"com.lihaoyi::mill-scalalib:${millVersion}"
  )
  override def artifactName = "mill-mdoc-testsupport"

  override def moduleDeps = Seq(core(millVersion, scalaVersionIn))
}

object itest extends Cross[ItestCross]("0.10.0") with TaskModule {
  override def defaultCommandName(): String = "test"
  // def testCached: T[Seq[TestCase]] = itest(c).testCached
  // def test(args: String*): Command[Seq[TestCase]] = itest(c).test(args: _*)
}

trait ItestCross extends Cross.Module[String] {
  // val (millVersion) = (crossValue)
  // override def millSourcePath: Path = super.millSourcePath / os.up
  // def deps = testVersions.toMap.apply(millVersion)
  override def millTestVersion = T { crossValue }

  // override def scalaVerion = scalaVersionIn
  override def pluginsUnderTest = Seq(core(crossValue, "2.13.7"), testsupport(crossValue, "2.13.7"))

  // override def pluginUnderTestDetails: Task.Sequence[(PathRef, (PathRef, (PathRef, (PathRef, (PathRef, Artifact)))))] =
  //   T.traverse(pluginsUnderTest) { p =>
  //     val jar = p match {
  //       case p: ScoverageModule => p.scoverage.jar
  //       case p => p.jar
  //     }
  //     jar zip (p.sourceJar zip (p.docJar zip (p.pom zip (p.ivy zip p.artifactMetadata))))
  //   }

  // override def perTestResources = T.sources { Seq(generatedSharedSrc()) }
  // def generatedSharedSrc = T {
  //   val scov = deps.scoverageRuntime.dep
  //   os.write(
  //     T.dest / "shared.sc",
  //     s"""import $$ivy.`${scov.module.organization.value}::${scov.module.name.value}:${scov.version}`
  //        |""".stripMargin
  //   )
  //   PathRef(T.dest)
  // }
}
