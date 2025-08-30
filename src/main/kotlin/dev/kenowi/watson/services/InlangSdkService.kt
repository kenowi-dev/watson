package dev.kenowi.watson.services

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.javascript.nodejs.interpreter.NodeCommandLineConfigurator
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.IOException

@Service(Service.Level.PROJECT)
internal class InlangSdkService(private val project: Project) {
    companion object {
        fun getInstance(project: Project): InlangSdkService = project.service()
    }

    fun isNodeAvailable(): NodeJsInterpreter? {
        val interpreter = NodeJsInterpreterManager.getInstance(project).interpreter

        if (interpreter == null) {
            // Handle case where no Node interpreter is configured
            Messages.showErrorDialog(project, "No Node.js interpreter configured for this project", "Error")
            return interpreter
        }
        return null
    }

    fun executeInlineScript(interpreter: NodeJsInterpreter): String? {

        val configurator = NodeCommandLineConfigurator.find(interpreter)
        val commandLine = GeneralCommandLine()
        configurator.configure(commandLine)


        commandLine.addParameter("--eval")
        commandLine.addParameter("import { humanId } from '@inlang/sdk'; console.log(humanId());")
        commandLine.setWorkDirectory(project.basePath)

        val processHandler = CapturingProcessHandler(commandLine)
        // This is slow when the node interpreter is on windows and the project is on WSL.
        val output = processHandler.runProcess(10000)


        if (output.getExitCode() != 0) {
            Messages.showErrorDialog(project,"error creating humanId from sdk: " + output.stderr, "Error")
            return null
        }

        return output.stdout.trim { it <= ' ' }
    }

    fun executeJsScript(interpreter: NodeJsInterpreter): String? {
        try {
            val scriptFile = createOrGetScriptFile()

            val configurator = NodeCommandLineConfigurator.find(interpreter)
            val commandLine = GeneralCommandLine()
            configurator.configure(commandLine)

            commandLine.addParameter(scriptFile.path)
            commandLine.setWorkDirectory(project.basePath)

            val processHandler = CapturingProcessHandler(commandLine)
            val output = processHandler.runProcess(10000)

            if (output.getExitCode() != 0) {
                Messages.showErrorDialog(project, "Script execution failed: " + output.stderr, "Error")
                return null
            }
            return output.stdout.trim { it <= ' ' }
        } catch (e: IOException) {
            Messages.showErrorDialog(project, "Error creating script", "Error")
            return null
        }
    }

    fun isSdkAvailable(): Boolean {
        val inlangAvailable = isPackageAvailable("@inlang")
        val inlangSdkAvailable = isPackageAvailable("@inlang/sdk")
        if (!inlangAvailable && !inlangSdkAvailable) {
            Messages.showErrorDialog(project, "Inlang sdk not available", "Error")
            return false
        }
        return true
    }

    private fun isPackageAvailable(packageName: String): Boolean {

        val projectPath = project.basePath ?: return false
        val projectDir = LocalFileSystem.getInstance().findFileByPath(projectPath) ?: return false

        val nodeModules = projectDir.findChild("node_modules") ?: return false

        val packageDir = nodeModules.findChild(packageName)
        return packageDir != null && packageDir.exists()
    }

    @Throws(IOException::class)
    private fun createOrGetScriptFile(): VirtualFile {
        val projectPath: String = project.basePath
            ?: throw IOException("Could not determine project directory")

        val projectDir = LocalFileSystem.getInstance().findFileByPath(projectPath)
            ?: throw IOException("Project directory not found")


        // Create script in .idea directory to keep it hidden from user
        var ideaDir = projectDir.findChild(".idea")
        if (ideaDir == null) {
            ideaDir = projectDir.createChildDirectory(this, ".idea")
        }

        var pluginDir = ideaDir.findChild("plugin-scripts")
        if (pluginDir == null) {
            pluginDir = ideaDir.createChildDirectory(this, "plugin-scripts")
        }


        // Use .mjs extension for ES modules
        var scriptFile = pluginDir.findChild("generator.mjs")
        if (scriptFile == null) {
            scriptFile = pluginDir.createChildData(this, "generator.mjs")
        }


        // Always update the script content to ensure it's current
        val scriptContent: String = generateESModuleScript()
        VfsUtil.saveText(scriptFile, scriptContent)

        return scriptFile
    }

    private fun generateESModuleScript(): String {
        return """
            import { humanId } from "@inlang/sdk";
            try {
                console.log(humanId());
            } catch (error) {
                console.error('Error generating human ID:', error.message);
                process.exit(1);
            }              
            """.trimIndent()
    }
}