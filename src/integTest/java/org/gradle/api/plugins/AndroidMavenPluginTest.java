package org.gradle.api.plugins;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

public class AndroidMavenPluginTest {

	private static final File gradleTest = new File("build/gradleTest");

	/**
	 * Test the install task will correctly install the aar in the maven repository.
	 * @throws Exception
	 */
	@Test
	public void testPomIsInstalled() throws Exception {
		String[] gradleVersions = System.getProperty("gradle.versions").split(",");
		for( String version : gradleVersions ) {
			validateRepo(new File(gradleTest, version + "/simple"));
		}
	}

	private void validateRepo(File projectFolder) {
		File repo = new File(projectFolder, "build/repo/org/test/simple");
		assertTrue(repo.getPath() + " does not exists", repo.exists());

		File metadata = new File(repo, "maven-metadata.xml");
		File metadataMd5 = new File(repo, "maven-metadata.xml.md5");
		File metadataSha1 = new File(repo, "maven-metadata.xml.sha1");
		assertTrue(metadata.getPath() + " does not exists", metadata.exists());
		assertTrue(metadataMd5.getPath() + " does not exists", metadataMd5.exists());
		assertTrue(metadataSha1.getPath() + " does not exists", metadataSha1.exists());

		File aar = new File(repo, "1.0/simple-1.0.aar");
		File aarMd5 = new File(repo, "1.0/simple-1.0.aar.md5");
		File aarSha1 = new File(repo, "1.0/simple-1.0.aar.sha1");
		assertTrue(aar.getPath() + " does not exists", aar.exists());
		assertTrue(aarMd5.getPath() + " does not exists", aarMd5.exists());
		assertTrue(aarSha1.getPath() + " does not exists", aarSha1.exists());

		File pom = new File(repo, "1.0/simple-1.0.pom");
		File pomMd5 = new File(repo, "1.0/simple-1.0.pom.md5");
		File pomSha1 = new File(repo, "1.0/simple-1.0.pom.sha1");
		assertTrue(pom.getPath() + " does not exists", pom.exists());
		assertTrue(pomMd5.getPath() + " does not exists", pomMd5.exists());
		assertTrue(pomSha1.getPath() + " does not exists", pomSha1.exists());
	}
}
