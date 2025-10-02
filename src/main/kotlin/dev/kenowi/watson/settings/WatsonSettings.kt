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

    var useInlayHints: Boolean
        get() = state.useInlayHints
        set(value) {
            updateState { it.copy(useInlayHints = value) }
        }

    var compileAfterExtract: Boolean
        get() = state.compileAfterExtract
        set(value) {
            updateState { it.copy(compileAfterExtract = value) }
        }

    var inlangProject: String
        get() = state.inlangProject
        set(value) {
            updateState { it.copy(inlangProject = value) }
        }

    var inlangSettingsFile: String
        get() = state.inlangSettingsFile
        set(value) {
            updateState { it.copy(inlangSettingsFile = value) }
        }

    var inlangOutDir: String
        get() = state.inlangOutDir
        set(value) {
            updateState { it.copy(inlangOutDir = value) }
        }
}

@Serializable
internal data class WatsonSettingsState(
    var humanID: Boolean = true,
    var inlangProject: String = "",
    var inlangSettingsFile: String = "settings.json",
    var inlangOutDir: String = "",
    var useInlayHints: Boolean = false,
    var compileAfterExtract: Boolean = true,
) {

    constructor(project: Project) : this(
        inlangProject = "${project.basePath}/project.inlang",
        inlangOutDir = "${project.basePath}/src/lib/paraglide"
    )

}