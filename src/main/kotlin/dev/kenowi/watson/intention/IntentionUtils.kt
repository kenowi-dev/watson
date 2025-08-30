package dev.kenowi.watson.intention

import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.lang.javascript.psi.ecma6.TypeScriptLiteralType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlText
import dev.blachut.svelte.lang.psi.SvelteHtmlTag


object IntentionUtils {

    data class StringLiteralInfo(
        val element: PsiElement,
        val content: String,
        val startOffset: Int,
        val endOffset: Int,
        val jsContext: Boolean,
    ) {

        constructor(element: PsiElement, content: String, jsContext: Boolean = false) : this(
            element = element,
            content = if (isStringLiteralText(content)) extractStringContent(content) else content,
            startOffset = element.textRange.startOffset,
            endOffset = element.textRange.endOffset,
            jsContext = jsContext
        )

        constructor(element: PsiElement, jsContext: Boolean = false) : this(
            element = element,
            content = element.text,
            jsContext = jsContext
        )
    }

    fun isJavaScriptFamily(file: PsiFile): Boolean {
        val language = file.language
        val fileName = file.name.lowercase()
        val fileTypeName = file.fileType.name.lowercase()


        return when {
            language.id in setOf(
                "SvelteJS",
                "SvelteHTML",
                "SvelteTS",
                "HTML",
                "XML",
                "JavaScript",
                "TypeScript",
                "ECMAScript 6",
                "ECMA Script Level 4"
            ) -> true

            fileName.endsWith(".js") -> true
            fileName.endsWith(".ts") -> true
            fileName.endsWith(".jsx") -> true
            fileName.endsWith(".tsx") -> true
            fileName.endsWith(".mjs") -> true
            fileName.endsWith(".svelte") -> true

            fileTypeName in setOf("javascript", "typescript", "svelte") -> true

            else -> false
        }
    }

    fun findStringLiteral(element: PsiElement): StringLiteralInfo? {

        val p1 = PsiTreeUtil.getParentOfType(element, JSLiteralExpression::class.java)
        val p2 = PsiTreeUtil.getParentOfType(element, TypeScriptLiteralType::class.java)
        val p3 = PsiTreeUtil.getParentOfType(element, XmlText::class.java)
        // if element.elementType === XML_ATTRIBUTE_VALUE_TOKEN -> true (But needs to be wrapped into svelte expression {})
        val p4 = PsiTreeUtil.getParentOfType(element, XmlAttributeValue::class.java)
        val p5 = PsiTreeUtil.getParentOfType(element, SvelteHtmlTag::class.java)

        return when {
            p1 != null -> StringLiteralInfo(p1, p1.stringValue ?: "", true)
            p2 != null -> StringLiteralInfo(p2, p2.innerText ?: "", true)
            p3 != null -> StringLiteralInfo(p3)
            p4 != null -> StringLiteralInfo(p4)
            p5 != null -> StringLiteralInfo(p5)
            else -> null
        }
    }

    private fun isStringLiteralText(text: String): Boolean {
        val trimmed = text.trim()
        return when {
            trimmed.length < 2 -> false
            trimmed.startsWith("\"") && trimmed.endsWith("\"") -> true
            trimmed.startsWith("'") && trimmed.endsWith("'") -> true
            trimmed.startsWith("`") && trimmed.endsWith("`") -> true
            else -> false
        }
    }

    private fun extractStringContent(literalText: String): String {
        val trimmed = literalText.trim()
        return if (trimmed.length >= 2) {
            trimmed.substring(1, trimmed.length - 1)
        } else {
            ""
        }
    }

}
