package com.credowolf.depe.utils

import com.android.tools.idea.gradle.parser.*
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project

val ModuleManager.activeSubmodules: List<Module> get()= modules.drop(1)

val Project.moduleManager: ModuleManager get() = ModuleManager.getInstance(this)

fun Project.showNotification(message: String) {
    val notification = NotificationGroup("Versions exporter", NotificationDisplayType.BALLOON, true)
            .createNotification("Nothing to export", null, message, NotificationType.INFORMATION)

    Notifications.Bus.notify(notification, this)
}

val GradleBuildFile.dependenciesList: List<BuildFileStatement>
    get() = (getValue(BuildFileKey.DEPENDENCIES) as java.util.ArrayList<*>? ?: ArrayList<Any>()).map { it as BuildFileStatement }

val VersionsGradleFile.versionsList: List<UnparseableStatement>
    get() = (getValue(ExtraBuildFileKey.EXT) as java.util.ArrayList<*>? ?: ArrayList<Any>()).map { it as UnparseableStatement }

fun union(vararg lists: List<Any>): List<*> {
    return ArrayList<Any>().apply {
        lists.forEach {
            addAll(it)
        }
    }
}


fun write(project: Project, action: () -> Unit) = WriteCommandAction.runWriteCommandAction(project, action)
