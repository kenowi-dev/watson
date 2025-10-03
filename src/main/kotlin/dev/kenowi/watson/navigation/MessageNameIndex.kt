package dev.kenowi.watson.navigation

import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiDocumentManager
import dev.kenowi.watson.services.InlangSettingsService

object MessageNameIndex {

    fun findMessageNames(name: String, project: Project): List<JsonProperty> {

        val messageFilesPaths = InlangSettingsService.getInstance(project).getLocaleMessagesFilePaths().values

        val result = ArrayList<JsonProperty>()

        for (file in messageFilesPaths) {
            val messageObject = file
                        .let { LocalFileSystem.getInstance().findFileByPath(it) }
                        ?.let { FileDocumentManager.getInstance().getDocument(it) }
                        ?.let { PsiDocumentManager.getInstance(project).getPsiFile(it) }
                        ?.let { it as? JsonFile }
                        ?.let { it.topLevelValue as? JsonObject }
                        ?.findProperty(name)

            if (messageObject == null) {
                continue
            }

            result.add(messageObject)
        }
        return result
    }
}