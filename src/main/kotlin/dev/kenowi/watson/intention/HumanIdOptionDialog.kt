package dev.kenowi.watson.intention

import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.*
import dev.kenowi.watson.services.InlangSettingsService
import java.util.*
import javax.swing.JComponent

class HumanIdOptionsDialog(val project: Project) : DialogWrapper(true) {

    companion object {
        private val ID_GENERATOR: HumanID = HumanID()
        private const val TITLE = "Human Id"
    }

    private val inlangSettingsService = InlangSettingsService.getInstance(project)
    private val inlangSettings = inlangSettingsService.getSettings()

    var methodName: String = ID_GENERATOR.generate()
    var translations = mutableMapOf<String, String>()

    init {
        init()
        title = TITLE
    }

    override fun createCenterPanel(): JComponent {
        return panel {

            val propertyGraph = PropertyGraph()
            val methodNameProperty = propertyGraph.property(this@HumanIdOptionsDialog.methodName)
            methodNameProperty.afterChange { methodName = it }

            row("Extracted function name") {
                textField()
                    .bindText(methodNameProperty)
                button("Generate New ID") {
                    methodNameProperty.set(ID_GENERATOR.generate())
                }
            }


            for (locale in inlangSettings?.locales ?: listOf()) {
                group {
                    row("$locale - ${Locale.forLanguageTag(locale).getDisplayName(Locale.US)}") {
                        textArea()
                            .bindText(
                                { translations[locale] ?: "" },
                                { l -> translations[locale] = l })
                            .rows(2)
                            .columns(COLUMNS_MEDIUM)
                    }
                }
            }
        }
    }
}