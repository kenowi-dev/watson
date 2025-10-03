package dev.kenowi.watson.navigation.linemarkers

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer
import com.intellij.icons.AllIcons
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.psi.PsiElement
import dev.kenowi.watson.navigation.JsFunctionUsageIndex
import dev.kenowi.watson.services.InlangSettingsService

class JsonKeyLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (element !is JsonProperty) {
            return
        }

        if (element.parent !is JsonObject || element.parent.parent !is JsonFile) {
            return
        }

        val messageFilePaths = InlangSettingsService
            .getInstance(element.project)
            .getLocaleMessagesFilePaths()
            .values

        if (!messageFilePaths.contains(element.containingFile.virtualFile.path)) {
            return
        }

        val keyName = element.name
        val findFunctionCallsByName = JsFunctionUsageIndex.findFunctionCallsByName(element.project, keyName)

        if (findFunctionCallsByName.isEmpty()) {
            return
        }

        val marker = NavigationGutterIconBuilder
            .create(AllIcons.Gutter.ImplementedMethod)
            //.create(AllIcons.General.Language)
            //.create(AllIcons.Gutter.ImplementingFunctionalInterface)
            //.create(AllIcons.Nodes.Function)
            .setTargets(findFunctionCallsByName)
            .setTooltipText("Navigate to function: $keyName")
            .setTargetRenderer() {
                object : PsiTargetPresentationRenderer<PsiElement>() {
                    override fun getContainerText(element: PsiElement): String {
                        val file = element.containingFile
                        return "in ${file.containingDirectory.name}/${file.name}"
                    }
                }
            }
            .createLineMarkerInfo(element.nameElement.firstChild)

        result.add(marker)
    }
}