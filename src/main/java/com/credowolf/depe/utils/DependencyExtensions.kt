package com.credowolf.depe.utils

import com.android.tools.idea.gradle.parser.Dependency

val Dependency.toDependencies: String get() = "$name:$group:\$${group.replace("-", "_")}"
val Dependency.toExt: String get() = "${group.replace("-", "_")}='$version'"

val Dependency.name: String get() = this.pathItem(0)
val Dependency.group: String get() = this.pathItem(1)
val Dependency.version: String get() = this.pathItem(2)

private fun Dependency.pathItem(position: Int): String = valueAsString.split(":")[position]



