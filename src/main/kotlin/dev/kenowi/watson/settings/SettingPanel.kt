package dev.kenowi.watson.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.dsl.builder.*
import dev.kenowi.watson.WatsonMessageBundle

private class SettingConfigurable(private val project: Project) :
    BoundConfigurable(WatsonMessageBundle.message("name")) {

    @Suppress("UnstableApiUsage")
    override fun createPanel(): DialogPanel {
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