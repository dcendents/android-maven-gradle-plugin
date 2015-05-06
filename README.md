[![Build Status](https://travis-ci.org/dcendents/android-maven-gradle-plugin.png)](https://travis-ci.org/dcendents/android-maven-gradle-plugin)

[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-android--maven--plugin-brightgreen.svg?style=flat)](https://android-arsenal.com/details/1/1520)

Gradle Android Maven plugin
====================

Modification to the standard Maven plugin to be compatible with android-library projects (aar).


Usage
====================

To use the android-maven-gradle-plugin, just apply the plugin in your android-library project.
Also add the plugin classpath dependency to the buildScript.

```Groovy
buildscript {
	repositories {
		mavenCentral()
	}

	dependencies {
		classpath 'com.github.dcendents:android-maven-gradle-plugin:1.3'
	}
}

apply plugin: 'com.android.library'
apply plugin: 'com.github.dcendents.android-maven'
```

You can set the maven groupId and version in the script build.gradle:
```Groovy
group = 'com.example'
version = '1.0'
```
	
The artifactId is set in settings.gradle:
```Groovy
rootProject.name = 'artifact'
```

Note: 
- For **multi-projects** build, please refer to issue #9: https://github.com/dcendents/android-maven-gradle-plugin/issues/9
- For proper **exclusion** in the generated maven pom, use both **group and module** notation:
```Groovy
	compile('com.group:lib-module:1.0') {
	        exclude group: 'com.exclusion.group', module: 'module.name'
    }
```
	
Documentation
====================

Please refer to the standard Maven plugin documentation: http://gradle.org/docs/current/userguide/maven_plugin.html


Note on Releases
====================

The following table shows the compatibility between the android-maven-gradle-plugin and gradle versions. It also lists the plugin name to use:

| Plugin Version | Plugin Name | Gradle Version |
| ------------- | ----------- | ----------- |
| 1.0 | android-maven | 1.8+ |
| 1.1 | android-maven | 1.12+ |
| 1.2 | com.github.dcendents.android-maven | 2.2+ |
| 1.3 | com.github.dcendents.android-maven | 2.4+ |


License
====================

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

