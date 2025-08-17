package dev.kenowi.watson.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.util.NlsContexts
import dev.kenowi.watson.WatsonMessageBundle
import javax.swing.JComponent

class SettingConfigurable: Configurable {



    override fun getDisplayName(): @NlsContexts.ConfigurableName String {
        return WatsonMessageBundle.message("watson.messages.settings.name")
    }

    override fun createComponent(): JComponent {
        return Settings2()
    }

    override fun isModified(): Boolean {
        return false
    }

    override fun apply() {
        TODO("Not yet implemented")
    }
}