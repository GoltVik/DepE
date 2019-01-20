package com.credowolf.depe

import com.android.tools.idea.gradle.parser.*
import com.android.tools.idea.gradle.util.GradleUtil
import com.credowolf.depe.utils.*
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class VersionsExporter(private val project: Project) {

    private val versionsFile get() = guessVersionsFile(project)

    private fun excludeVersionsFromModules(selectedModules: List<Module>): Boolean {
        val versionsMap = mutableMapOf<String, UnparseableStatement>()
        selectedModules.map { GradleBuildFile.get(it)!! }.forEach { buildFile ->
            var modified = false
            val dependencies = buildFile.dependenciesList.map { dependency ->
                if (dependency is Dependency) {
                    if (dependency.type == Dependency.Type.EXTERNAL && !dependency.version.startsWith("\$")) { //replace this dependency version
                        versionsMap[dependency.name] = UnparseableStatement(dependency.toExt, project)
                        modified = true
                        Dependency(dependency.scope, dependency.type, dependency.toDependencies, dependency.extraClosure)
                    } else {
                        dependency
                    }
                } else {
                    dependency
                }
            }
            if (modified) write(project) { buildFile.setValue(BuildFileKey.DEPENDENCIES, dependencies) }
        }
        return if (!versionsMap.values.isEmpty()) {
            write(project) { VersionsGradleFile(versionsFile, project).addVersions(versionsMap.values.map { it }) }
            true
        } else {
            false
        }
    }

    private fun applyVersionsFile(versionsFile: VirtualFile) {
        val baseModule = project.moduleManager.modules[0]
        val gradle = GradleUtil.getGradleBuildFile(baseModule)
        val doc = FileDocumentManager.getInstance().getDocument(gradle!!)!!
        val applyText = "apply from: '" + versionsFile.name + "'\n"

        //VEEEERY BAD SOLUTION but works
        write(project) {
            if (!doc.text.contains(applyText))
                doc.setText(applyText + doc.text)
        }

    }

    fun startActionsWithModules(modules: List<Module>, sync: () -> Unit) {
        val modified = excludeVersionsFromModules(modules)
        if (modified) {
            applyVersionsFile(versionsFile)
            sync.invoke()
        } else project.showNotification("No dependencies to export versions")
    }
}