organization := "no.java.ems"

name := "ems-video"

description := "Site for updating videos"

scalaVersion := "2.11.8"

val circeVersion = "0.4.1"

libraryDependencies += "net.databinder" %% "unfiltered-jetty" % "0.8.4"
libraryDependencies += "net.databinder" %% "unfiltered-filter" % "0.8.4"
libraryDependencies += "net.databinder.dispatch" %% "dispatch-core" % "0.11.2"

libraryDependencies += "io.argonaut" %% "argonaut" % "6.1"
