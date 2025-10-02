package dev.kenowi.watson.services

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.javascript.nodejs.NodeCommandLineUtil
import com.intellij.javascript.nodejs.interpreter.NodeCommandLineConfigurator
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import dev.kenowi.watson.settings.WatsonSettings
import java.nio.charset.StandardCharsets

@Service(Service.Level.PROJECT)
internal class InlangSdkService(private val project: Project) {

    val settings = WatsonSettings.getInstance(project)

    companion object {
        fun getInstance(project: Project): InlangSdkService = project.service()
    }

    fun compileMessageInConsole() {
        ApplicationManager.getApplication().invokeLater {
            val handler = NodeCommandLineUtil.createProcessHandler(compileMessageCommandLine(), false)
            NodeCommandLineUtil.showConsole(
                handler,
                "compile-inlang-messages",
                project,
                listOf(),
                "Compile Inlang Messages"
            )
        }
    }

    fun compileMessagesBackground() {
        ApplicationManager.getApplication().executeOnPooledThread {
            val processHandler = CapturingProcessHandler(compileMessageCommandLine())
            val output = processHandler.runProcess(10000)
            if (output.exitCode != 0) {
                Messages.showErrorDialog(project, "error compiling messages: " + output.stderr, "Error")
                return@executeOnPooledThread
            }

            NotificationGroupManager.getInstance()
                .getNotificationGroup("Watson Notifications")
                .createNotification(
                    "Watson",
                    "Messages compiled successfully",
                    NotificationType.INFORMATION
                )
                .notify(project)
        }
    }


    fun executeInlineScript(): String? {
        val interpreter = getNodeInterpreter()
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
            Messages.showErrorDialog(project, "error creating humanId from sdk: " + output.stderr, "Error")
            return null
        }

        return output.stdout.trim { it <= ' ' }
    }

    private fun getNodeInterpreter(): NodeJsInterpreter {
        val interpreter = NodeJsInterpreterManager.getInstance(project).interpreter
        if (interpreter == null) {
            // Handle case where no Node interpreter is configured
            Messages.showErrorDialog(project, "No Node.js interpreter configured for this project", "Error")
            throw IllegalStateException("Node.js interpreter not configured.")
        }
        return interpreter
    }

    private fun compileMessageCommandLine(): GeneralCommandLine {
        val interpreter = getNodeInterpreter()
        val paraglideJs = "@inlang/paraglide-js"

        assertPackageAvailable(paraglideJs)

        val commandLine = NodeCommandLineUtil.createCommandLine()
        NodeCommandLineUtil.prependNodeDirToPATH(commandLine, interpreter)

        commandLine.withWorkDirectory(project.basePath)
        commandLine.charset = StandardCharsets.UTF_8
        commandLine.exePath = if (SystemInfo.isWindows) "npx.cmd" else "npx"
        // TODO this should be compile --project ./project.inlang --outdir ./src/lib/paraglide
        //  But project and outdir needs to be read from vite.config.ts
        commandLine.addParameters(
            paraglideJs,
            "compile",
            "--project",
            settings.inlangProject,
            "--outdir",
            settings.inlangOutDir,
        )
        return commandLine
    }

    private fun assertPackageAvailable(packageName: String) {

        val nodeModulesDir = project
            .guessProjectDir()
            ?.path
            ?.let { LocalFileSystem.getInstance().findFileByPath(it) }
            ?.findChild("node_modules")?: return

        var packageDir: VirtualFile? = nodeModulesDir
        for (p in packageName.split('/')) {
            packageDir = packageDir?.findChild(p)
        }


        if (packageDir == null || !packageDir.exists()) {
            val msg = "Package $packageName not found"
            Messages.showErrorDialog(project, msg, "Error")
        }
    }

}