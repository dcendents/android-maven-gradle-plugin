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
package org.gradle.api.plugins

import org.gradle.api.artifacts.maven.Conf2ScopeMappingContainer
import org.gradle.api.artifacts.maven.MavenResolver
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.tasks.Upload
import org.gradle.test.fixtures.AbstractProjectBuilderSpec

class AndroidMavenPluginTest extends AbstractProjectBuilderSpec {

    void "adds convention to project"() {
        when:
        project.pluginManager.apply(AndroidMavenPlugin)

        then:
        project.convention.getPlugin(MavenPluginConvention)
    }

    void "adds default convention values"() {
        given:
        project.pluginManager.apply(AndroidMavenPlugin)

        when:
        def convention = project.convention.getPlugin(MavenPluginConvention)

        then:
        convention.mavenPomDir == project.file("$project.buildDir/poms")
        convention.conf2ScopeMappings
    }

    void "apply with war plugin"() {
        when:
        project.pluginManager.apply(WarPlugin)
        project.pluginManager.apply(AndroidMavenPlugin)

        then:
        assertHasConfigurationAndMapping(WarPlugin.PROVIDED_COMPILE_CONFIGURATION_NAME, Conf2ScopeMappingContainer.PROVIDED,
            AndroidMavenPlugin.PROVIDED_COMPILE_PRIORITY)
        assertHasConfigurationAndMapping(WarPlugin.PROVIDED_RUNTIME_CONFIGURATION_NAME, Conf2ScopeMappingContainer.PROVIDED,
            AndroidMavenPlugin.PROVIDED_RUNTIME_PRIORITY)

        when:
        def task = project.getTasks().getByName(AndroidMavenPlugin.INSTALL_TASK_NAME)
        def dependencies = task.getTaskDependencies().getDependencies(task)

        then:
        dependencies == [project.tasks.getByName(WarPlugin.WAR_TASK_NAME)] as Set
    }

    private void assertHasConfigurationAndMapping(String configurationName, String scope, int priority) {
        def scopeMappingContainer = project.convention.getPlugin(MavenPluginConvention).conf2ScopeMappings
        def configurationContainer = project.configurations
        def mapping = scopeMappingContainer.mappings.get(configurationContainer.getByName(configurationName))

        assert mapping?.scope == scope
        assert mapping?.priority == priority
    }

    void "apply with Java plugin"() {
        when:
        project.pluginManager.apply(JavaPlugin)
        project.pluginManager.apply(AndroidMavenPlugin)

        then:
        assertHasConfigurationAndMapping(JavaPlugin.COMPILE_CONFIGURATION_NAME, Conf2ScopeMappingContainer.COMPILE,
            AndroidMavenPlugin.COMPILE_PRIORITY)
        assertHasConfigurationAndMapping(JavaPlugin.RUNTIME_CONFIGURATION_NAME, Conf2ScopeMappingContainer.RUNTIME,
            AndroidMavenPlugin.RUNTIME_PRIORITY)
        assertHasConfigurationAndMapping(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, Conf2ScopeMappingContainer.RUNTIME,
            AndroidMavenPlugin.RUNTIME_PRIORITY)
        assertHasConfigurationAndMapping(JavaPlugin.TEST_COMPILE_CONFIGURATION_NAME, Conf2ScopeMappingContainer.TEST,
            AndroidMavenPlugin.TEST_COMPILE_PRIORITY)
        assertHasConfigurationAndMapping(JavaPlugin.TEST_RUNTIME_CONFIGURATION_NAME, Conf2ScopeMappingContainer.TEST,
            AndroidMavenPlugin.TEST_RUNTIME_PRIORITY)
        assertHasConfigurationAndMapping(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, Conf2ScopeMappingContainer.TEST,
            AndroidMavenPlugin.TEST_RUNTIME_PRIORITY)

        when:
        def task = project.tasks.getByName(AndroidMavenPlugin.INSTALL_TASK_NAME)
        def dependencies = task.taskDependencies.getDependencies(task)

        then:
        assert dependencies == [project.tasks.getByName(JavaPlugin.JAR_TASK_NAME)] as Set
    }

    void "apply with Java library plugin"() {
        when:
        project.pluginManager.apply(JavaLibraryPlugin)
        project.pluginManager.apply(AndroidMavenPlugin)

        then:
        assertHasConfigurationAndMapping(JavaPlugin.API_CONFIGURATION_NAME, Conf2ScopeMappingContainer.COMPILE,
            AndroidMavenPlugin.COMPILE_PRIORITY)
        assertHasConfigurationAndMapping(JavaPlugin.COMPILE_CONFIGURATION_NAME, Conf2ScopeMappingContainer.COMPILE,
            AndroidMavenPlugin.COMPILE_PRIORITY)
        assertHasConfigurationAndMapping(JavaPlugin.RUNTIME_CONFIGURATION_NAME, Conf2ScopeMappingContainer.RUNTIME,
            AndroidMavenPlugin.RUNTIME_PRIORITY)
        assertHasConfigurationAndMapping(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, Conf2ScopeMappingContainer.RUNTIME,
            AndroidMavenPlugin.RUNTIME_PRIORITY)
        assertHasConfigurationAndMapping(JavaPlugin.TEST_COMPILE_CONFIGURATION_NAME, Conf2ScopeMappingContainer.TEST,
            AndroidMavenPlugin.TEST_COMPILE_PRIORITY)
        assertHasConfigurationAndMapping(JavaPlugin.TEST_RUNTIME_CONFIGURATION_NAME, Conf2ScopeMappingContainer.TEST,
            AndroidMavenPlugin.TEST_RUNTIME_PRIORITY)
        assertHasConfigurationAndMapping(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, Conf2ScopeMappingContainer.TEST,
            AndroidMavenPlugin.TEST_RUNTIME_PRIORITY)

        when:
        def task = project.tasks.getByName(AndroidMavenPlugin.INSTALL_TASK_NAME)
        def dependencies = task.taskDependencies.getDependencies(task)

        then:
        assert dependencies == [project.tasks.getByName(JavaPlugin.JAR_TASK_NAME)] as Set
    }

    void "adds and configures an install task"() {
        when:
        project.pluginManager.apply(JavaPlugin)
        project.pluginManager.apply(AndroidMavenPlugin)

        then:
        def task = project.tasks.withType(Upload).getByName(AndroidMavenPlugin.INSTALL_TASK_NAME)
        task.repositories[0] instanceof MavenResolver
    }

    void "adds convention mapping to the repository container of each upload task"() {
        given:
        project.pluginManager.apply(JavaPlugin)
        project.pluginManager.apply(AndroidMavenPlugin)

        when:
        def task = project.tasks.withType(Upload).getByName(AndroidMavenPlugin.INSTALL_TASK_NAME)
        def convention = new DslObject(task.repositories).convention.getPlugin(MavenRepositoryHandlerConvention)

        then:
        convention != null

        when:
        task = project.tasks.create("customUpload", Upload)
        convention = new DslObject(task.repositories).convention.getPlugin(MavenRepositoryHandlerConvention)

        then:
        convention != null
    }

    void "apply without War plugin"() {
        when:
        project.pluginManager.apply(AndroidMavenPlugin)

        then:
        project.configurations.findByName(WarPlugin.PROVIDED_COMPILE_CONFIGURATION_NAME) == null
    }

    void "apply without Java plugin"() {
        when:
        project.pluginManager.apply(AndroidMavenPlugin)

        then:
        project.configurations.findByName(JavaPlugin.COMPILE_CONFIGURATION_NAME) == null
    }
}
