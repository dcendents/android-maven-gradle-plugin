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

import org.gradle.api.Task;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.maven.Conf2ScopeMapping;
import org.gradle.api.artifacts.maven.Conf2ScopeMappingContainer;
import org.gradle.api.artifacts.maven.MavenResolver;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.Project;
import org.gradle.api.tasks.Upload;
import org.gradle.testfixtures.ProjectBuilder;

import java.io.File;
import java.util.Set;

import static org.gradle.util.WrapUtil.toSet;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class AndroidMavenPluginTest {
    private final Project project = ProjectBuilder.builder().build();

    @org.junit.Test
    public void addsConventionToProject() {
        project.getPluginManager().apply(AndroidMavenPlugin.class);

        assertThat(project.getConvention().getPlugin(MavenPluginConvention.class), notNullValue());
    }

    @org.junit.Test
    public void defaultConventionValues() {
        project.getPluginManager().apply(AndroidMavenPlugin.class);

        MavenPluginConvention convention = project.getConvention().getPlugin(MavenPluginConvention.class);
        assertThat(convention.getMavenPomDir(), equalTo(new File(project.getBuildDir(), "poms")));
        assertThat(convention.getConf2ScopeMappings(), notNullValue());
    }

    @org.junit.Test
    public void applyWithWarPlugin() {
        project.getPluginManager().apply(WarPlugin.class);
        project.getPluginManager().apply(AndroidMavenPlugin.class);

        assertHasConfigurationAndMapping(project, WarPlugin.PROVIDED_COMPILE_CONFIGURATION_NAME, Conf2ScopeMappingContainer.PROVIDED,
                AndroidMavenPlugin.PROVIDED_COMPILE_PRIORITY);
        assertHasConfigurationAndMapping(project, WarPlugin.PROVIDED_RUNTIME_CONFIGURATION_NAME, Conf2ScopeMappingContainer.PROVIDED,
                AndroidMavenPlugin.PROVIDED_RUNTIME_PRIORITY);

        Task task = project.getTasks().getByName(AndroidMavenPlugin.INSTALL_TASK_NAME);
        Set dependencies = task.getTaskDependencies().getDependencies(task);
        assertThat(dependencies, equalTo((Set) toSet(project.getTasks().getByName(WarPlugin.WAR_TASK_NAME))));
    }

    private static void assertHasConfigurationAndMapping(final Project project, final String configurationName, final String scope, final int priority) {
        Conf2ScopeMappingContainer scopeMappingContainer = project.getConvention().getPlugin(MavenPluginConvention.class).getConf2ScopeMappings();
        ConfigurationContainer configurationContainer = project.getConfigurations();
        Conf2ScopeMapping mapping = scopeMappingContainer.getMappings().get(configurationContainer.getByName(configurationName));
        assertThat(mapping.getScope(), equalTo(scope));
        assertThat(mapping.getPriority(), equalTo(priority));
    }

    @org.junit.Test
    public void applyWithJavaPlugin() {
        project.getPluginManager().apply(JavaPlugin.class);
        project.getPluginManager().apply(AndroidMavenPlugin.class);

        assertHasConfigurationAndMapping(project, JavaPlugin.COMPILE_CONFIGURATION_NAME, Conf2ScopeMappingContainer.COMPILE,
                AndroidMavenPlugin.COMPILE_PRIORITY);
        assertHasConfigurationAndMapping(project, JavaPlugin.RUNTIME_CONFIGURATION_NAME, Conf2ScopeMappingContainer.RUNTIME,
                AndroidMavenPlugin.RUNTIME_PRIORITY);
        assertHasConfigurationAndMapping(project, JavaPlugin.TEST_COMPILE_CONFIGURATION_NAME, Conf2ScopeMappingContainer.TEST,
                AndroidMavenPlugin.TEST_COMPILE_PRIORITY);
        assertHasConfigurationAndMapping(project, JavaPlugin.TEST_RUNTIME_CONFIGURATION_NAME, Conf2ScopeMappingContainer.TEST,
                AndroidMavenPlugin.TEST_RUNTIME_PRIORITY);

        Task task = project.getTasks().getByName(AndroidMavenPlugin.INSTALL_TASK_NAME);
        Set dependencies = task.getTaskDependencies().getDependencies(task);
        assertEquals(dependencies, toSet(project.getTasks().getByName(JavaPlugin.JAR_TASK_NAME)));
    }

    @org.junit.Test
    public void addsAndConfiguresAnInstallTask() {
        project.getPluginManager().apply(JavaPlugin.class);
        project.getPluginManager().apply(AndroidMavenPlugin.class);

        Upload task = project.getTasks().withType(Upload.class).getByName(AndroidMavenPlugin.INSTALL_TASK_NAME);
        assertThat(task.getRepositories().get(0), instanceOf(MavenResolver.class));
    }

    @org.junit.Test
    public void addsConventionMappingToTheRepositoryContainerOfEachUploadTask() {
        project.getPluginManager().apply(JavaPlugin.class);
        project.getPluginManager().apply(AndroidMavenPlugin.class);

        Upload task = project.getTasks().withType(Upload.class).getByName(AndroidMavenPlugin.INSTALL_TASK_NAME);
        MavenRepositoryHandlerConvention convention = new DslObject(task.getRepositories()).getConvention().getPlugin(MavenRepositoryHandlerConvention.class);
        assertThat(convention, notNullValue());

        task = project.getTasks().create("customUpload", Upload.class);
        convention = new DslObject(task.getRepositories()).getConvention().getPlugin(MavenRepositoryHandlerConvention.class);
        assertThat(convention, notNullValue());
    }

    @org.junit.Test
    public void applyWithoutWarPlugin() {
        project.getPluginManager().apply(AndroidMavenPlugin.class);

        assertThat(project.getConfigurations().findByName(WarPlugin.PROVIDED_COMPILE_CONFIGURATION_NAME),
                nullValue());
    }

    @org.junit.Test
    public void applyWithoutJavaPlugin() {
        project.getPluginManager().apply(AndroidMavenPlugin.class);

        assertThat(project.getConfigurations().findByName(JavaPlugin.COMPILE_CONFIGURATION_NAME),
                nullValue());
    }
}
