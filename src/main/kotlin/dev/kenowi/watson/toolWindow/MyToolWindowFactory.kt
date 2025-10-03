package dev.kenowi.watson.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.panel
import dev.kenowi.watson.MessageUtils
import dev.kenowi.watson.services.InlangSdkService
import dev.kenowi.watson.services.InlangSettingsService


class MyToolWindowFactory() : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val inlangSdkService = InlangSdkService.getInstance(project)

        val mainPanel = panel {
            row {
                label("Watson tool window")
            }
            row {
                textField().label("Text field")
            }
            row {
                button("Compile Messages In Console") {
                    inlangSdkService.compileMessageInConsole()
                }
                button("Compile Messages In Background") {
                    inlangSdkService.compileMessagesBackground()
                }
            }
            group("Inlang Settings (Debug)") {
                val service = InlangSettingsService.getInstance(project)
                val settings = service.getSettings()

                row("Status:") {
                    label(if (settings != null) "✓ Loaded" else "✗ Not found")
                }

                settings?.let {
                    row("Base Locale:") {
                        label(it.baseLocale)
                    }

                    row("Locales:") {
                        label(it.locales.joinToString(", "))
                    }

                    row("Path Pattern:") {
                        label(it.pathPattern ?: "N/A")
                    }

                    separator()

                    row {
                        label("Message Files:")
                    }

                    service.getLocaleMessagesFilePaths().forEach { (locale, path) ->
                        row("  $locale:") {
                            label(path)
                        }
                    }

                    row {
                        button("Invalidate Cache") {
                            service.invalidateCache()
                            MessageUtils.clearCache()
                        }
                    }
                }
            }
        }


        SimpleToolWindowPanel(true, true)
            .apply { setContent(mainPanel) }
            .let {
                ContentFactory
                    .getInstance()
                    .createContent(
                        it,
                        null,
                        false
                    )
            }
            .let {
                toolWindow.contentManager.addContent(
                    it
                )
            }


    }

    override fun shouldBeAvailable(project: Project) =
        true
}
