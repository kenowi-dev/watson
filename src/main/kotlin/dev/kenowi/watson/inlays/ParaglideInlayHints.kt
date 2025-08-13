package dev.kenowi.watson.inlays

import com.intellij.codeInsight.hints.declarative.InlayHintsCollector
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider
import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector
import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import com.intellij.psi.util.endOffset


class ParaglideInlayHints : InlayHintsProvider {

    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector? {
        if (!file.name.endsWith(".svelte")) {
            return null
        }
        return ThreadingCollector(file)
    }

    private class ThreadingCollector(file: PsiFile) : SharedBypassCollector {

        private var messages = mutableMapOf<String, String>()

        init {
            //messages = InlangUtils.readMessages(file)
            messages = mutableMapOf(
                "abc" to "def",
                "aaa" to "bbb"
            )
        }

        override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
            if (element is JSCallExpression) {
                if (element.parent.elementType.toString() !== "CONTENT_EXPRESSION") {
                    return
                }

                if (!element.text.trim().startsWith("m.")) {
                    return
                }

                element.children.find { it is JSReferenceExpression }?.let {
                    val key = it.text.replace("m.", "")
                    if (messages.containsKey(key) && messages[key] != "") {
                        sink.addPresentation(
                            InlineInlayPosition(element.endOffset, true),
                            tooltip = element.text,
                            hasBackground = true,
                        ) {
                            text(messages[key] ?: "")
                        }
                    }
                }
            }
        }
    }

}