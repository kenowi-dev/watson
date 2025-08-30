package dev.kenowi.watson.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import kotlinx.serialization.Serializable

@Service(Service.Level.PROJECT)
@State(name = "WatsonSettings", storages = [Storage("watsonsettings.json")])
internal class WatsonSettings(project: Project) :
    SerializablePersistentStateComponent<WatsonSettingsState>(WatsonSettingsState(project)) {

    companion object {
        fun getInstance(project: Project): WatsonSettings = project.service()
    }

    var humanID: Boolean
        get() = state.humanID
        set(value) {
            updateState { it.copy(humanID = value) }
        }

    var inlangLocation: String
        get() = state.inlangLocation
        set(value) {
            updateState { it.copy(inlangLocation = value) }
        }

    var demoSetting: String
        get() = state.demoSetting
        set(value) {
            updateState { it.copy(demoSetting = value) }
        }
}

@Serializable
internal data class WatsonSettingsState(
    var humanID: Boolean = true,
    var inlangLocation: String = "",
    var demoSetting: String = ""
) {

    constructor(project: Project) : this(
        humanID = true,
        inlangLocation = "${project.basePath}/project.inlang/settings.json"
    )

}