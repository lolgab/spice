import sbt._

object Dependencies {
  object version {
    val profig: String = "3.4.3"
    val scribe: String = "3.10.3"
    val fabric: String = "1.6.1"
    val reactify: String = "4.0.8"
    val cats: String = "3.3.14"
    val fs2: String = "3.2.14"
    val literally: String = "1.1.0"
    val okHttp: String = "4.10.0"
    val undertow: String = "2.2.19.Final"
    val moduload: String = "1.1.6"

    val scalaJSDOM: String = "2.3.0"

    val scalaTest: String = "3.2.13"
    val catsEffectTesting: String = "1.4.0"
  }

  val profig: ModuleID = "com.outr" %% "profig" % version.profig
	val scribe: ModuleID = "com.outr" %% "scribe" % version.scribe
  val scribeSlf4j: ModuleID = "com.outr" %% "scribe-slf4j" % version.scribe
  val scribeCats: ModuleID = "com.outr" %% "scribe-cats" % version.scribe
	val fabricParse: ModuleID = "com.outr" %% "fabric-io" % version.fabric
	val reactify: ModuleID = "com.outr" %% "reactify" % version.reactify
	val catsEffect: ModuleID = "org.typelevel" %% "cats-effect" % version.cats
	val fs2: ModuleID = "co.fs2" %% "fs2-core" % version.fs2
	val literally: ModuleID = "org.typelevel" %% "literally" % version.literally
	val okHttp: ModuleID = "com.squareup.okhttp3" % "okhttp" % version.okHttp
	val undertow: ModuleID = "io.undertow" % "undertow-core" % version.undertow
	val moduload: ModuleID = "com.outr" %% "moduload" % version.moduload

	val scalaTest: ModuleID = "org.scalatest" %% "scalatest" % version.scalaTest % Test
  val catsEffectTesting: ModuleID = "org.typelevel" %% "cats-effect-testing-scalatest" % version.catsEffectTesting % Test
}