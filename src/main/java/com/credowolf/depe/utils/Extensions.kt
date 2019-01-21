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
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement

val ModuleManager.activeSubmodules: List<Module> get() = modules.drop(1)

val Project.moduleManager: ModuleManager get() = ModuleManager.getInstance(this)

fun Project.showNotification(message: String) {
    val notification = NotificationGroup("Versions exporter", NotificationDisplayType.BALLOON, true)
            .createNotification("Nothing to export", null, message, NotificationType.INFORMATION)

    Notifications.Bus.notify(notification, this)
}

fun Project.guessVersionsFile(): VirtualFile {
    write(this) { this.guessProjectDir()!!.findOrCreateChildData(this, this.getVersionsFileName()) }
    return this.guessProjectDir()!!.findChild(this.getVersionsFileName())!!
}

val GrStatement.group: String get() = text.split("=")[0]

fun createSingleGradleFileDescriptor(extension: String = "gradle"): FileChooserDescriptor {
    return FileChooserDescriptor(true, false, false, false, false, false)
            .withFileFilter { file ->
                Comparing.equal(file.extension, extension, SystemInfo.isFileSystemCaseSensitive) &&
                        file.name != "build.gradle" &&
                        file.name != "settings.gradle"
            }
}

fun union(vararg lists: List<Any>): List<*> {
    return ArrayList<Any>().apply {
        lists.forEach {
            addAll(it)
        }
    }
}
fun write(project: Project, action: () -> Unit) = WriteCommandAction.runWriteCommandAction(project, action)

fun String.sanitizeFileName(): String = if (!this.endsWith(".gradle")) this.plus(".gradle").toLowerCase() else this.toLowerCase()

