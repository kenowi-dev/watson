package dev.kenowi.watson.navigation.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.json.psi.JsonElementVisitor
import com.intellij.json.psi.JsonProperty
import com.intellij.psi.PsiElementVisitor
import dev.kenowi.watson.navigation.JsFunctionUsageIndex

class UnusedJsonKeyInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = "Unused JSON Key"
    override fun getShortName(): String = "UnusedJsonKey"
    override fun isEnabledByDefault(): Boolean = true

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JsonElementVisitor() {
            override fun visitProperty(property: JsonProperty) {
                val keyName = property.name
                val project = property.project

                val usages = JsFunctionUsageIndex.findFunctionCallsByName(project, keyName)
                if (usages.isEmpty()) {
                    holder.registerProblem(
                        property.nameElement,
                        "Key '$keyName' is not used",
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL
                    )
                }
            }
        }
    }
}