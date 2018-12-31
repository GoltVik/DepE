package com.credowolf.depe.ui.modulesdialog;

import com.intellij.openapi.module.Module;

import javax.swing.*;
import java.util.List;

public class ModulesDialogForm extends JPanel {
    JPanel contentPane;
    JRadioButton allModulesRadioButton;
    JRadioButton singleModuleRadioButton;
    JComboBox<String> modulesCombo;

    ModulesDialogForm(List<Module> modules) {
        ButtonGroup group = new ButtonGroup();
        group.add(allModulesRadioButton);
        group.add(singleModuleRadioButton);
        allModulesRadioButton.setSelected(true);
        for (Module module : modules) {
            modulesCombo.addItem(module.getName());
        }
        add(contentPane);
    }
}
