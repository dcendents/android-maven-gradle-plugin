/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.plugins;

import org.apache.maven.project.MavenProject;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.maven.Conf2ScopeMappingContainer;
import org.gradle.api.artifacts.maven.MavenPom;
import org.gradle.api.artifacts.maven.MavenResolver;
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier;
import org.gradle.api.internal.artifacts.ModuleInternal;
import org.gradle.api.internal.artifacts.configurations.ConfigurationInternal;
import org.gradle.api.internal.artifacts.dsl.DefaultRepositoryHandler;
import org.gradle.api.internal.artifacts.ivyservice.projectmodule.DefaultProjectPublication;
import org.gradle.api.internal.artifacts.ivyservice.projectmodule.ProjectPublicationRegistry;
import org.gradle.api.internal.artifacts.mvnsettings.LocalMavenRepositoryLocator;
import org.gradle.api.internal.artifacts.mvnsettings.MavenSettingsProvider;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.publication.maven.internal.DefaultDeployerFactory;
import org.gradle.api.publication.maven.internal.DefaultMavenRepositoryHandlerConvention;
import org.gradle.api.publication.maven.internal.MavenFactory;
import org.gradle.api.tasks.Upload;
import org.gradle.configuration.project.ProjectConfigurationActionContainer;
import org.gradle.internal.Factory;
import org.gradle.internal.logging.LoggingManagerInternal;

import javax.inject.Inject;

/**
 * <p>A {@link org.gradle.api.Plugin} which allows project artifacts to be deployed to a Maven repository, or installed
 * to the local Maven cache.</p>
 */
public class AndroidMavenPlugin implements Plugin<ProjectInternal> {
    public static final int COMPILE_PRIORITY = 300;
    public static final int RUNTIME_PRIORITY = 200;
    public static final int TEST_COMPILE_PRIORITY = 150;
    public static final int TEST_RUNTIME_PRIORITY = 100;

    public static final int PROVIDED_COMPILE_PRIORITY = COMPILE_PRIORITY + 100;
    public static final int PROVIDED_RUNTIME_PRIORITY = COMPILE_PRIORITY + 150;

    public static final String INSTALL_TASK_NAME = "install";

    private final Factory<LoggingManagerInternal> loggingManagerFactory;
    private final FileResolver fileResolver;
    private final ProjectPublicationRegistry publicationRegistry;
    private final ProjectConfigurationActionContainer configurationActionContainer;
    private final MavenSettingsProvider mavenSettingsProvider;
    private final LocalMavenRepositoryLocator mavenRepositoryLocator;

    private Project project;

    @Inject
    public AndroidMavenPlugin(final Factory<LoggingManagerInternal> loggingManagerFactory, final FileResolver fileResolver,
                              final ProjectPublicationRegistry publicationRegistry, final ProjectConfigurationActionContainer configurationActionContainer,
                              final MavenSettingsProvider mavenSettingsProvider, final LocalMavenRepositoryLocator mavenRepositoryLocator) {
        this.loggingManagerFactory = loggingManagerFactory;
        this.fileResolver = fileResolver;
        this.publicationRegistry = publicationRegistry;
        this.configurationActionContainer = configurationActionContainer;
        this.mavenSettingsProvider = mavenSettingsProvider;
        this.mavenRepositoryLocator = mavenRepositoryLocator;
    }

    public void apply(final ProjectInternal project) {
        this.project = project;
        project.getPluginManager().apply(BasePlugin.class);

        MavenFactory mavenFactory = project.getServices().get(MavenFactory.class);
        final MavenPluginConvention pluginConvention = addConventionObject(project, mavenFactory);
        final DefaultDeployerFactory deployerFactory = new DefaultDeployerFactory(
                mavenFactory,
                loggingManagerFactory,
                fileResolver,
                pluginConvention,
                project.getConfigurations(),
                pluginConvention.getConf2ScopeMappings(),
                mavenSettingsProvider,
                mavenRepositoryLocator);

        configureUploadTasks(deployerFactory);
        configureUploadArchivesTask();

        PluginContainer plugins = project.getPlugins();

		try {
			Class appPluginClass = Class.forName("com.android.build.gradle.AppPlugin");
			Class libraryPluginClass = Class.forName("com.android.build.gradle.LibraryPlugin");
			Class testPluginClass = Class.forName("com.android.build.gradle.TestPlugin");
			plugins.withType(appPluginClass, new Action<Plugin>() {
				public void execute(final Plugin appPlugin) {
					configureAndroidScopeMappings(project.getConfigurations(), pluginConvention.getConf2ScopeMappings());
				}
			});
			plugins.withType(libraryPluginClass, new Action<Plugin>() {
				public void execute(final Plugin libraryPlugin) {
					configureAndroidScopeMappings(project.getConfigurations(), pluginConvention.getConf2ScopeMappings());
				}
			});
			plugins.withType(testPluginClass, new Action<Plugin>() {
				public void execute(final Plugin testPlugin) {
					configureAndroidScopeMappings(project.getConfigurations(), pluginConvention.getConf2ScopeMappings());
				}
			});
		}
		catch( ClassNotFoundException ex ) { }

        plugins.withType(JavaPlugin.class, new Action<JavaPlugin>() {
            public void execute(final JavaPlugin javaPlugin) {
                configureJavaScopeMappings(project.getConfigurations(), pluginConvention.getConf2ScopeMappings());
            }
        });
        plugins.withType(JavaBasePlugin.class, new Action<JavaBasePlugin>() {
            public void execute(final JavaBasePlugin javaPlugin) {
                configureInstall(project);
            }
        });
        plugins.withType(WarPlugin.class, new Action<WarPlugin>() {
            public void execute(final WarPlugin warPlugin) {
                configureWarScopeMappings(project.getConfigurations(), pluginConvention.getConf2ScopeMappings());
            }
        });
    }

    private void configureUploadTasks(final DefaultDeployerFactory deployerFactory) {
        project.getTasks().withType(Upload.class, new Action<Upload>() {
            public void execute(final Upload upload) {
                RepositoryHandler repositories = upload.getRepositories();
                DefaultRepositoryHandler handler = (DefaultRepositoryHandler) repositories;
                DefaultMavenRepositoryHandlerConvention repositoryConvention = new DefaultMavenRepositoryHandlerConvention(handler, deployerFactory);
                new DslObject(repositories).getConvention().getPlugins().put("maven", repositoryConvention);
            }
        });
    }

    private void configureUploadArchivesTask() {
        configurationActionContainer.add(new Action<Project>() {
            public void execute(final Project project) {
                Upload uploadArchives = project.getTasks().withType(Upload.class).findByName(BasePlugin.UPLOAD_ARCHIVES_TASK_NAME);
                if (uploadArchives == null) {
                    return;
                }

                ConfigurationInternal configuration = (ConfigurationInternal) uploadArchives.getConfiguration();
                ModuleInternal module = configuration.getModule();
                for (MavenResolver resolver : uploadArchives.getRepositories().withType(MavenResolver.class)) {
                    MavenPom pom = resolver.getPom();
                    ModuleVersionIdentifier publicationId = new DefaultModuleVersionIdentifier(
                            pom.getGroupId().equals(MavenProject.EMPTY_PROJECT_GROUP_ID) ? module.getGroup() : pom.getGroupId(),
                            pom.getArtifactId().equals(MavenProject.EMPTY_PROJECT_ARTIFACT_ID) ? module.getName() : pom.getArtifactId(),
                            pom.getVersion().equals(MavenProject.EMPTY_PROJECT_VERSION) ? module.getVersion() : pom.getVersion()
                    );
                    publicationRegistry.registerPublication(project.getPath(), new DefaultProjectPublication(publicationId));
                }
            }
        });
    }

    private static MavenPluginConvention addConventionObject(final ProjectInternal project, final MavenFactory mavenFactory) {
        MavenPluginConvention mavenConvention = new MavenPluginConvention(project, mavenFactory);
        Convention convention = project.getConvention();
        convention.getPlugins().put("maven", mavenConvention);
        return mavenConvention;
    }

    private static void configureAndroidScopeMappings(final ConfigurationContainer configurations, final Conf2ScopeMappingContainer mavenScopeMappings) {
        mavenScopeMappings.addMapping(COMPILE_PRIORITY, configurations.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME),
                Conf2ScopeMappingContainer.COMPILE);
    }

    private static void configureJavaScopeMappings(final ConfigurationContainer configurations, final Conf2ScopeMappingContainer mavenScopeMappings) {
        mavenScopeMappings.addMapping(COMPILE_PRIORITY, configurations.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME),
                Conf2ScopeMappingContainer.COMPILE);
        mavenScopeMappings.addMapping(RUNTIME_PRIORITY, configurations.getByName(JavaPlugin.RUNTIME_CONFIGURATION_NAME),
                Conf2ScopeMappingContainer.RUNTIME);
        mavenScopeMappings.addMapping(TEST_COMPILE_PRIORITY, configurations.getByName(JavaPlugin.TEST_COMPILE_CONFIGURATION_NAME),
                Conf2ScopeMappingContainer.TEST);
        mavenScopeMappings.addMapping(TEST_RUNTIME_PRIORITY, configurations.getByName(JavaPlugin.TEST_RUNTIME_CONFIGURATION_NAME),
                Conf2ScopeMappingContainer.TEST);
    }

    private static void configureWarScopeMappings(final ConfigurationContainer configurations, final Conf2ScopeMappingContainer mavenScopeMappings) {
        mavenScopeMappings.addMapping(PROVIDED_COMPILE_PRIORITY, configurations.getByName(WarPlugin.PROVIDED_COMPILE_CONFIGURATION_NAME),
                Conf2ScopeMappingContainer.PROVIDED);
        mavenScopeMappings.addMapping(PROVIDED_RUNTIME_PRIORITY, configurations.getByName(WarPlugin.PROVIDED_RUNTIME_CONFIGURATION_NAME),
                Conf2ScopeMappingContainer.PROVIDED);
    }

    private static void configureInstall(final Project project) {
        Upload installUpload = project.getTasks().create(INSTALL_TASK_NAME, Upload.class);
        Configuration configuration = project.getConfigurations().getByName(Dependency.ARCHIVES_CONFIGURATION);
        installUpload.setConfiguration(configuration);
        MavenRepositoryHandlerConvention repositories = new DslObject(installUpload.getRepositories()).getConvention().getPlugin(MavenRepositoryHandlerConvention.class);
        repositories.mavenInstaller();
        installUpload.setDescription("Installs the 'archives' artifacts into the local Maven repository.");
    }
}

