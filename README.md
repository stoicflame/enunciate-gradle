# Enunciate Gradle Plugin

Gradle plugin for generating REST/WS documentation for Java projects using [Enunciate](http://enunciate.webcohesion.com).

## Issues

If you experience any issues with the plugin, please file and issue at github, https://github.com/stoicflame/enunciate-gradle/issues

## Applying the plugin

```
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath "gradle.plugin.com.webcohesion.enunciate:enunciate-gradle:0.9.0")
    classpath "com.webcohesion.enunciate:enunciate-core:2.0.0"
    classpath "com.webcohesion.enunciate:enunciate-docs:2.0.0"
    classpath "com.webcohesion.enunciate:enunciate-jaxb:2.0.0"
    classpath "com.webcohesion.enunciate:enunciate-jaxrs:2.0.0"
  }
}
apply plugin: "com.webcohesion.enunciate"
```

The above allows generation of documentation for a REST-based XML API. You may want to include [other Enunciate modules](https://github.com/stoicflame/enunciate/wiki/Modules) to handle JSON, to generate client-side libraries, or to generate Swagger documentation. For information about the other modules available, see [Enunciate Modules](https://github.com/stoicflame/enunciate/wiki/Modules).


## Enunciate Task

The plugin adds a single task which runs Enunciate. It uses the main sources and compile classpath of the project when invoking Enunciate.

The task can be configured with these options:

Option | Default value | Description
-------|---------------|-------------
buildDirName | enunciate | Enunciate's working directory, relative to Gradle's `buildDir`
configFileName | src/main/enunciate/enunciate.xml	| Location of enunciate configuration file.
extraJavacArgs | [] | Javac arguments. Arguments for source, target, encoding and bootstrapClasspath are added automatically.

The task can be further configured with these methods:

Method | Operation
-------|----------------
exclude(pattern) | Excludes files matching pattern from sources.
include(pattern) | Includes files matching pattern from sources (implicitly excluding those that do not match).
export(artifactId, destination) | Defines an artifact export (see https://github.com/stoicflame/enunciate/wiki/Artifacts). If the destination is a folder, the output is generated there. Otherwise the output is put in a zip file.

## Example usage

This will generate the documentation artifact and copy it to the dist/docs/api folder.

```
apply plugin: "com.webcohesion.enunciate"

tasks.enunciate {
  File enunciateDistDir = file("dist/docs/api")
  doFirst {
    enunciateDistDir.deleteDir()
    enunciateDistDir.mkdirs()
  }
  export("docs", enunciateDistDir)
}
```
