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
            row("Inlang Project Location:") {
                textFieldWithBrowseButton(
                    fileChooserDescriptor = FileChooserDescriptorFactory.singleDir(),
                    project = project,
                    fileChosen = VirtualFile::getPath,
                ).columns(COLUMNS_LARGE)
                    .bindText(settings::inlangProject)
            }

            row("Inlang Output Directory:") {
                textFieldWithBrowseButton(
                    fileChooserDescriptor = FileChooserDescriptorFactory.singleDir(),
                    project = project,
                    fileChosen = VirtualFile::getPath,
                ).columns(COLUMNS_LARGE)
                    .bindText(settings::inlangOutDir)
            }

            row("HumanID:") {
                checkBox("")
                    .bindSelected(settings::humanID)
            }

            row("Use inlay hints instead:") {
                checkBox("")
                    .bindSelected(settings::useInlayHints)
            }
        }
    }
}