package dev.kenowi.watson.intention

import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.not
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
    var translations = Translations(mutableMapOf(baseLocale to Translation(selection, "", false)))

    init {
        init()
        title = TITLE
    }

    fun previewString(txt: String): String = "<html><code>m.${txt}()</code></html>"

    override fun createCenterPanel(): JComponent {
        val propertyGraph = PropertyGraph()
        return panel {

            group("Function Name") {
                val methodNameProperty = propertyGraph.property(this@HumanIdOptionsDialog.methodName)
                methodNameProperty.afterChange { methodName = it }

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
                    button("Generate New ID") { methodNameProperty.set(ID_GENERATOR.generate()) }
                }


                val methodPreview = propertyGraph.property(previewString(methodName))
                methodNameProperty.afterChange { methodPreview.set(previewString(it)) }
                row {
                    label("")
                        .bindText(methodPreview)
                        .gap(RightGap.SMALL)
                        .enabled(false)
                }
            }

            group("Translations") {

                for (locale in inlangSettings?.locales ?: listOf()) {
                    val msgSingular = propertyGraph.property(translations.get(locale).singular)
                    msgSingular.afterChange { translations.singular(locale, it) }

                    val msgPlural = propertyGraph.property(translations.get(locale).plural)
                    msgPlural.afterChange { translations.plural(locale, it) }

                    val localeName = Locale.forLanguageTag(locale).getDisplayName(Locale.US)
                    lateinit var plural: Cell<JBCheckBox>

                    group("$locale - $localeName") {
                        row {
                            plural = checkBox("Plural")
                                .selected(translations.get(locale).pluralEnabled)
                                .onChanged { translations.enablePlural(locale, it.isSelected) }
                        }
                        row {
                            textField()
                                .bindText(msgSingular)
                                .columns(COLUMNS_MEDIUM)
                        }.visibleIf(plural.selected.not())

                        row("Singular") {
                            textField()
                                .bindText(msgSingular)
                                .columns(COLUMNS_MEDIUM)
                        }.visibleIf(plural.selected)

                        row("Plural") {
                            textField()
                                .bindText(msgPlural)
                                .columns(COLUMNS_MEDIUM)
                        }.visibleIf(plural.selected)
                    }
                }
            }
        }
    }
}