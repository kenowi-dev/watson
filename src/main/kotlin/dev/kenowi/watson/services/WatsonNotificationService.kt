package dev.kenowi.watson.services

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import dev.kenowi.watson.WatsonMessageBundle

@Service(Service.Level.PROJECT)
internal class WatsonNotificationService(private val project: Project) {

    companion object {
        fun getInstance(project: Project): WatsonNotificationService = project.service()
    }

    fun info(msg: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Watson Notifications")
            .createNotification(
                WatsonMessageBundle.message("name"),
                msg,
                NotificationType.INFORMATION
            )
            .notify(project)
    }

    fun warn(msg: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Watson Notifications")
            .createNotification(
                WatsonMessageBundle.message("name"),
                msg,
                NotificationType.WARNING
            )
            .notify(project)
    }

    fun error(msg: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Watson Notifications")
            .createNotification(
                WatsonMessageBundle.message("name"),
                msg,
                NotificationType.ERROR
            )
            .notify(project)
    }
}