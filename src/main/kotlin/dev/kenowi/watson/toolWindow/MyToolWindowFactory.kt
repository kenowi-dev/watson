package dev.kenowi.watson.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.panel
import dev.kenowi.watson.services.InlangSdkService


class MyToolWindowFactory() : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val inlangSdkService = InlangSdkService.getInstance(project)

        val panel = panel {
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
        }


        SimpleToolWindowPanel(true, true)
            .apply { setContent(panel) }
            .let {
                ContentFactory
                    .getInstance()
                    .createContent(it, null, false)
            }
            .let { toolWindow.contentManager.addContent(it) }


    }

    override fun shouldBeAvailable(project: Project) = true
}
