package com.credowolf.depe.ui.modulesdialog

import com.credowolf.depe.utils.*
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir

import javax.swing.*

class ModulesDialogForm internal constructor(project: Project) : JPanel() {
    internal var contentPane: JPanel? = null
    internal var allModulesRadioButton: JRadioButton? = null
    internal var singleModuleRadioButton: JRadioButton? = null
    internal var modulesCombo: JComboBox<String>? = null
    internal var versionsFileName: JTextField? = null
    internal var selectFile: JButton? = null
    private var versionsFileLabel: JLabel? = null

    init {
        ButtonGroup().let {
            it.add(allModulesRadioButton)
            it.add(singleModuleRadioButton)
        }

        allModulesRadioButton!!.isSelected = true

        project.moduleManager.activeSubmodules.forEach {
            modulesCombo!!.addItem(it.name)
        }

        versionsFileName!!.text = project.getVersionsFileName()
        versionsFileName!!.isEditable = true

        selectFile!!.addActionListener {
            val projectDir = project.guessProjectDir()
            if(projectDir != null) {
                val virtualFile = FileChooser.chooseFile(createSingleGradleFileDescriptor(), project, project.guessVersionsFile())
                if (virtualFile != null) {
                    versionsFileName!!.text = virtualFile.path.removePrefix("${projectDir.path}/")
                }
            }
            else{
                //show error -> can't guess project dir
            }
        }

        add(contentPane)
    }
}
