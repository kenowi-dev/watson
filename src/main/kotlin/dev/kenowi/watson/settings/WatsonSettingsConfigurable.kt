package dev.kenowi.watson.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import dev.kenowi.watson.WatsonMessageBundle

class WatsonSettingsConfigurable(private val project: Project):
    BoundConfigurable(WatsonMessageBundle.message("name")) {

    @Suppress("UnstableApiUsage")
    override fun createPanel(): DialogPanel {
        val settings = WatsonSettings.getInstance(project)
        return panel {
            row(WatsonMessageBundle.message("settings.paraglide.project.location")) {
                textFieldWithBrowseButton(
                    fileChooserDescriptor = FileChooserDescriptorFactory.singleDir(),
                    project = project,
                    fileChosen = VirtualFile::getPath,
                ).columns(COLUMNS_LARGE)
                    .bindText(settings::inlangProject)
            }

            row(WatsonMessageBundle.message("settings.paraglide.output.location")) {
                textFieldWithBrowseButton(
                    fileChooserDescriptor = FileChooserDescriptorFactory.singleDir(),
                    project = project,
                    fileChosen = VirtualFile::getPath,
                ).columns(COLUMNS_LARGE)
                    .bindText(settings::inlangOutDir)
            }

            row(WatsonMessageBundle.message("settings.inlay.hints")) {
                checkBox("")
                    .bindSelected(settings::useInlayHints)
            }
        }
    }
}