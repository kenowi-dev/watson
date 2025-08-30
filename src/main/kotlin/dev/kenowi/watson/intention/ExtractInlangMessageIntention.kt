package dev.kenowi.watson.intention

import com.intellij.codeInsight.intention.BaseElementAtCaretIntentionAction
import com.intellij.codeInsight.intention.FileModifier
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import java.time.LocalDateTime

class ExtractInlangMessageIntention : BaseElementAtCaretIntentionAction() {

    private fun log(message: String) {
        println("${LocalDateTime.now()} --- $message")
    }

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

            // Validate range
            if (stringLiteral.startOffset >= stringLiteral.endOffset || stringLiteral.content.isEmpty()) {
                throw IllegalStateException("Cannot operate on empty text range")
            }

            return true
        } catch (_: Exception) {
            return false
        }
    }

    override fun startInWriteAction(): Boolean = false

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {

        val selection = getSelection(editor, element)
        //val xmlText = element.parent as? XmlText ?: element.parent as? SvelteHtmlTag ?: return
        if (!IntentionPreviewUtils.isIntentionPreviewActive()) {
            // TODO handle selection.text abd dialog
            val dialog = HumanIdOptionsDialog()
            if (!dialog.showAndGet()) {
                return // User cancelled
            }

            //val original = xmlText.value
            val lowercase = dialog.lowercase
            val separator = dialog.separator
        }

        val humanID = humanID.generate()
        WriteCommandAction.runWriteCommandAction(project) {
            val message = "{m.$humanID()}"
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