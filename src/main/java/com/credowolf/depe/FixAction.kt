package com.credowolf.depe

import com.credowolf.depe.ui.modulesdialog.ModulesDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent


class FixAction : AnAction() {

    override fun update(anActionEvent: AnActionEvent) {
        anActionEvent.presentation.isEnabledAndVisible = anActionEvent.project != null
    }

    override fun actionPerformed(anActionEvent: AnActionEvent) = ModulesDialog(anActionEvent).show()
}
