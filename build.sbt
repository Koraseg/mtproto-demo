name := "MProtoTestTask"

version := "0.1"

scalaVersion := "2.12.7"

resolvers += "Sonatype Public" at "https://oss.sonatype.org/content/groups/public/"
resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"

libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.17"

libraryDependencies += "org.scodec" %% "scodec-core" % "1.10.3"

libraryDependencies += "org.scodec" %% "scodec-protocols" % "1.0.2"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"

