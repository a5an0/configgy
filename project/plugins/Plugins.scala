import sbt._

class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
  val scalaTools = "scala-tools.org" at "http://scala-tools.org/repo-releases/"
  val defaultProject = "com.twitter" % "standard-project" % "0.7.17"
  val twitter = "twitter.com" at "http://maven.twttr.com"
}
