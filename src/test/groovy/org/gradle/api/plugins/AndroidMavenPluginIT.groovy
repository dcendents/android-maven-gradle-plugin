package org.gradle.api.plugins

import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.*
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Shared
import spock.lang.Unroll

class AndroidMavenPluginIT extends Specification {
	
	@Shared def gradleVersions = "${System.properties['gradle.versions']}".split(",")
	@Shared def androidGradleBuildVersion = "${System.properties['android.gradle.build.version']}"
	@Shared def androidCompileSdkVersion = "${System.properties['android.compile.sdk.version']}"
	@Shared def androidBuildToolsVersion = "${System.properties['android.build.tools.version']}"
	@Shared def jacocoRuntime = "${System.properties['jacoco.runtime']}"
	@Shared def buildDir = "${System.properties['buildDir']}"
	
    @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
	List<File> pluginClasspath
	
	File androidManifest
	File settingsFile
	File gradlePropertiesFile
    File buildFile
	
    def setup() {
		def srcFolder = testProjectDir.newFolder('src')
		def mainFolder = new File(srcFolder, 'main')
		mainFolder.mkdirs()
		androidManifest = new File(mainFolder, 'AndroidManifest.xml')
        settingsFile = testProjectDir.newFile('settings.gradle')
		gradlePropertiesFile = testProjectDir.newFile('gradle.properties')
        buildFile = testProjectDir.newFile('build.gradle')

        def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }

        pluginClasspath = pluginClasspathResource.readLines().collect { new File(it) }
    }

	@Unroll
    def "pom is installed with gradle #gradleVersion"() {
        given:
		androidManifest << """<?xml version="1.0" encoding="utf-8"?>
			<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="org.test.simple" android:versionCode="1" android:versionName="1.0" >
				<application />
			</manifest>
		"""
		
		settingsFile << "rootProject.name = 'simple'"
		
		gradlePropertiesFile << "org.gradle.jvmargs=-javaagent:${jacocoRuntime}=destfile=${buildDir}/jacoco/testKit-${gradleVersion}.exec"
		
        def classpathString = pluginClasspath
            .collect { it.absolutePath.replace('\\', '/') }
            .collect { "'$it'" }
            .join(", ")

		def uri = testProjectDir.root.toURI()
        buildFile << """
			buildscript {
				System.properties['com.android.build.gradle.overrideVersionCheck'] = 'true'
				
				repositories {
					jcenter()
					mavenCentral()
					mavenLocal()
					maven {
						url 'https://maven.google.com'
					}
				}

				dependencies {
					classpath "com.android.tools.build:gradle:${androidGradleBuildVersion}"
					classpath files(${classpathString})
				}
			}
			
			apply plugin: 'com.android.library'
			apply plugin: 'com.github.dcendents.android-maven'

			group = 'org.test'
			version = '1.0'
			
			repositories {
				mavenCentral()
			}

			install {
				repositories {
					// Should be mavenInstaller but cannot find how to override the location
					mavenDeployer {
						repository(url: "${uri}/${gradleVersion}/repo")
					}
				}
			}

			android {
				compileSdkVersion "${androidCompileSdkVersion}"
				buildToolsVersion "${androidBuildToolsVersion}"
			}
			
			dependencies {
				compile 'commons-io:commons-io:2.2'
				
				testCompile	'junit:junit:4.12'
			}

			task runGradleTest {
				dependsOn install
			}
        """
		
        when:
        def result = GradleRunner.create()
			.withGradleVersion(gradleVersion)
            .withProjectDir(testProjectDir.root)
            .withArguments('install')
			.withPluginClasspath()
            .build()

        then:
        result.task(":install").outcome == SUCCESS
		File pom = validateRepo(gradleVersion);
		validateDependencyScope(pom, 'commons-io', 'commons-io', '2.2', 'compile');
		validateDependencyScope(pom, 'junit', 'junit', '4.12', 'test');
		
        where:
        gradleVersion << gradleVersions
    }

	@Unroll
    def "api and implementation is resolved with gradle #gradleVersion"() {
        given:
		androidManifest << """<?xml version="1.0" encoding="utf-8"?>
			<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="org.test.simple" android:versionCode="1" android:versionName="1.0" >
				<application />
			</manifest>
		"""
		
		settingsFile << "rootProject.name = 'simple'"
		
		gradlePropertiesFile << "org.gradle.jvmargs=-javaagent:${jacocoRuntime}=destfile=${buildDir}/jacoco/testKit-${gradleVersion}.exec"
		
        def classpathString = pluginClasspath
            .collect { it.absolutePath.replace('\\', '/') }
            .collect { "'$it'" }
            .join(", ")

		def uri = testProjectDir.root.toURI()
        buildFile << """
			buildscript {
				System.properties['com.android.build.gradle.overrideVersionCheck'] = 'true'
				
				repositories {
					jcenter()
					mavenCentral()
					mavenLocal()
					maven {
						url 'https://maven.google.com'
					}
				}

				dependencies {
					classpath "com.android.tools.build:gradle:${androidGradleBuildVersion}"
					classpath files(${classpathString})
				}
			}
			
			apply plugin: 'com.android.library'
			apply plugin: 'com.github.dcendents.android-maven'

			group = 'org.test'
			version = '1.0'
			
			repositories {
				mavenCentral()
			}

			install {
				repositories {
					// Should be mavenInstaller but cannot find how to override the location
					mavenDeployer {
						repository(url: "${uri}/${gradleVersion}/repo")
					}
				}
			}

			android {
				compileSdkVersion "${androidCompileSdkVersion}"
				buildToolsVersion "${androidBuildToolsVersion}"
			}
			
			dependencies {
				api 'commons-io:commons-io:2.2'
				implementation 'org.codehaus.groovy:groovy-all:2.4.11'
				
				testCompile	'junit:junit:4.12'
			}

			task runGradleTest {
				dependsOn install
			}
        """
		
        when:
        def result = GradleRunner.create()
			.withGradleVersion(gradleVersion)
            .withProjectDir(testProjectDir.root)
            .withArguments('install')
			.withPluginClasspath()
            .build()

        then:
        result.task(":install").outcome == SUCCESS
		File pom = validateRepo(gradleVersion);
		validateDependencyScope(pom, 'commons-io', 'commons-io', '2.2', 'compile');
		validateDependencyScope(pom, 'org.codehaus.groovy', 'groovy-all', '2.4.11', 'runtime');
		validateDependencyScope(pom, 'junit', 'junit', '4.12', 'test');
		
        where:
        gradleVersion << gradleVersions
    }
	
	private File validateRepo(String version) {
		File repo = new File(testProjectDir.root, version + "/repo/org/test/simple")
		assert repo.exists()
		
		File metadata = new File(repo, "maven-metadata.xml");
		File metadataMd5 = new File(repo, "maven-metadata.xml.md5");
		File metadataSha1 = new File(repo, "maven-metadata.xml.sha1");
		assert metadata.exists()
		assert metadataMd5.exists()
		assert metadataSha1.exists()
		
		File aar = new File(repo, "1.0/simple-1.0.aar");
		File aarMd5 = new File(repo, "1.0/simple-1.0.aar.md5");
		File aarSha1 = new File(repo, "1.0/simple-1.0.aar.sha1");
		assert aar.exists()
		assert aarMd5.exists()
		assert aarSha1.exists()
		
		File pom = new File(repo, "1.0/simple-1.0.pom");
		File pomMd5 = new File(repo, "1.0/simple-1.0.pom.md5");
		File pomSha1 = new File(repo, "1.0/simple-1.0.pom.sha1");
		assert pom.exists()
		assert pomMd5.exists()
		assert pomSha1.exists()
		
		//System.out.println(pom.text);
		return pom;
	}
	
	private void validateDependencyScope(File pom, String groupId, String artifactId, String version, String scope) {
		def project = new XmlSlurper().parse(pom);
		def dependencies = project.dependencies;
		def dependency = dependencies.'*'.find { node->
			node.groupId = groupId && node.artifactId == artifactId && node.version == version
		}
		assert dependency.scope == scope;
	}
}
