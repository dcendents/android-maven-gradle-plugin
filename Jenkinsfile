#!/usr/bin/env groovy

node {
	stage 'fetch code' {
		checkout scm
	}
	
	stage 'build' {
		wrap([$class: 'TimestamperBuildWrapper']) {
			wrap([$class: 'AnsiColorBuildWrapper']) {
				env.JAVA_HOME="${tool 'jdk-8-latest'}"
				env.PATH="${env.JAVA_HOME}/bin:${env.PATH}"
				sh 'java -version'
				sh './gradlew clean build --info --stacktrace --no-daemon'
			}
		}
	}

	stage 'results' {
		junit 'build/test-results/**/*.xml'
		step([$class: 'JacocoPublisher', classPattern: 'build/classes/main', execPattern: 'build/**/**.exec'])
	}
}
