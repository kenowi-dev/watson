package dev.kenowi.watson.intention

import com.intellij.codeInsight.intention.BaseElementAtCaretIntentionAction
import com.intellij.codeInsight.intention.FileModifier
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonPsiUtil
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import dev.kenowi.watson.services.InlangSdkService
import dev.kenowi.watson.services.InlangSettingsService
import dev.kenowi.watson.services.NotificationService
import dev.kenowi.watson.settings.WatsonSettings

class ExtractInlangMessageIntention : BaseElementAtCaretIntentionAction() {

    @FileModifier.SafeFieldForPreview
    private val humanID: HumanID = HumanID()

    override fun getText(): String {
        return familyName
    }

    override fun getFamilyName(): String {
        return "Extract inlang message"
    }

    override fun isAvailable(
        project: Project,
        editor: Editor,
        element: PsiElement
    ): Boolean {
        try {
            if (!IntentionUtils.isJavaScriptFamily(element.containingFile)) {
                return false
            }
            val stringLiteral = IntentionUtils.findStringLiteral(element) ?: return false
            return stringLiteral.content.trim().isNotEmpty()
                    && stringLiteral.startOffset < stringLiteral.endOffset
            //throw IllegalStateException("Cannot operate on empty text range")
        } catch (_: Exception) {
            return false
        }
    }

    override fun startInWriteAction(): Boolean = false

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {

        val selection = getSelection(editor, element)

        var methodName = humanID.generate()
        if (!IntentionPreviewUtils.isIntentionPreviewActive()) {
            // TODO handle selection.text and dialog
            val dialog = HumanIdOptionsDialog(project)
            if (!dialog.showAndGet()) {
                return // User cancelled
            }

            methodName = dialog.methodName
            val localeMessagesFilePaths = InlangSettingsService.getInstance(project).getLocaleMessagesFilePaths()

            for (entry in dialog.translations) {
                val locale = entry.key
                val message = entry.value

                val messageObject = localeMessagesFilePaths[locale]
                    ?.let { LocalFileSystem.getInstance().findFileByPath(it) }
                    ?.let { FileDocumentManager.getInstance().getDocument(it) }
                    ?.let { PsiDocumentManager.getInstance(project).getPsiFile(it) }
                    ?.let { it as? JsonFile }
                    ?.let { it.topLevelValue as? JsonObject }

                if (messageObject == null) {
                    NotificationService
                        .getInstance(project)
                        .warn("Error getting message file: $locale")
                    return
                }
                val newMessage = JsonElementGenerator(project).createProperty(methodName, "\"$message\"")
                runWriteAction {
                    JsonPsiUtil.addProperty(messageObject, newMessage, false)
                }
            }
            if (WatsonSettings.getInstance(project).compileAfterExtract) {
                InlangSdkService.getInstance(project).compileMessagesBackground()
            }
        }


        WriteCommandAction.runWriteCommandAction(project) {
            val message = when {
                IntentionUtils.needsSvelteWrapping(element) -> "{m.$methodName()}"
                else -> "m.$methodName()"
            }

            editor.document.replaceString(selection.start, selection.end, message)
            PsiDocumentManager.getInstance(project).commitDocument(editor.document)
        }
    }


    override fun generatePreview(project: Project, editor: Editor, psiFile: PsiFile): IntentionPreviewInfo {
        val element = psiFile.findElementAt(editor.caretModel.offset) ?: return IntentionPreviewInfo.EMPTY
        val selection = getSelection(editor, element)
        val humanID = humanID.generate()
        val message = "{m.$humanID()}"
        editor.document.replaceString(selection.start, selection.end, message)
        return IntentionPreviewInfo.DIFF
    }

    internal data class Selection(val start: Int, val end: Int, val text: String)

    private fun getSelection(editor: Editor, element: PsiElement): Selection {
        val selectionModel = editor.selectionModel
        val selectedText = selectionModel.selectedText?.trim()

        if (!selectedText.isNullOrEmpty()) {
            return Selection(selectionModel.selectionStart, selectionModel.selectionEnd, selectedText)
        }

        val effectiveElement = IntentionUtils.findStringLiteral(element)!!

        return Selection(effectiveElement.startOffset, effectiveElement.endOffset, effectiveElement.content)
    }
}