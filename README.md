# Enunciate Gradle Plugin

Gradle plugin for generating documentation for Java-based Web service API projects using [Enunciate](http://enunciate.webcohesion.com).

## Issues

If you experience any issues with the plugin, please file and issue at github, https://github.com/stoicflame/enunciate-gradle/issues

## Applying the plugin

From version *2.17*, the plugin will be compiled with Java 17, on Gradle 6+.

So your build process needs to run on Java 17 with a 6+ version of Gradle.


### Usage

Use the plugin mechanism to load the plugin:

```
plugins {
  id "com.webcohesion.enunciate" version "2.18.0"
}
```

## Enunciate Task

The plugin adds a single task which runs Enunciate. It uses the main sources and the compileClasspath (or specified) classpath of the project when invoking Enunciate.

The plugin also adds a configuration named `enunciate` where you can add additional enunciate module dependencies.

The task can be configured with these options:

Option | Default value | Description
-------|---------------|-------------
buildDirName | enunciate | Enunciate's working directory, relative to Gradle's `buildDir`
configFileName | src/main/enunciate/enunciate.xml	| Location of enunciate configuration file.
extraJavacArgs | [] | Javac arguments. Arguments for source, target, encoding and bootstrapClasspath are added automatically.

From version *2.11* there is this additional option:

Option | Default value | Description
-------|---------------|-------------
classpathConfigName | compileClasspath | This configuration's files are passed on as classpath to the Enunciate compilation.

The task can be further configured with these methods:

Method | Operation
-------|----------------
exclude(pattern) | Excludes files matching pattern from sources.
include(pattern) | Includes files matching pattern from sources (implicitly excluding those that do not match).
export(artifactId, destination) | Defines an artifact export (see https://github.com/stoicflame/enunciate/wiki/Artifacts). If the destination is a folder, the output is generated there. Otherwise the output is put in a zip file.
sourcepath(Object...) | Adds additional source paths. The given paths are evaluated as per Project.files(Object...). 

## Example usage

This will generate the documentation artifact and copy it to the dist/docs/api folder.

```
apply plugin: "com.webcohesion.enunciate"

dependencies {
  // From version 1.11 optionally add extra modules with:
  // enunciate "group:artifact:version"
}

tasks.enunciate {
  File enunciateDistDir = file("dist/docs/api")
  doFirst {
    enunciateDistDir.deleteDir()
    enunciateDistDir.mkdirs()
  }
  export("docs", enunciateDistDir)
}
```

## Integrating Optional Modules

```
buildscript {
  dependencies {
    classpath "com.webcohesion.enunciate:enunciate-lombok:2.18.0"
  }
}

plugins {
  id "com.webcohesion.enunciate" version "2.18.0"
}
```
