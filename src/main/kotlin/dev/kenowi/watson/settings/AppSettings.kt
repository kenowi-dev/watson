package dev.kenowi.watson.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import org.jetbrains.annotations.NonNls
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "dev.kenowi.watson.settings.AppSettings", storages = [Storage("SdkSettingsPlugin.xml")])
internal class AppSettings: PersistentStateComponent<AppSettings.State?> {

    internal class State {
        @NonNls
        var userId: String = "John Smith"
        var ideaStatus: Boolean = false
    }

    private var myState: State? = State()

    override fun getState(): State? {
        return myState
    }

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        val instance: AppSettings?
            get() = ApplicationManager.getApplication()
                .getService<AppSettings?>(AppSettings::class.java)
    }
}