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
  ("0.11.0","2.13.12"),
  ("0.10.0","2.13.12"),
  ("0.9.8","2.13.12")
)

object core extends Cross[MillMDocModule](millPlatforms)
trait MillMDocModule extends Cross.Module2[String, String] with ScalaModule with PublishModule {
  val (millVersion, scalaVersionIn) = (crossValue, crossValue2)
  def millPlatform: String = millVersion.take(4).replace(".", "")
  //def deps: Deps = millPlatforms.toMap.apply(millPlatform)
  override def scalaVersion = scalaVersionIn  // override def ivyDeps = Agg(deps.scalaLibrary)
  // override def artifactSuffix = s"_mill${deps.millPlatform}_${artifactScalaVersion()}"
  override def artifactName = "de.wayofquality.blended.mill.mdoc"
  def publishVersion =  VcsVersion.vcsState().format()
    override def compileIvyDeps = Agg(
    ivy"com.lihaoyi::mill-main:${millVersion}",
    ivy"com.lihaoyi::mill-scalalib:${millVersion}"
  )
  override def javacOptions = Seq("-source", "17", "-target", "17", "-encoding", "UTF-8")
  override def scalacOptions = Seq("17", "-encoding", "UTF-8")
  override def pomSettings = T {
    PomSettings(
      description = "Mill module to execute Scalameta MDoc",
      organization = "de.wayofquality.blended",
      url = "https://github.com/atooni/mill-mdoc",
      licenses = Seq(License.`Apache-2.0`),
      versionControl = VersionControl.github("atooni", "mill-mdoc"),
      developers = Seq(Developer("atooni", "Andreas Gies", "https://github.com/atooni"))
    )
  }
    override def generatedSources: Target[Seq[PathRef]] = T {
    val dest = T.dest
    val infoClass =
      s"""// Generated with mill from build.sc
         |package de.wayofquality.mill.mdoc.internal
         |
         |object BuildInfo {
         |  def millMdocVerison = "${publishVersion()}"
         |  def millVersion = "${millPlatform.take(4).replace(".", "")}"
         |}
         |""".stripMargin
    os.write(dest / "BuildInfo.scala", infoClass)
    super.generatedSources() ++ Seq(PathRef(dest))
  }

}

object itest extends Cross[Itest](millPlatforms)

trait Itest extends Cross.Module2[String, String] with MillIntegrationTestModule {
  val (millVersion, scalaVersionIn) = (crossValue, crossValue2)
  //override def millSourcePath: Path = super.millSourcePath / os.up
  //def deps = testVersions.toMap.apply(millVersion)
  override def millTestVersion = T { millVersion }
  override def pluginsUnderTest = Seq(core(millVersion, scalaVersionIn))

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