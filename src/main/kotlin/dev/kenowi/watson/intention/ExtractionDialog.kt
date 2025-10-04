package dev.kenowi.watson.intention

import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.not
import dev.kenowi.watson.WatsonMessageBundle
import dev.kenowi.watson.services.ParaglideSettingsService
import java.util.*
import javax.swing.JComponent

class ExtractionDialog(val project: Project, val selection: String) : DialogWrapper(true) {

    companion object {
        private val ID_GENERATOR: HumanID = HumanID()
    }

    private val paraglideSettingsService = ParaglideSettingsService.getInstance(project)
    private val inlangSettings = paraglideSettingsService.getSettings()
    private val baseLocale = inlangSettings?.baseLocale ?: "en"

    var methodName: String = ID_GENERATOR.generate()
    var translations = Translations(mutableMapOf(baseLocale to Translation(selection, "", false)))

    init {
        init()
        title = WatsonMessageBundle.message("intention.dialog.title")
    }

    fun previewString(txt: String): String = "<html><code>m.${txt}()</code></html>"

    override fun createCenterPanel(): JComponent {
        val propertyGraph = PropertyGraph()
        return panel {

            group(WatsonMessageBundle.message("intention.dialog.function.name")) {
                val methodNameProperty = propertyGraph.property(this@ExtractionDialog.methodName)
                methodNameProperty.afterChange { methodName = it }

                row(WatsonMessageBundle.message("intention.dialog.function.extracted")) {
                    textField()
                        .bindText(methodNameProperty)
                        .validationOnInput {
                            when {
                                it.text.isEmpty() -> error(WatsonMessageBundle.message("intention.dialog.error.empty"))
                                it.text.matches(Regex("^[0-9].*")) -> error(WatsonMessageBundle.message("intention.dialog.error.number"))
                                !it.text.matches(Regex("^[a-z_][a-z_0-9]*$")) -> error(WatsonMessageBundle.message("intention.dialog.error.pattern"))
                                else -> null
                            }
                        }
                    button(WatsonMessageBundle.message("intention.dialog.function.generate")) {
                        methodNameProperty.set(ID_GENERATOR.generate())
                    }
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

            group(WatsonMessageBundle.message("intention.dialog.translations")) {

                for (locale in inlangSettings?.locales ?: listOf()) {
                    val msgSingular = propertyGraph.property(translations.get(locale).singular)
                    msgSingular.afterChange { translations.singular(locale, it) }

                    val msgPlural = propertyGraph.property(translations.get(locale).plural)
                    msgPlural.afterChange { translations.plural(locale, it) }

                    val localeName = Locale.forLanguageTag(locale).getDisplayName(Locale.US)
                    lateinit var plural: Cell<JBCheckBox>

                    group("$locale - $localeName") {
                        row {
                            plural = checkBox(WatsonMessageBundle.message("intention.dialog.translations.plural"))
                                .selected(translations.get(locale).pluralEnabled)
                                .onChanged { translations.enablePlural(locale, it.isSelected) }
                        }
                        row {
                            textField()
                                .bindText(msgSingular)
                                .columns(COLUMNS_MEDIUM)
                        }.visibleIf(plural.selected.not())

                        row(WatsonMessageBundle.message("intention.dialog.translations.singular")) {
                            textField()
                                .bindText(msgSingular)
                                .columns(COLUMNS_MEDIUM)
                        }.visibleIf(plural.selected)

                        row(WatsonMessageBundle.message("intention.dialog.translations.plural")) {
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