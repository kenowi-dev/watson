package dev.kenowi.watson.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.dsl.builder.*
import dev.kenowi.watson.bundles.WatsonSettingsBundle
import org.jetbrains.annotations.ApiStatus
import javax.swing.JComponent

class SettingConfigurable(private val project: Project) : Configurable {

    private val panel = panel()

    override fun getDisplayName(): @NlsContexts.ConfigurableName String {
        return WatsonSettingsBundle.message("title")
    }

    override fun createComponent(): JComponent {
        return panel
    }

    override fun isModified(): Boolean {
        return panel.isModified();
    }

    override fun reset() {
        panel.reset()
    }

    override fun apply() {
        panel.apply()
    }

    private fun panel(): DialogPanel {
        val settings = WatsonSettings.getInstance(project)
        return panel {
            row("inlang location:") {
                textFieldWithBrowseButton(
                    fileChooserDescriptor = FileChooserDescriptorFactory.singleFile(),
                    project = project,
                    fileChosen = VirtualFile::getPath,
                ).columns(COLUMNS_LARGE)
                    .bindText(settings::inlangLocation)
            }
            row("HumanID:") {
                checkBox("")
                    .bindSelected(settings::humanID)
            }
            row("Demo Setting:") {
                textField().bindText(settings::demoSetting)
            }
        }
    }
}