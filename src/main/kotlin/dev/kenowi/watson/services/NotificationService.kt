package dev.kenowi.watson.services

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
internal class NotificationService(private val project: Project) {

    companion object {
        fun getInstance(project: Project): NotificationService = project.service()
    }

    fun info(msg: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Watson Notifications")
            .createNotification(
                "Watson",
                msg,
                NotificationType.INFORMATION
            )
            .notify(project)
    }

    fun warn(msg: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Watson Notifications")
            .createNotification(
                "Watson",
                msg,
                NotificationType.WARNING
            )
            .notify(project)
    }

    fun error(msg: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Watson Notifications")
            .createNotification(
                "Watson",
                msg,
                NotificationType.ERROR
            )
            .notify(project)
    }
}