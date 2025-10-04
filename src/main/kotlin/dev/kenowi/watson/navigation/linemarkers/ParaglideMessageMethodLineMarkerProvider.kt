package dev.kenowi.watson.navigation.linemarkers

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer
import com.intellij.icons.AllIcons
import com.intellij.json.psi.JsonProperty
import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.psi.PsiElement
import dev.kenowi.watson.WatsonMessageBundle
import dev.kenowi.watson.utils.ParaglideMessageKeyIndex

class ParaglideMessageMethodLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (element !is JSCallExpression) {
            return
        }

        val methodExpr = element.methodExpression

        if (methodExpr !is JSReferenceExpression) {
            return
        }

        if (methodExpr.qualifier?.text != "m") {
            return
        }

        val referenceName = methodExpr.referenceName ?: return

        val marker = NavigationGutterIconBuilder
            .create(AllIcons.General.Language)
            .setTargets(ParaglideMessageKeyIndex.findMessageNames(referenceName, element.project))
            .setTooltipText(WatsonMessageBundle.message("markers.message.navigate", referenceName))
            .setTargetRenderer {
                object : PsiTargetPresentationRenderer<PsiElement>() {
                    override fun getElementText(element: PsiElement): String {
                        val jsonProperty = element as? JsonProperty
                        return jsonProperty?.name ?: "<unknown>"
                    }

                    override fun getContainerText(element: PsiElement): String {
                        val file = element.containingFile
                        return "in ${file.containingDirectory?.name}/${file.name}"
                    }
                }
            }

            .createLineMarkerInfo(methodExpr.referenceNameElement ?: methodExpr.firstChild)

        result.add(marker)

    }
}