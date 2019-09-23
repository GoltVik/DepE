package com.credowolf.depe.utils

import com.android.tools.idea.gradle.parser.*
import com.intellij.util.IncorrectOperationException
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariableDeclaration
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrStatementOwner

val GradleBuildFile.dependenciesList: List<BuildFileStatement>
    get() = (getValue(BuildFileKey.DEPENDENCIES) as java.util.ArrayList<*>?
            ?: ArrayList<Any>()).map { it as BuildFileStatement }

val VersionsGradleFile.versionsList: List<UnparseableStatement>
    get() = (getValue(ExtraBuildFileKey.EXT) as java.util.ArrayList<*>?
            ?: ArrayList<Any>()).map { it as UnparseableStatement }

fun GradleBuildFile.findDefinitions(): List<GrStatement> {
    val owner = this.psiFile as GrStatementOwner
    val closure = this.getClosure(BuildFileKey.DEPENDENCIES.path)

    val ownerList = owner.statements.filterIsInstance<GrVariableDeclaration>()
    val closureList = closure?.statements?.filterIsInstance<GrVariableDeclaration>() ?: listOf()

    return (union(ownerList, closureList) as List<GrStatement>)
}

fun GradleBuildFile.removeDefinitions(definitions: Array<GrStatement>) {
    try {
        (psiFile as GrStatementOwner).removeElements(definitions)
    } catch (exception: IncorrectOperationException) {
        getClosure(BuildFileKey.DEPENDENCIES.path)?.removeElements(definitions)
    }
}