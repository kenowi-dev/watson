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
import com.intellij.util.indexing.FileBasedIndexExtension
import dev.kenowi.watson.WatsonMessageBundle
import dev.kenowi.watson.utils.ParaglideFunctionUsageIndex
import dev.kenowi.watson.services.ParaglideSettingsService

class ParaglideMessageKeyLineMarkerProvider : RelatedItemLineMarkerProvider() {
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

        val messageFilePaths = ParaglideSettingsService
            .getInstance(element.project)
            .getLocaleMessagesFilePaths()
            .values

        if (!messageFilePaths.contains(element.containingFile.virtualFile.path)) {
            return
        }

        val keyName = element.name
        val findFunctionCallsByName = ParaglideFunctionUsageIndex.findFunctionCallsByName2(element.project, keyName)

        if (findFunctionCallsByName.isEmpty()) {
            return
        }

        val marker = NavigationGutterIconBuilder
            .create(AllIcons.Gutter.ImplementedMethod)
            //.create(AllIcons.General.Language)
            //.create(AllIcons.Gutter.ImplementingFunctionalInterface)
            //.create(AllIcons.Nodes.Function)
            .setTargets(findFunctionCallsByName)
            .setTooltipText(WatsonMessageBundle.message("markers.function.navigate", keyName))
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