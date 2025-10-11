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
import com.intellij.util.Alarm
import dev.kenowi.watson.WatsonMessageBundle
import dev.kenowi.watson.services.InlangSdkService
import dev.kenowi.watson.services.ParaglideSettingsService
import dev.kenowi.watson.services.WatsonNotificationService
import dev.kenowi.watson.settings.WatsonSettings
import dev.kenowi.watson.utils.StringLiteralUtils
import java.util.concurrent.TimeUnit

class ExtractParaglideMessageIntention : BaseElementAtCaretIntentionAction() {

    @FileModifier.SafeFieldForPreview
    private val humanID: HumanID = HumanID()

    override fun getText(): String {
        return familyName
    }

    override fun getFamilyName(): String {
        return WatsonMessageBundle.message("intention.family.name")
    }

    override fun isAvailable(
        project: Project,
        editor: Editor,
        element: PsiElement
    ): Boolean {
        try {
            if (!StringLiteralUtils.isJavaScriptFamily(element.containingFile)) {
                return false
            }
            val stringLiteral = StringLiteralUtils.findStringLiteral(element) ?: return false
            return stringLiteral.content.trim().isNotEmpty()
                    && stringLiteral.startOffset < stringLiteral.endOffset
            //throw IllegalStateException("Cannot operate on empty text range")
        } catch (_: Exception) {
            return false
        }
    }

    override fun startInWriteAction(): Boolean = false

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        if (IntentionPreviewUtils.isIntentionPreviewActive()) {
            return
        }
        val selection = getSelection(editor, element)

        val dialog = ExtractionDialog(project, selection.text)
        if (!dialog.showAndGet()) {
            return // User cancelled
        }

        val methodName = dialog.methodName
        val localeMessagesFilePaths = ParaglideSettingsService.getInstance(project).getLocaleMessagesFilePaths()
        val locales = ParaglideSettingsService.getInstance(project).getSettings()?.locales
        for (locale in locales ?: emptyList()) {
            val messageObject = localeMessagesFilePaths[locale]
                ?.let { LocalFileSystem.getInstance().findFileByPath(it) }
                ?.let { FileDocumentManager.getInstance().getDocument(it) }
                ?.let { PsiDocumentManager.getInstance(project).getPsiFile(it) }
                ?.let { it as? JsonFile }
                ?.let { it.topLevelValue as? JsonObject }

            if (messageObject == null) {
                WatsonNotificationService
                    .getInstance(project)
                    .warn(WatsonMessageBundle.message("intention.error.file", locale))
                return
            }
            val newMessage = JsonElementGenerator(project)
                .createProperty(methodName, dialog.translations.get(locale).toJsonString())
            runWriteAction {
                JsonPsiUtil.addProperty(messageObject, newMessage, false)
            }
        }
        if (WatsonSettings.getInstance(project).compileAfterExtract) {
            val alarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD)
            alarm.addRequest({
                InlangSdkService.getInstance(project).compileMessagesBackground()
            }, TimeUnit.SECONDS.toMillis(2).toInt())

        }

        WriteCommandAction.runWriteCommandAction(project) {
            val params = parameterString(calculateParameters(dialog.translations), dialog.translations.hasPlural())

            val message = when {
                StringLiteralUtils.needsSvelteWrapping(element) -> "{m.$methodName($params)}"
                else -> "m.$methodName($params)"
            }

            editor.document.replaceString(selection.start, selection.end, message)
            PsiDocumentManager.getInstance(project).commitDocument(editor.document)
        }
    }


    override fun generatePreview(project: Project, editor: Editor, psiFile: PsiFile): IntentionPreviewInfo {
        val element = psiFile.findElementAt(editor.caretModel.offset) ?: return IntentionPreviewInfo.EMPTY
        val selection = getSelection(editor, element)
        val humanID = humanID.generate()
        val params = parameterString(calculateParameters(selection.text), false)
        val message = when {
            StringLiteralUtils.needsSvelteWrapping(element) -> "{m.$humanID($params)}"
            else -> "m.$humanID($params)"
        }
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

        val effectiveElement = StringLiteralUtils.findStringLiteral(element)!!

        return Selection(effectiveElement.startOffset, effectiveElement.endOffset, effectiveElement.content)
    }

    private fun calculateParameters(txt: String): Set<String> {
        val regex = "\\{(\\w+)}".toRegex()
        return regex.findAll(txt).map { it.groupValues[1] }.toSet()
    }

    private fun calculateParameters(translations: Translations): Set<String> {
        val txt = translations
            .translations
            .values
            .joinToString(" ") {
                it.singular + if (it.pluralEnabled) it.plural else ""
            }
        return calculateParameters(txt)
    }

    private fun parameterString(params: Set<String>, hasPlural: Boolean): String {
        if (params.isEmpty()) {
            return ""
        }
        return params
            .plus(if (hasPlural) setOf("count") else emptySet())
            .joinToString(", ", "{ ", " }") {
                if (it == "count") {
                    "$it: 0"
                } else {
                    "$it: ''"
                }
            }
    }
}