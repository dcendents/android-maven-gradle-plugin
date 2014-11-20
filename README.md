[![Build Status](https://travis-ci.org/dcendents/android-maven-plugin.png)](https://travis-ci.org/dcendents/android-maven-plugin)
Gradle Android Maven plugin
====================

Modification to the standard Maven plugin to be compatible with android-library projects (aar).


Usage
====================

To use the android-maven-plugin, just apply the android-maven plugin in your android-library project.
Also add the plugin classpath dependency to the buildScript.

```Groovy
buildscript {
	repositories {
		mavenCentral()
	}

	dependencies {
		classpath 'com.github.dcendents:android-maven-plugin:1.2'
	}
}

apply plugin: 'com.android.library'
apply plugin: 'android-maven'
```
	
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

