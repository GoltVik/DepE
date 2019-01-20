package com.credowolf.depe.ui.modulesdialog

import com.android.tools.idea.gradle.actions.SyncProjectAction
import com.credowolf.depe.VersionsExporter
import com.credowolf.depe.utils.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper


class ModulesDialog internal constructor(private val actionEvent: AnActionEvent, private val currentProject: Project = actionEvent.project!!) : DialogWrapper(currentProject, true) {

    private var form: ModulesDialogForm = ModulesDialogForm(currentProject)

    init {
        title = "Select modules"
        init()
    }

    override fun createCenterPanel(): ModulesDialogForm = form

    override fun doOKAction() {
        ApplicationManager.getApplication().invokeLater {
            currentProject.setFileName(form.versionsFileName!!.text)
            VersionsExporter(currentProject).startActionsWithModules(selectedModules, sync)
        }
        super.doOKAction()

    }

    private inline val selectedModules
        get() = if (form.singleModuleRadioButton!!.isSelected) {
            listOf(currentProject.moduleManager.activeSubmodules[form.modulesCombo!!.selectedIndex])
        } else {
            currentProject.moduleManager.activeSubmodules
        }

    private inline val sync get() = { SyncProjectAction().actionPerformed(actionEvent) }

}


