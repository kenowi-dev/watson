package dev.kenowi.watson.inlays

import com.intellij.codeInsight.hints.declarative.*
import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.endOffset
import dev.kenowi.watson.MessageUtils
import dev.kenowi.watson.settings.WatsonSettings


class ParaglideInlayHints : InlayHintsProvider {

    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector? {
        val settings = WatsonSettings.getInstance(file.project)
        if (!settings.useInlayHints) {
            return null
        }

        return ThreadingCollector()
    }

    private class ThreadingCollector() : SharedBypassCollector {

        override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
            if (element !is JSCallExpression) {
                return
            }

            if (!MessageUtils.isMessageCall(element)) {
                return
            }

            sink.addPresentation(
                InlineInlayPosition(element.endOffset, true),
                hintFormat = HintFormat.default,
                tooltip = element.text,
            ) {
                text(MessageUtils.getMessageText(element))
            }
        }
    }

}