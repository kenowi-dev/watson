package dev.kenowi.watson.navigation

import com.intellij.lang.javascript.JavaScriptFileType
import com.intellij.lang.javascript.TypeScriptFileType
import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import dev.blachut.svelte.lang.SvelteHtmlFileType
import dev.kenowi.watson.MessageUtils

object JsFunctionUsageIndex {

    val fileTypes = setOf(
        JavaScriptFileType,
        TypeScriptFileType,
        SvelteHtmlFileType
    )

    fun findFunctionCallsByName(project: Project, name: String): List<PsiElement> {
        val result = mutableListOf<PsiElement>()
        val scope = GlobalSearchScope.projectScope(project)
        val psiManager = PsiManager.getInstance(project)

        for (type in fileTypes) {
            FileTypeIndex.getFiles(type, scope).forEach { virtualFile ->
                val psiFile = psiManager.findFile(virtualFile) ?: return@forEach

                psiFile.accept(object : PsiRecursiveElementVisitor() {
                    override fun visitElement(element: PsiElement) {
                        super.visitElement(element)

                        if (element is JSCallExpression ) {
                            val methodExpr = element.methodExpression
                            if (methodExpr is JSReferenceExpression) {
                                if (methodExpr.referenceName == name && MessageUtils.isMessageCall(element)) {
                                    result.add(element)
                                }
                            }
                        }
                    }
                })
            }
        }

        return result
    }
}