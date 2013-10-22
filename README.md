android-maven-plugin
====================

Maven plugin for gradle compatible with android library projects


How to use:

 - Clone the repo.
 - Compile and install the plugin in your local maven repository:
```
gradlew clean build install
```
 - Configure your android project to use this plugin:

```Groovy
buildscript {
	repositories {
		mavenLocal()
	}

	dependencies {
		classpath 'org.gradle.api.plugins:android-maven-plugin:1.0-SNAPSHOT'
	}
}

apply plugin: 'android-library'
apply plugin: 'android-maven'
```
	

