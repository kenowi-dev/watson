package dev.kenowi.watson.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import dev.kenowi.watson.settings.WatsonSettings
import kotlinx.io.IOException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Service(Service.Level.PROJECT)
internal class ParaglideSettingsService(private val project: Project) {

    private val settings = WatsonSettings.getInstance(project)
    private val notificationService = WatsonNotificationService.getInstance(project)
    private val json = Json {
        ignoreUnknownKeys = true // Ignore fields not in data class
        isLenient = true
    }

    private var cachedSettings: InlangSettings? = null
    private var lastModificationStamp: Long = -1

    companion object {
        fun getInstance(project: Project): ParaglideSettingsService = project.service()
    }

    @Serializable
    data class InlangSettings(
        val baseLocale: String,
        val locales: List<String>,
        val modules: List<String>,
        @SerialName("plugin.inlang.messageFormat")
        val messageFormatPlugin: Map<String, String>? = null,
    ) {
        val pathPattern: String?
            get() = messageFormatPlugin?.get("pathPattern")
    }

    fun InlangSettings.getMessageFilePath(locale: String): String? {
        val pattern = pathPattern ?: return null
        val relativePath = pattern.replace("{locale}", locale)
        val cleanPath = relativePath.removePrefix("./")
        return "${project.basePath}/$cleanPath"
    }

    fun getBaseLocaleMessageFilePath(): String? {
        val settings = getSettings() ?: return null
        return settings.getMessageFilePath(settings.baseLocale)
    }

    fun getLocaleMessagesFilePaths(): Map<String, String> {
        val settings = getSettings() ?: return emptyMap()

        return settings.locales.associateWith { locale ->
            settings.getMessageFilePath(locale) ?: ""
        }.filterValues { it.isNotEmpty() }
    }

    fun getSettings(): InlangSettings? {
        val filePath = "${settings.inlangProject}/${settings.inlangSettingsFile}"
        val settingsFile = LocalFileSystem.getInstance().findFileByPath(filePath)

        if (settingsFile == null) {
            notificationService.warn("Cannot find inlang settings at: $settingsFile")
            return null
        }

        val currentStamp = settingsFile.modificationStamp
        if (cachedSettings == null || currentStamp != lastModificationStamp) {
            val jsonContent = String(settingsFile.contentsToByteArray(), settingsFile.charset)
            cachedSettings = try {
                json.decodeFromString<InlangSettings>(jsonContent)
            } catch (_: IOException) {
                notificationService.warn("Settings file cannot be deserialized")
                null
            }
            lastModificationStamp = currentStamp
        }

        return cachedSettings
    }

    fun invalidateCache() {
        cachedSettings = null
        lastModificationStamp = -1
    }

}