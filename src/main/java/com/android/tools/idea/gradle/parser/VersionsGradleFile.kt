package com.android.tools.idea.gradle.parser

import com.credowolf.depe.utils.versionsList
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrStatementOwner

class VersionsGradleFile(@NotNull buildFile: VirtualFile, @NotNull project: Project) : GradleBuildFile(buildFile, project) {

    fun addVersions(newVersions: List<UnparseableStatement>) = setValue(ExtraBuildFileKey.EXT, getCombined(newVersions))

    private fun getCombined(newItems: List<UnparseableStatement>): List<UnparseableStatement> {
        ArrayList(versionsList.map { VersionUnparseableStatement(it) })
                .let { existingVersions ->
                    newItems.map { VersionUnparseableStatement(it) }
                            .forEach {
                                when (val statement = existingVersions.containsGroup(it)) {
                                    null -> existingVersions.add(it)

                                    else -> {
                                        if (statement.version < it.version) {
                                            existingVersions.remove(statement)
                                            existingVersions.add(it)
                                        }
                                    }
                                }

                            }
                    return existingVersions.map { it.realStatement }
                }
    }

    private fun List<VersionUnparseableStatement>.containsGroup(statement: VersionUnparseableStatement): VersionUnparseableStatement? {
        forEach { if (it.name == statement.name) return it }
        return null
    }


    fun setValue(key: ExtraBuildFileKey, value: Any) {
        checkInitialized()
        commitDocumentChanges()
        setValue(myGroovyFile, key, value, null)
    }

    fun setValue(rootOwner: GrStatementOwner?, key: ExtraBuildFileKey, value: Any, filter: ValueFactory.KeyFilter?) {
        var root = rootOwner
        checkInitialized()
        commitDocumentChanges()
        if (root == null) {
            root = myGroovyFile
        }
        setValueStatic(root!!, key, value, true, filter)
    }

    private fun setValueStatic(root: GrStatementOwner, key: ExtraBuildFileKey, value: Any, reformatClosure: Boolean, filter: ValueFactory.KeyFilter?) {
        if (value === GradleBuildFile.UNRECOGNIZED_VALUE) {
            return
        }
        var method = getMethodCallByPath(root, key.path)
        if (method == null) {
            method = createNewValue(root, key, value, reformatClosure)
            if (key.type !== BuildFileKeyType.CLOSURE) {
                return
            }
        }
        if (method != null) {
            val arg = (if (key.type === BuildFileKeyType.CLOSURE) getMethodClosureArgument(method) else getFirstArgument(method))
                    ?: return
            key.setValue(arg, value, filter)
        }
    }

    //region additional funs to SetValueStatic
    private fun createNewValue(root: GrStatementOwner, key: ExtraBuildFileKey, value: Any?, reformatClosure: Boolean): GrMethodCall? {
        // First iterate through the components of the path and make sure all of the nested closures are in place.
        val factory = GroovyPsiElementFactory.getInstance(root.project)
        val parts = key.path.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var parent = root
        for (i in 0 until parts.size - 1) {
            val part = parts[i]
            var closure: GrStatementOwner? = getMethodClosureArgument(parent, part)
            if (closure == null) {
                parent.addStatementBefore(factory.createStatementFromText("$part {}"), null)
                closure = getMethodClosureArgument(parent, part)
                if (closure == null) {
                    return null
                }
            }
            parent = closure
        }
        val name = parts[parts.size - 1]
        val text = name + " " + key.type.convertValueToExpression(value!!)
        var statementBefore: GrStatement? = null
        if (key.shouldInsertAtBeginning()) {
            val parentStatements = parent.statements
            if (parentStatements.isNotEmpty()) {
                statementBefore = parentStatements[0]
            }
        }
        parent.addStatementBefore(factory.createStatementFromText(text), statementBefore)
        if (reformatClosure) {
            internalReformatClosure(parent)
        }
        return getMethodCall(parent, name)
    }

    private fun internalReformatClosure(closure: GrStatementOwner) {
        ReformatCodeProcessor(closure.project, closure.containingFile, closure.parent.textRange, false).runWithoutProgress()

        // Now strip out any blank lines. They tend to accumulate otherwise. To do this, we iterate through our elements and find those that
        // consist only of whitespace, and eliminate all double-newline occurrences.
        for (psiElement in closure.children) {
            if (psiElement is LeafPsiElement) {
                val text = psiElement.text
                if (StringUtil.isEmptyOrSpaces(text)) {
                    var newText = text
                    while (newText.contains("\n\n")) {
                        newText = newText.replace("\n\n".toRegex(), "\n")
                    }
                    if (newText != text) {
                        psiElement.replaceWithText(newText)
                    }
                }
            }
        }
    }
    //endregion

    fun getValue(key: ExtraBuildFileKey): Any? {
        checkInitialized()
        return getValue(myGroovyFile, key)
    }

    fun getValue(rootOwner: GrStatementOwner?, key: ExtraBuildFileKey): Any? {
        var root = rootOwner
        checkInitialized()
        if (root == null) {
            root = myGroovyFile
        }
        return getValueStatic(root!!, key)
    }

    private fun getValueStatic(root: GrStatementOwner, key: ExtraBuildFileKey): Any? {
        val method = getMethodCallByPath(root, key.path) ?: return null
        val arg = (if (key.type === BuildFileKeyType.CLOSURE) getMethodClosureArgument(method) else getFirstArgument(method))
                ?: return null
        return key.getValue(arg)
    }

    data class VersionUnparseableStatement(val realStatement: UnparseableStatement,
                                           val name: String = realStatement.toString().split("=")[0].trim(),
                                           val version: String = realStatement.toString().split("=")[1].trim())
}