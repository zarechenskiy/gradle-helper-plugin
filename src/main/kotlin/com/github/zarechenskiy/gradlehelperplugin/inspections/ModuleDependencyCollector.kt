package com.github.zarechenskiy.gradlehelperplugin.inspections

import com.github.zarechenskiy.gradlehelperplugin.*
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderEntry
import org.jetbrains.kotlin.idea.util.projectStructure.allModules

class ModuleDependencyCollector {
    private val libraryNamesToOrderEntries = mutableMapOf<String, MutableSet<LibraryOrderEntry>>()
    private val moduleNamesToOrderEntries = mutableMapOf<String, MutableSet<ModuleOrderEntry>>()

    private val dependencyVisitor = object : DependencyVisitor {
        override fun apply(moduleEntry: ModuleOrderEntry) {
            val parentModuleName = moduleEntry.module?.getParentModuleName() ?: return
            moduleNamesToOrderEntries.add(parentModuleName, moduleEntry)
        }

        override fun apply(libraryEntry: LibraryOrderEntry) {
            val name = libraryEntry.libraryName?.substringAfter(GRADLE_LIBRARY_NAME_PREFIX)
            if (name != null) {
                libraryNamesToOrderEntries.add(name, libraryEntry)
            }
        }
    }

    fun collectDependencies(module: Module) {
        val (mainChildModule, testChildModule) = module.getMainAndTestChildModules()
        mainChildModule?.apply { traverseDependencies(dependencyVisitor) }
        testChildModule?.apply { traverseDependencies(dependencyVisitor) }
    }

    fun getLibraryEntries(name: String) =
        libraryNamesToOrderEntries[name]?.toList().orEmpty()

    fun getModuleEntries(name: String) =
        moduleNamesToOrderEntries[name]?.toList().orEmpty()
}

private interface DependencyVisitor {
    fun apply(moduleEntry: ModuleOrderEntry)

    fun apply(libraryEntry: LibraryOrderEntry)
}

private fun Module.traverseDependencies(visitor: DependencyVisitor) {
    val parentName = getParentModuleName()
    for (dependency in getModuleDependencies()) {
        if (dependency.getParentModuleName() != parentName) {
            val orderEntries = ModuleRootManager.getInstance(dependency).orderEntries
            for (orderEntry in orderEntries) {
                orderEntry.accept(visitor)
            }
        }
    }
}

private fun OrderEntry.accept(visitor: DependencyVisitor) =
    when (this) {
        is LibraryOrderEntry -> visitor.apply(this)
        is ModuleOrderEntry -> visitor.apply(this)
        else -> {}
    }

private fun Module.getMainAndTestChildModules(): Pair<Module?, Module?> {
    var mainChildModule: Module? = null
    var testChildModule: Module? = null
    val mainChildModuleName = "$name$INTELLIJ_MODULE_NAME_DELIMITER$MAIN_CHILD_MODULE_NAME"
    val testChildModuleName = "$name$INTELLIJ_MODULE_NAME_DELIMITER$TEST_CHILD_MODULE_NAME"
    for (module in project.allModules()) {
        val moduleName = module.name
        if (moduleName == mainChildModuleName) {
           mainChildModule = module
        } else if (moduleName == testChildModuleName) {
            testChildModule = module
        }
    }

    return Pair(mainChildModule, testChildModule)
}

private fun Module.getModuleDependencies(): Array<Module> =
    ModuleRootManager.getInstance(this).dependencies

private fun <T> MutableMap<String, MutableSet<T>>.add(key: String, value: T) {
    compute(key) { _, set ->
        if (set != null) {
            set.add(value)
            set
        } else {
            mutableSetOf(value)
        }
    }
}
