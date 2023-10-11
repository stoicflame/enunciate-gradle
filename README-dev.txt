To test this plugin, build it, and add this to the project it should be tested with:

buildscript {
	dependencies {
		classpath files("/path/to/output/enunciate-gradle-2.17.0.jar")
		classpath "com.webcohesion.enunciate:enunciate-top:2.17.0"
	} 
}

apply plugin: "enunciate"

...
