# Enunciate Gradle Plugin

Gradle plugin for generating REST/WS documentation for Java projects using Enunciate.

## Issues

If you experience any issues with the plugin, please file and issue at github, https://github.com/stoicflame/enunciate-gradle/issues

## Applying the plugin

I still need to figure out https://plugins.gradle.org deployment.

Until this is done, you can clone this project, and run the command

```
gradlew jar
```

This will generate `build\libs\enunciate-gradle-0.9.0.jar` which can be included in a build file like this:

```
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath files("PATH_TO_YOUR_WORKSPACE/enunciate-gradle/build/libs/enunciate-gradle-0.9.0.jar")
    classpath "com.webcohesion.enunciate:enunciate-core:2.0.0-RC.1"
    classpath "com.webcohesion.enunciate:enunciate-docs:2.0.0-RC.1"
    classpath "com.webcohesion.enunciate:enunciate-jaxb:2.0.0-RC.1"
    classpath "com.webcohesion.enunciate:enunciate-jaxrs:2.0.0-RC.1"
	}
}
apply plugin: "enunciate"
```

The above allows generation of documentation for a REST based XML API.
You may have to add other modules for other API types.


## Enunciate Task

The plugin adds a single task which runs Enunciate.
It uses the main sources and compile classpath of the project when invoking Enunciate.

The task can be configured with these options:

Option | Default value | Description
-------|---------------|-------------
buildDirName	| enunciate							| Enunciate's tmp dir, relative to Gradle's buildDir
configFileName	| src/main/enunciate/enunciate.xml	| Location of enunciate configuration file.
extraJavacArgs	| []								| Javac arguments. Arguments for source, target, encoding and bootstrapClasspath are added automatically.

The task can be further configured with these methods:

Method | Operation
-------|----------------
exclude(pattern) | Excludes files matching pattern from sources.
include(pattern) | Includes files matching pattern from sources (implicitly excluding those that do not match).
export(artifactId, destination) | Defines an artifact export (see https://github.com/stoicflame/enunciate/wiki/Artifacts). If the destination is a folder, the output is generated there. Otherwise the output is put in an archive.


## Example usage

This will generate the documentation artifact and copy it to the dist/docs/api folder.

```
apply plugin: "enunciate"
	   
tasks.enunciate {
  File enunciateDistDir = file("dist/docs/api")
  doFirst {
    enunciateDistDir.deleteDir()
    enunciateDistDir.mkdirs()
  }
  export("docs", enunciateDistDir)
}
```

