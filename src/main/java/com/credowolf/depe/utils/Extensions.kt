package com.credowolf.depe.utils

import com.android.tools.idea.gradle.parser.*
import com.credowolf.depe.VersionsExporter
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile

val ModuleManager.activeSubmodules: List<Module> get() = modules.drop(1)

val Project.moduleManager: ModuleManager get() = ModuleManager.getInstance(this)

fun Project.showNotification(message: String) {
    val notification = NotificationGroup("Versions exporter", NotificationDisplayType.BALLOON, true)
            .createNotification("Nothing to export", null, message, NotificationType.INFORMATION)

    Notifications.Bus.notify(notification, this)
}

val GradleBuildFile.dependenciesList: List<BuildFileStatement>
    get() = (getValue(BuildFileKey.DEPENDENCIES) as java.util.ArrayList<*>?
            ?: ArrayList<Any>()).map { it as BuildFileStatement }

val VersionsGradleFile.versionsList: List<UnparseableStatement>
    get() = (getValue(ExtraBuildFileKey.EXT) as java.util.ArrayList<*>?
            ?: ArrayList<Any>()).map { it as UnparseableStatement }

fun guessVersionsFile(project: Project): VirtualFile {
    write(project) { project.guessProjectDir()!!.findOrCreateChildData(project, project.getVersionsFileName()) }
    return project.guessProjectDir()!!.findChild(project.getVersionsFileName())!!
}

fun createSingleGradleFileDescriptor(extension: String = "gradle"): FileChooserDescriptor {
    return FileChooserDescriptor(true, false, false, false, false, false)
            .withFileFilter { file ->
                Comparing.equal(file.extension, extension, SystemInfo.isFileSystemCaseSensitive) &&
                        file.name != "build.gradle" &&
                        file.name != "settings.gradle"
            }
}

fun write(project: Project, action: () -> Unit) = WriteCommandAction.runWriteCommandAction(project, action)

fun String.sanitizeFileName(): String = if (!this.endsWith(".gradle")) this.plus(".gradle").toLowerCase() else this.toLowerCase()

