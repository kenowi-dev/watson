package dev.kenowi.watson.navigation.references

import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.ResolveResult
import com.intellij.util.ProcessingContext
import dev.kenowi.watson.utils.ParaglideFunctionUsageIndex
import dev.kenowi.watson.services.ParaglideSettingsService

class ParaglideMessageKeyReferenceContributor : PsiReferenceContributor() {

    class JsonKeyToJsFunctionReference(
        element: JsonStringLiteral,
        private val key: String
    ) : PsiPolyVariantReferenceBase<JsonStringLiteral>(element, TextRange(1, element.textLength - 1)) {

        override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
            if (myElement.parent !is JsonProperty
                || (myElement.parent as JsonProperty).nameElement != myElement
                || myElement.parent.parent !is JsonObject
                || myElement.parent.parent.parent !is JsonFile) {
                return emptyArray()
            }

            val project = myElement.project
            val results = ParaglideFunctionUsageIndex.findFunctionCallsByName(project, key)

            return results.map { PsiElementResolveResult(it) }.toTypedArray()
        }

        override fun getVariants(): Array<Any> = emptyArray()
    }

    class JsonKeyReferenceProvider : PsiReferenceProvider() {
        override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
            if (element !is JsonStringLiteral) {
                return PsiReference.EMPTY_ARRAY
            }

            return arrayOf(JsonKeyToJsFunctionReference(element, element.value))
        }
    }

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {

        val pattern = PlatformPatterns.psiElement(JsonStringLiteral::class.java)
            .inFile(
                PlatformPatterns.psiFile()
                    .with(object : PatternCondition<PsiFile>("filePathMatches") {
                        override fun accepts(file: PsiFile, context: ProcessingContext?): Boolean {
                            val vFile = file.virtualFile ?: return false
                            val messageFilePaths = ParaglideSettingsService
                                .getInstance(file.project)
                                .getLocaleMessagesFilePaths()
                                .values

                            return messageFilePaths.contains(vFile.path)
                        }
                    })
            )

        registrar.registerReferenceProvider(pattern, JsonKeyReferenceProvider())
    }
}