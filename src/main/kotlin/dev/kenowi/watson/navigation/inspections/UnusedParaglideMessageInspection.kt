package dev.kenowi.watson.navigation.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.json.psi.JsonElementVisitor
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.psi.PsiElementVisitor
import dev.kenowi.watson.WatsonMessageBundle
import dev.kenowi.watson.utils.ParaglideFunctionUsageIndex

class UnusedParaglideMessageInspection : LocalInspectionTool() {

    override fun isEnabledByDefault(): Boolean = true

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JsonElementVisitor() {
            override fun visitProperty(property: JsonProperty) {

                if (property.parent !is JsonObject || property.parent.parent !is JsonFile) {
                    return
                }

                val keyName = property.name
                val project = property.project

                val usages = ParaglideFunctionUsageIndex.findFunctionCallsByName(project, keyName)
                if (usages.isEmpty()) {
                    holder.registerProblem(
                        property.nameElement,
                        WatsonMessageBundle.message("inspection.description.template", keyName),
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL
                    )
                }
            }
        }
    }
}