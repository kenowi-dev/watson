package dev.kenowi.watson.intention

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.*
import javax.swing.JComponent

class HumanIdOptionsDialog : DialogWrapper(true) {

    var lowercase: Boolean = true
    var separator: String = "-"

    private val dialogPanel = panel {
        row {
            checkBox("Convert to lowercase")
                .bindSelected(::lowercase)
        }

        row("Separator:") {
            textField()
                .bindText(::separator)
                .columns(10)
        }
    }

    init {
        init()
        title = "HumanID Options"
    }

    override fun createCenterPanel(): JComponent = dialogPanel
}