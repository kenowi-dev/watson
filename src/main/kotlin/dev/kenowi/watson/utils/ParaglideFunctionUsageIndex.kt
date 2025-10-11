package dev.kenowi.watson.utils

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
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.FileBasedIndex
import dev.blachut.svelte.lang.SvelteHtmlFileType
import dev.kenowi.watson.indexes.ParaglideFunctionCallIndex

object ParaglideFunctionUsageIndex {

    val fileTypes = setOf(
        JavaScriptFileType,
        TypeScriptFileType,
        SvelteHtmlFileType
    )

    /**
     * Find all PSI elements where a function with the given name is called.
     */
    fun findFunctionCallsByName2(project: Project, functionName: String): List<PsiElement> {
        val result = mutableListOf<PsiElement>()
        val scope = GlobalSearchScope.projectScope(project)
        val psiManager = PsiManager.getInstance(project)
        val index = FileBasedIndex.getInstance()

        // Query the index for all files that contain calls to this function
        index.processValues(
            ParaglideFunctionCallIndex.NAME,
            functionName,
            null,
            { file, offsets ->
                val psiFile = psiManager.findFile(file) ?: return@processValues true

                // For each offset, find the actual PsiElement
                offsets.forEach { offset ->
                    val element = psiFile.findElementAt(offset)
                    if (element != null) {
                        // Find the JSCallExpression that contains this element
                        val callExpression = PsiTreeUtil.getParentOfType(
                            element,
                            JSCallExpression::class.java
                        )
                        if (callExpression != null) {
                            result.add(callExpression)
                        }
                    }
                }

                true // continue processing
            },
            scope
        )

        return result
    }

    /**
     * Check if a function name has any usages in the project.
     * This is faster than findFunctionCallsByName when you only need to know if usages exist.
     */
    fun hasUsages(project: Project, functionName: String): Boolean {
        val scope = GlobalSearchScope.projectScope(project)
        val index = FileBasedIndex.getInstance()

        return index.processValues(
            ParaglideFunctionCallIndex.NAME,
            functionName,
            null,
            { _, offsets ->
                if (offsets.isNotEmpty()) {
                    false // stop processing, we found at least one usage
                } else {
                    true // continue
                }
            },
            scope
        )
    }



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