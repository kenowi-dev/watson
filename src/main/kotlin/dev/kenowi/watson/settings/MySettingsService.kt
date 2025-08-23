package dev.kenowi.watson.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(name = "MySettingsService", storages = [Storage("mysettingsservice.xml")])
internal class MySettingsService(private val project: Project) :
    SerializablePersistentStateComponent<MySettingsServiceState>(MySettingsServiceState()) {
    companion object {
        fun getInstance(project: Project): MySettingsService = project.service()
    }

    var value: String?
        get() = state.storeValue
        set(value) {
            updateState {
                it.copy(storeValue = value)
            }
        }
}

internal data class MySettingsServiceState(
    @JvmField val storeValue: String? = null
)