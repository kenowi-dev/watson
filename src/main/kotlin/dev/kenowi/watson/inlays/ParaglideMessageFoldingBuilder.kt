package dev.kenowi.watson.inlays

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import dev.kenowi.watson.utils.MessageUtils
import dev.kenowi.watson.settings.WatsonSettings

class ParaglideMessageFoldingBuilder : FoldingBuilderEx() {

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val settings = WatsonSettings.getInstance(root.project)
        if (settings.useInlayHints) {
            return arrayOf()
        }

        val descriptors = mutableListOf<FoldingDescriptor>()

        root.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)

                if (element !is JSCallExpression) {
                    return
                }

                if (!MessageUtils.isMessageCall(element)) {
                    return
                }
                descriptors.add(FoldingDescriptor(element.node, element.textRange))
            }
        })

        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String? {
        val element = node.psi
        val settings = WatsonSettings.getInstance(element.project)
        if (settings.useInlayHints) {
            return element.text
        }

        if (element is JSCallExpression) {
            return MessageUtils.getMessageText(element)
        }
        return element.text
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        return true
    }
}