# Gradle Mammoth

This project is designed to help you out while editing `build.gradle.kts` files.

## List of features:
1. Completion of compiler arguments
   ![alt text](gifs/compilerArgumentsCompletion.gif)

2. Completion of JVM arguments
   ![alt text](gifs/jvmArgumentsCompletion.gif)

3. Multiplatform target generation
   ![alt text](gifs/generatingMultiplatformTargets.gif)

4. Resolve to build script file by module name
   ![alt text](gifs/resoleToBuildScripts.gif)

5. Inspection that finds redundant dependency declarations and allows you to navigate to their declaration in parent modules.
   ![alt text](gifs/redundantDependenciesInspection.gif)

6. Enhanced documentation for functions that declare dependencies
   ![alt text](gifs/enhancedDocumentation.gif)

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "gradle-helper-plugin"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/zarechenskiy/gradle-helper-plugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>
