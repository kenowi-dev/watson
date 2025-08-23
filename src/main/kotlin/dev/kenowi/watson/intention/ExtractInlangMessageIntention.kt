package dev.kenowi.watson.intention

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.javascript.nodejs.interpreter.NodeCommandLineConfigurator
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.psi.xml.XmlText
import java.io.IOException
import java.time.LocalDateTime


class ExtractInlangMessageIntention : PsiElementBaseIntentionAction() {

    private val LOG: (String) -> Unit = { msg -> println("${LocalDateTime.now()} --- $msg") }

    private val humanID: HumanID = HumanID()

    override fun getText(): String {
        return familyName
    }

    override fun getFamilyName(): String {
        return "Extract inlang message"
    }

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        //Log.info(element.elementType.toString())
        return element.parent is XmlText ||
                element is XmlText ||
                element.elementType.toString() == "XML_NAME" ||
                element.elementType.toString() == "SVELTE_HTML_TAG" ||
                element.parent.elementType.toString() == "SVELTE_HTML_TAG"
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        if (editor == null) {
            return
        }
        LOG("Run Intention")

        val interpreter = NodeJsInterpreterManager.getInstance(project).interpreter

        if (interpreter == null) {
            // Handle case where no Node interpreter is configured
            Messages.showErrorDialog(project, "No Node.js interpreter configured for this project", "Error")
            return
        }
        LOG("Node interpreter available")


        val inlangAvailable = isPackageAvailable(project, "@inlang")
        val inlangSdkAvailable = isPackageAvailable(project, "@inlang/sdk")
        if (!inlangAvailable && !inlangSdkAvailable) {
            Messages.showErrorDialog(project, "Inlang sdk not available", "Error")
            return
        }

        LOG("Packages available")

        // If in preview mode
        if (IntentionPreviewUtils.isIntentionPreviewActive()) {
            // Intention Preview Mode
            LOG("Preview generated: ${humanID.generate()}")
            return
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                //val generatedText = executeInlineScript(project, interpreter)
                val generatedText = humanID.generate()
                LOG("Script generated: $generatedText")
                ApplicationManager.getApplication().invokeLater {
                    WriteCommandAction.runWriteCommandAction(editor.project) {
                        val document: Document = editor.document
                        val offset = editor.caretModel.offset
                        document.insertString(offset, generatedText)
                    }
                    LOG("Intention successful")
                }
            } catch (e: Exception) {
                LOG("Error during script execution: ${e.message}")
                if (!IntentionPreviewUtils.isIntentionPreviewActive()) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(project, "Error generating text: " + e.message, "Error")
                    }
                }
            }
        }

    }


    @Throws(java.lang.Exception::class)
    private fun executeInlineScript(project: Project, interpreter: NodeJsInterpreter): String {

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
            throw java.lang.RuntimeException("Inline script failed: " + output.stderr)
        }

        return output.stdout.trim { it <= ' ' }
    }

    @Throws(java.lang.Exception::class)
    private fun executeJsScript(project: Project, interpreter: NodeJsInterpreter): String {
        val scriptFile = createOrGetScriptFile(project)

        val configurator = NodeCommandLineConfigurator.find(interpreter)
        val commandLine = GeneralCommandLine()
        configurator.configure(commandLine)

        commandLine.addParameter(scriptFile.path)
        commandLine.setWorkDirectory(project.basePath)

        val processHandler = CapturingProcessHandler(commandLine)
        val output = processHandler.runProcess(10000)

        if (output.getExitCode() != 0) {
            throw RuntimeException("Script execution failed: " + output.stderr)
        }

        return output.stdout.trim { it <= ' ' }
    }

    private fun isPackageAvailable(project: Project, packageName: String): Boolean {
        val projectPath = project.basePath ?: return false
        val projectDir = LocalFileSystem.getInstance().findFileByPath(projectPath) ?: return false

        val nodeModules = projectDir.findChild("node_modules") ?: return false

        val packageDir = nodeModules.findChild(packageName)
        return packageDir != null && packageDir.exists()
    }

    @Throws(IOException::class)
    private fun createOrGetScriptFile(project: Project?): VirtualFile {
        val projectPath: String = project!!.basePath
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