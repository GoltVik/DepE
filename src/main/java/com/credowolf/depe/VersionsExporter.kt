package com.credowolf.depe

import com.android.tools.idea.gradle.parser.*
import com.android.tools.idea.gradle.util.GradleUtil
import com.credowolf.depe.utils.*
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement
import java.lang.Exception

class VersionsExporter(private val project: Project) {

    private val versionsFile get() = project.guessVersionsFile()

    private fun excludeVersionsFromModules(selectedModules: List<Module>): Boolean {
        val versionsMap = mutableMapOf<String, UnparseableStatement>()
        selectedModules.map { GradleBuildFile.get(it)!! }.forEach { buildFile ->
            //rewrites dependencies from file
            val (depsVersions, dependencies) = readDepVersionsFromModule(buildFile)
            versionsMap.putAll(depsVersions)
            if (depsVersions.isNotEmpty()) write(project) { buildFile.setValue(BuildFileKey.DEPENDENCIES, dependencies) }

            //rewrites variables from file
            val (varsVersions, definitions) = readVarsFromModule(buildFile)
            versionsMap.putAll(varsVersions)
            write(project) { buildFile.removeDefinitions(definitions) }
        }
        return if (!versionsMap.values.isEmpty()) {
            write(project) { VersionsGradleFile(versionsFile, project).addVersions(versionsMap.values.map { it }) }
            true
        } else false
    }

    private fun readDepVersionsFromModule(buildFile: GradleBuildFile): Pair<MutableMap<String, UnparseableStatement>, List<BuildFileStatement>> {
        val moduleVersionsMap = mutableMapOf<String, UnparseableStatement>()

        val rawDependencies = preparseDependencies(ArrayList(buildFile.dependenciesList))

        val dependencies = rawDependencies.map { dependency ->
            if (dependency is Dependency) {
                if (dependency.type == Dependency.Type.EXTERNAL && !dependency.version.startsWith("\$")) { //replace this dependency version
                    moduleVersionsMap[dependency.group] = UnparseableStatement(dependency.toExt, project)
                    Dependency(dependency.scope, dependency.type, dependency.toDependencies, dependency.extraClosure)
                } else dependency
            } else dependency
        }
        return Pair(moduleVersionsMap, dependencies)
    }

    private fun preparseDependencies(rawDeps: ArrayList<BuildFileStatement>): ArrayList<BuildFileStatement> {
        val parsedList = ArrayList<BuildFileStatement>(rawDeps)

        rawDeps.forEachIndexed { index, statement ->
            if (statement is UnparseableStatement) {
                try {
                    val parsed = processUnparseableStatement(statement)
                    if(parsed.isNotEmpty()) {
                        parsedList.remove(statement)
                        processUnparseableStatement(statement).forEach { parsedStatement -> parsedList.add(index, parsedStatement) }
                    }
                } catch (ignored : Exception) {
                }

            }
        }
        return parsedList
    }


    private fun processUnparseableStatement(originalStatement: UnparseableStatement): List<UnparseableStatement> {

        val statementParts = originalStatement.toString()
                .replace("(", "")
                .replace(")", "")
                .replace(",", "")
                .replace(" ", "")
                .replace("\"", "")
                .replace("\'", "").trimIndent()
                .split('\n')

        val prefix = statementParts[0];

        return statementParts.subList(1, statementParts.size).map { stringValue -> UnparseableStatement("$prefix \"$stringValue\"", project) }.reversed()
    }

    private fun readVarsFromModule(buildFile: GradleBuildFile): Pair<MutableMap<String, UnparseableStatement>, Array<GrStatement>> {
        val moduleVarsMap = mutableMapOf<String, UnparseableStatement>()
        val definitions = buildFile.findDefinitions()
        definitions.forEach { moduleVarsMap[it.group] = UnparseableStatement(it.text.removePrefix("def").removePrefix("final").trim(), project) }
        return Pair(moduleVarsMap, definitions.toTypedArray())
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



