package org.gradle.api.plugins;

import java.io.File;

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.junit.Test;

import static org.junit.Assert.*;

public class AndroidMavenPluginTest {

	private static final File simpleProject = new File("build/resources/test/projects/simple");
	
	/**
	 * Test the install task will correctly install the aar in the maven repository.
	 * @throws Exception
	 */
	@Test
	public void testPomIsInstalled() throws Exception {
		ProjectConnection connection = GradleConnector.newConnector()
				.forProjectDirectory(simpleProject)
				.connect();

		try {
			connection.newBuild()
			// Should try to pass the same arguments as received!?!
			.withArguments()
			.forTasks("clean", "build", "install")
			.run();
		} 
		finally {
			connection.close();
		}
		
		File repo = new File(simpleProject, "build/repo/org/test/simple");
		assertTrue(repo.exists());
		
		File metadata = new File(repo, "maven-metadata.xml");
		File metadataMd5 = new File(repo, "maven-metadata.xml.md5");
		File metadataSha1 = new File(repo, "maven-metadata.xml.sha1");
		assertTrue(metadata.exists());
		assertTrue(metadataMd5.exists());
		assertTrue(metadataSha1.exists());
		
		File aar = new File(repo, "1.0/simple-1.0.aar");
		File aarMd5 = new File(repo, "1.0/simple-1.0.aar.md5");
		File aarSha1 = new File(repo, "1.0/simple-1.0.aar.sha1");
		assertTrue(aar.exists());
		assertTrue(aarMd5.exists());
		assertTrue(aarSha1.exists());
		
		File pom = new File(repo, "1.0/simple-1.0.pom");
		File pomMd5 = new File(repo, "1.0/simple-1.0.pom.md5");
		File pomSha1 = new File(repo, "1.0/simple-1.0.pom.sha1");
		assertTrue(pom.exists());
		assertTrue(pomMd5.exists());
		assertTrue(pomSha1.exists());
	}
}
