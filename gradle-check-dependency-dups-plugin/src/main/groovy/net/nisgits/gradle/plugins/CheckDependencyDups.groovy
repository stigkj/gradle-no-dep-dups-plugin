package net.nisgits.gradle.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project

 /**
 * Gradle plugin that checks for duplicate dependencies in a build. That is, it throws an exception if the build has
 * more than one version of any of its dependencies.
 *
 * @author Stig Kleppe-Jorgensen, 2011.03.30
 */
class CheckDependencyDups implements Plugin<Project> {
    def void apply(Project project) {
        project.gradle.taskGraph.whenReady {
            def dependencyToVersions = [:]

            project.allprojects {
                it.configurations.all {
                    def configName = name
                    it.allDependencies.each {
                        def versionsForDependency = dependencyToVersions.get("${it.group}:${it.name}")

                        if (versionsForDependency == null) {
                            versionsForDependency = [:]
                            dependencyToVersions.put("${it.group}:${it.name}", versionsForDependency)
                        }

                        def configuration = versionsForDependency.get("${it.version}")

                        if (configuration == null) {
                            configuration = new HashSet()
                            versionsForDependency.put("${it.version}", configuration)
                        }

                        configuration.add("${configName}")
                    }
                }
            }

            def dependenciesGroupedByDupsOrNot = dependencyToVersions.groupBy {it.value.size() > 1}
            def duplicates = dependenciesGroupedByDupsOrNot.get(true)

            if (duplicates != null) {
                // FIXME improve error handling; check how the compile plugin do it, for example
                throw new IllegalStateException(buildExceptionMessageFor(duplicates))
            }
        }
    }

    def buildExceptionMessageFor(duplicates) {
        def exceptionString = "Dependencies with several versions: \n"

        duplicates.each {
            exceptionString += "${it.getKey()} --> ${stringFor(it.getValue())}\n"
        }

        return exceptionString
    }

    def stringFor(versionToConfigurations) {
        def s = []

        versionToConfigurations.each {
            s += "${it.getKey()} in ${it.getValue()}"
        }

        return s
    }
}
