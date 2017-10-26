# Enunciate Gradle Plugin

Gradle plugin for generating documentation for Java-based Web service API projects using [Enunciate](http://enunciate.webcohesion.com).

## Issues

If you experience any issues with the plugin, please file and issue at github, https://github.com/stoicflame/enunciate-gradle/issues

## Applying the plugin


### Gradle 2.1+

Use the plugin mechanism to load the plugin:

```
plugins {
  id "com.webcohesion.enunciate" version "2.10.0"
}
```

### Older Gradle versions

```
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "gradle.plugin.com.webcohesion.enunciate:enunciate-gradle:2.10.0"
  }
}
```

To apply the plugin:

```
apply plugin: "com.webcohesion.enunciate"
```

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
sourcepath(Object...) | Adds additional source paths. The given paths are evaluated as per Project.files(Object...). 
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

As a workaround for enunciate-gradle#5 you might want to add:

```
tasks.enunciate.doFirst { project.delete("build/enunciate") }
```
