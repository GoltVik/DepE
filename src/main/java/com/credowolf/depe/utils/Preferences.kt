package com.credowolf.depe.utils

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project

const val defaultVersionsFileName = "versions.gradle"
private const val def_filename = "def_versions_filename"

fun Project.getVersionsFileName(): String =
        PropertiesComponent.getInstance(this).getValue(def_filename, defaultVersionsFileName)


fun Project.setFileName(newFileName: String) {
    PropertiesComponent.getInstance(this).setValue(def_filename,
            if (newFileName.isEmpty()) {
                defaultVersionsFileName
            } else {
                newFileName.sanitizeFileName()
            })
}