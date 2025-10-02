package dev.kenowi.watson.intention

import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.*
import dev.kenowi.watson.services.InlangSettingsService
import java.util.*
import javax.swing.JComponent

class HumanIdOptionsDialog(val project: Project, val selection: String) : DialogWrapper(true) {

    companion object {
        private val ID_GENERATOR: HumanID = HumanID()
        private const val TITLE = "Human Id"
    }

    private val inlangSettingsService = InlangSettingsService.getInstance(project)
    private val inlangSettings = inlangSettingsService.getSettings()
    private val baseLocale = inlangSettings?.baseLocale ?: "en"

    var methodName: String = ID_GENERATOR.generate()
    var translations = mutableMapOf(baseLocale to selection)

    init {
        init()
        title = TITLE
    }

    fun previewString(txt: String): String = "<html><code>m.${txt}()</code></html>"

    override fun createCenterPanel(): JComponent {
        return panel {

            val propertyGraph = PropertyGraph()
            lateinit var previewLabel: JBLabel

            val methodNameProperty = propertyGraph.property(this@HumanIdOptionsDialog.methodName)
            methodNameProperty.afterChange { methodName = it }
            methodNameProperty.afterChange { previewLabel.text = previewString(it) }


            row("Extracted function name") {
                textField()
                    .bindText(methodNameProperty)
                    .validationOnInput {
                        when {
                            it.text.isEmpty() -> error("Name must not be empty")
                            it.text.matches(Regex("^[0-9].*")) -> error("Cannot start with a number")
                            !it.text.matches(Regex("^[a-z_][a-z_0-9]*$")) -> error("Name must only contain 'a-z' and '_' (snake_case)")
                            else -> null
                        }
                    }
                button("Generate New ID") {
                    methodNameProperty.set(ID_GENERATOR.generate())
                }
            }
            row {
                previewLabel = JBLabel(previewString(methodNameProperty.get()))
                cell(previewLabel)
                    .gap(RightGap.SMALL)
                    .enabled(false)
            }

            for (locale in inlangSettings?.locales ?: listOf()) {
                group {
                    row("$locale - ${Locale.forLanguageTag(locale).getDisplayName(Locale.US)}") {
                        textField()
                            .bindText(
                                { translations[locale] ?: "" },
                                { l -> translations[locale] = l })
                            .columns(COLUMNS_MEDIUM)
                    }
                }
            }
        }
    }
}