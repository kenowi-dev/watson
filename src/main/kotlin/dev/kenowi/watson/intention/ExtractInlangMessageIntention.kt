package dev.kenowi.watson.intention

import com.intellij.codeInsight.intention.FileModifier
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import com.intellij.psi.xml.XmlText
import java.time.LocalDateTime

class ExtractInlangMessageIntention : PsiElementBaseIntentionAction() {

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

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        //log(element.elementType.toString())
        return element.parent is XmlText ||
                element is XmlText ||
                element.elementType.toString() == "XML_NAME" ||
                element.elementType.toString() == "SVELTE_HTML_TAG" ||
                element.parent.elementType.toString() == "SVELTE_HTML_TAG"
    }

    override fun startInWriteAction(): Boolean = false

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        if (editor == null) {
            return
        }

        val selection = getSelection(editor, element);
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

        val humanID = humanID.generate();
        WriteCommandAction.runWriteCommandAction(project) {
            val message = "{m.$humanID()}"
            editor.document.replaceString(selection.start, selection.end, message)
            PsiDocumentManager.getInstance(project).commitDocument(editor.document)
        }
    }

    override fun generatePreview(project: Project, editor: Editor, psiFile: PsiFile): IntentionPreviewInfo {
        val element = psiFile.findElementAt(editor.caretModel.offset) ?: return IntentionPreviewInfo.FALLBACK_DIFF
        val selection = getSelection(editor, element);
        val humanID = humanID.generate();
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

        val effectiveElement = getEffectiveElement(element)
        val textRange = effectiveElement.textRange

        // Validate range
        if (textRange.isEmpty) {
            throw IllegalStateException("Cannot operate on empty text range")
        }

        return Selection(textRange.startOffset, textRange.endOffset, effectiveElement.text)
    }

    private fun getEffectiveElement(element: PsiElement): PsiElement {

        //when (element) {
        //    is XmlName -> return element
        //    is SvelteHtmlTag -> return element
        //    else -> return element.parent
        //}
        return when (element.elementType.toString()) {
            "XML_NAME" -> {
                element
            }

            "SVELTE_HTML_TAG" -> {
                element
            }

            else -> {
                element.parent
            }
        }
    }


}