import bindgen.interface.*
import bindgen.plugin.BindgenMode
import com.indoorvivants.detective.Platform

val Versions = new {
  val Scala = "3.6.3"
}

// This is the main project - a Scala Native binary that invokes
// classes and methods from JVM and the given classpath
lazy val binary =
  project
    .in(file("mod/binary"))
    .enablePlugins(ScalaNativePlugin)
    .dependsOn(jniBindings)
    .settings(
      scalaVersion  := Versions.Scala,
      run / envVars := Map("CLASSPATH" -> depsClasspath.value),
      nativeConfig := {
        val conf = nativeConfig.value
        conf.withLinkingOptions(
          _ ++ Seq(
            "-L" + (detectedJavaHome.value / "lib").toString,
            "-ljli",
            "-L" + (detectedJavaHome.value / "lib/server").toString,
            "-ljvm",
            // Note that adding rpath like this makes the binary non portable,
            // but I don't know how else to fix the @rpath problem this creates
            "-Wl,-rpath",
            (detectedJavaHome.value / "lib").toString,
            "-Wl,-rpath",
            (detectedJavaHome.value / "lib/server").toString,
          ),
        )
      },
    )

// This project is only necessary if you wish to test your native code with
// java/scala code coming from dependencies - add your dependencies here (using normal
// libraryDependencies syntax) and the classpath will be automatically passed to the binary project
lazy val deps =
  project.in(file("mod/deps")).settings(scalaVersion := Versions.Scala)

// This project hosts the bindings to JNI.
// The bindings are already generated so you don't really need to regenerate them,
// unless you have some special needs. It's left here only for reproducibility and
// demonstration purposes - you can easily just remove the module and move its scala files
// into your project.
// Bindings made with sn-bindgen: https://sn-bindgen.indoorvivants.com/quickstart/index.html
lazy val jniBindings = project
  .in(file("mod/jni-bindings"))
  .enablePlugins(ScalaNativePlugin, BindgenPlugin)
  .settings(
    scalaVersion := Versions.Scala,
    bindgenBindings += {
      import Platform.OS.*
      val jni_md = Platform.target.os match {
        case Linux   => "linux"
        case MacOS   => "darwin"
        case Windows => "windows"
      }
      Binding(
        detectedJavaHome.value / "include/jni.h",
        "libjni",
      ).withNoConstructor(Set("JNINativeInterface_"))
        .addClangFlag(
          "-I" + (detectedJavaHome.value / s"include/$jni_md").toString,
        )
    },
  )
  .settings(
    bindgenMode := BindgenMode.Manual(
      scalaDir = (Compile / sourceDirectory).value / "scala" / "generated",
      cDir = (Compile / resourceDirectory).value / "scala-native" / "generated",
    ),
    bindgenBindings := {
      bindgenBindings.value.map(_.withNoLocation(true))
    },
  )

val detectedJavaHome = settingKey[File]("")
ThisBuild / detectedJavaHome := {
  val fromEnv = sys.env.get("JAVA_HOME").map(new File(_))
  val log     = sLog.value
  lazy val fromDiscovery = {
    val disc = (deps / discoveredJavaHomes).value
    disc
      .flatMap { case (v, loc) =>
        scala.util.Try(v.toInt).toOption.map(_ -> loc)
      }
      .toSeq
      .sortBy(_._1)
      .reverse
      .headOption
      .map(_._2)
      .map { loc =>
        log.warn(
          s"Selecting $loc by choosing the highest available version from discoveredJavaHomes (no othe options worked)",
        )
        loc
      }
  }

  (deps / javaHome).value
    .orElse(fromEnv)
    .orElse(fromDiscovery)
    .getOrElse(
      sys.error("No Java home detected!"),
    )
}

val depsClasspath = taskKey[String]("")
ThisBuild / depsClasspath := {
  (deps / Compile / dependencyClasspathAsJars).value
    .map(_.data)
    .mkString(java.io.File.pathSeparator)
}
