package dev.kenowi.watson.listeners

import com.intellij.lang.Language
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StartupPopup : ProjectActivity {

    override suspend fun execute(project: Project) {


        val allLanguages = Language.getRegisteredLanguages()
        for (language in allLanguages) {
            println("Language ID: ${language.id}, Display Name: ${language.displayName}")
        }

        // Switch to Main thread for UI-related actions
        withContext(Dispatchers.Main) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Test Notification")
                .createNotification(
                    "My plugin",
                    "Plugin started successfully!",
                    NotificationType.INFORMATION
                )
                .notify(project)
        }
    }
}