import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys._
import sbtassembly._
import sbtassembly.AssemblyPlugin._
import sbtunidoc.Plugin._
import spray.revolver.RevolverPlugin._

object HyperScalaBuild extends Build {
  import Dependencies._

  val baseSettings = Defaults.coreDefaultSettings ++ Seq(
    version := "0.9.3",
    organization := "org.hyperscala",
    scalaVersion := "2.11.4",
    libraryDependencies ++= Seq(
      powerScalaReflect,
      powerScalaHierarchy,
      powerScalaProperty,
      jdom,
      jaxen,
      htmlcleaner,
      musterJawn,
      musterJackson,
      akkaActors,
      scalaTest
    ),
    fork := true,
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
//    scalacOptions in (Compile,doc) ++= Seq("-groups", "-implicits", "-diagrams", "-diagrams-dot-restart", "500"),
    resolvers ++= Seq("Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
                      "twitter-repo" at "http://maven.twttr.com",
                      "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"),
    publishTo <<= version {
      (v: String) =>
        val nexus = "https://oss.sonatype.org/"
        if (v.trim.endsWith("SNAPSHOT"))
          Some("snapshots" at nexus + "content/repositories/snapshots")
        else
          Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    publishArtifact in Test := false,
    pomExtra := <url>http://hyperscala.org</url>
      <licenses>
        <license>
          <name>BSD-style</name>
          <url>http://www.opensource.org/licenses/bsd-license.php</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <developerConnection>scm:https://github.com/darkfrog26/hyperscala.git</developerConnection>
        <connection>scm:https://github.com/darkfrog26/hyperscala.git</connection>
        <url>https://github.com/darkfrog26/hyperscala</url>
      </scm>
      <developers>
        <developer>
          <id>darkfrog</id>
          <name>Matt Hicks</name>
          <url>http://matthicks.com</url>
        </developer>
      </developers>
  )

  private def createSettings(_name: String) = baseSettings ++ assemblySettings ++ Seq(name := _name)

  lazy val root = Project("root", file("."), settings = unidocSettings ++ createSettings("hyperscala-root"))
    .settings(publishArtifact in Compile := false, publishArtifact in Test := false)
    .aggregate(core, html, javascript, svg, web, service, jquery, connect, realtime, ui, ux, bootstrap, generator, hello, examples, numberGuess, site)
  lazy val core = Project("core", file("core"), settings = createSettings("hyperscala-core"))
    .settings(libraryDependencies ++= Seq(outrNetCore, argonaut))
  lazy val html = Project("html", file("html"), settings = createSettings("hyperscala-html"))
    .dependsOn(core)
  lazy val svg = Project("svg", file("svg"), settings = createSettings("hyperscala-svg"))
    .dependsOn(html)
  lazy val javascript = Project("javascript", file("javascript"), settings = createSettings("hyperscala-javascript"))
    .dependsOn(html)
  lazy val web = Project("web", file("web"), settings = createSettings("hyperscala-web"))
    .dependsOn(html, javascript, svg)
    .settings(libraryDependencies ++= Seq(uaDetector, scalaSwing, commonsCodec))
  lazy val service = Project("service", file("service"), settings = createSettings("hyperscala-service"))
    .dependsOn(web)
    .settings(libraryDependencies ++= Seq(powerScalaJson))
  lazy val snapSVG = Project("snapsvg", file("snapsvg"), settings = createSettings("hyperscala-snapsvg"))
    .dependsOn(web)
  lazy val jquery = Project("jquery", file("jquery"), settings = createSettings("hyperscala-jquery"))
    .dependsOn(web)
  lazy val connect = Project("connect", file("connect"), settings = createSettings("hyperscala-connect"))
    .dependsOn(jquery)
  lazy val realtime = Project("realtime", file("realtime"), settings = createSettings("hyperscala-realtime"))
    .dependsOn(web, jquery, connect)
  lazy val ui = Project("ui", file("ui"), settings = createSettings("hyperscala-ui"))
    .dependsOn(web, realtime, jquery)
  lazy val ux = Project("ux", file("ux"), settings = createSettings("hyperscala-ux"))
    .dependsOn(web, realtime, jquery, ui)
  lazy val bootstrap = Project("bootstrap", file("bootstrap"), settings = createSettings("hyperscala-bootstrap"))
    .dependsOn(ui)
  lazy val generator = Project("generator", file("generator"), settings = createSettings("hyperscala-generator"))
    .settings(publishArtifact := false)
  // Examples and Site
  lazy val hello = Project("hello", file("hello"), settings = createSettings("hyperscala-hello") ++ Revolver.settings)
    .dependsOn(web)
    .settings(libraryDependencies ++= Seq(outrNetServlet, outrNetJetty))
    .settings(mainClass := Some("org.hyperscala.hello.HelloSite"))
  lazy val examples = Project("examples", file("examples"), settings = createSettings("hyperscala-examples"))
    .dependsOn(web, ui, ux, bootstrap, snapSVG, connect, hello)
    .settings(libraryDependencies ++= Seq(outrNetServlet))
  lazy val numberGuess = Project("numberguess", file("numberguess"), settings = createSettings("hyperscala-numberguess") ++ Revolver.settings)
    .dependsOn(ui)
    .settings(libraryDependencies ++= Seq(outrNetServlet, outrNetJetty))
    .settings(mainClass := Some("org.hyperscala.numberguess.NumberGuessSite"))
  lazy val site = Project("site", file("site"), settings = createSettings("hyperscala-site") ++ Revolver.settings)
    .settings(assemblyJarName in assembly := s"hyperscala-${version.value}.jar", assemblyMergeStrategy in assembly <<= (assemblyMergeStrategy in assembly) {
      case old => {
        case PathList("about.html") => MergeStrategy.first
        case PathList("META-INF", "jdom-info.xml") => MergeStrategy.first
        case x => old(x)
      }
    }, mainClass in assembly := Some("org.hyperscala.site.HyperscalaSite"), assemblyExcludedJars in assembly := {
        val cp = (fullClasspath in assembly).value
        cp filter {_.data.getName == "annotations-2.0.1.jar"}
    })
    .settings(mainClass := Some("org.hyperscala.site.HyperscalaSite"))
    .settings(libraryDependencies ++= Seq(outrNetServlet, outrNetJetty, githubCore))
    .dependsOn(examples)
}
