android-maven-plugin
====================

Maven plugin compatible with android projects


How to use:

 - Clone the repo.
 - Compile and install the plugin in your local maven reposaitory:
     gradlew clean build install
 - Configure your android project to use this plugin:
 
buildscript {
	repositories {
		mavenLocal()
	}

	dependencies {
		classpath 'org.gradle.api.plugins:android-maven-plugin:1.0-SNAPSHOT'
	}
}

apply plugin: 'android-library'
