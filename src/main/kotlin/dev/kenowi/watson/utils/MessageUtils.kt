package dev.kenowi.watson.utils

import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.lang.javascript.psi.JSProperty
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.jetbrains.rd.util.first
import dev.kenowi.watson.services.ParaglideSettingsService
import dev.kenowi.watson.services.WatsonNotificationService
import dev.kenowi.watson.settings.WatsonSettings
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.collections.get
import kotlin.collections.iterator

object MessageUtils {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var lastModificationStamp: Long = -1
    private var cachedMessages: ImmutableMap<String, String>? = null
    private val cachedMessagesLock = Any()

    fun clearCache() {
        synchronized(cachedMessagesLock) {
            cachedMessages = null
            lastModificationStamp = -1
        }
    }

    fun loadBaseLocaleMessages(project: Project): Map<String, String> {
        val inlangService = ParaglideSettingsService.getInstance(project)
        val messagesPath = inlangService.getBaseLocaleMessageFilePath() ?: return mutableMapOf()

        val messagesFile = LocalFileSystem.getInstance().findFileByPath(messagesPath)
        if (messagesFile == null || !messagesFile.exists()) {
            return mutableMapOf()
        }
        val currentStamp = messagesFile.modificationStamp
        if (cachedMessages == null || currentStamp != lastModificationStamp) {
            synchronized(cachedMessagesLock) {
                if (cachedMessages == null || currentStamp != lastModificationStamp) {
                    val jsonContent = String(messagesFile.contentsToByteArray(), messagesFile.charset)
                    val messages = mutableMapOf<String, String>()
                    try {
                        val jsonElement = json.parseToJsonElement(jsonContent)
                        val rootObject = jsonElement.jsonObject

                        for ((key, value) in rootObject) {
                            val msg = parseMessageObject(value)
                            if (msg == null) {
                                WatsonNotificationService.getInstance(project).error("Unsupported message format: $key")
                                continue
                            }
                            messages[key] = msg
                        }
                        cachedMessages = messages.toImmutableMap()
                        lastModificationStamp = currentStamp
                    } catch (_: IOException) {
                        // Silently fail and return empty map
                    }
                }
            }
        }
        return cachedMessages ?: persistentMapOf()
    }

    fun isMessageCall(element: JSCallExpression): Boolean {
        val methodExpression = element.methodExpression ?: return false

        if (methodExpression !is JSReferenceExpression) {
            return false
        }

        // Get the qualifier (the 'm' in 'm.hello_world')
        val qualifier = methodExpression.qualifier
        if (qualifier !is JSReferenceExpression) {
            return false
        }

        val resolved = qualifier.resolve()
        if (resolved != null) {
            val inlangOutDir = WatsonSettings.getInstance(resolved.project).inlangOutDir
            // Check if the qualifier resolves to the outDir (meaning it is part of inlang)
            if (!resolved.containingFile.parent?.virtualFile?.path.equals(inlangOutDir)) {
                return false
            }
            // Check if message exists
            val methodName = methodExpression.referenceName ?: return false
            return loadBaseLocaleMessages(resolved.project).containsKey(methodName)
        }

        return false
    }

    fun getMessageText(element: PsiElement): String {
        if (element is JSCallExpression) {
            val messages = this.loadBaseLocaleMessages(element.project)
            val methodExpression = element.methodExpression
            if (methodExpression is JSReferenceExpression) {
                val key = methodExpression.referenceName
                val template = messages[key]
                if (template != null) {
                    val args = extractArguments(element)
                    return replacePlaceholders(template, args)
                }
            }
        }
        return element.text
    }

    private fun parseMessageObject(value: JsonElement): String? {
        val match = when (value) {
            is JsonPrimitive -> return value.jsonPrimitive.content
            is JsonObject -> value.jsonObject["match"]
            is JsonArray if value.jsonArray.isNotEmpty() -> value.jsonArray[0].jsonObject["match"]
            else -> return null
        }
        if (match !is JsonObject || match.jsonObject.isEmpty()) {
            return null
        }
        val msg = match.jsonObject.first().value
        if (msg !is JsonPrimitive) {
            return null
        }
        return msg.jsonPrimitive.content
    }

    private fun extractArguments(callExpression: JSCallExpression): Map<String, String> {
        val args = mutableMapOf<String, String>()

        // Find the argument list
        val argumentList = callExpression.argumentList ?: return args

        // Get the first argument (should be an object literal)
        val firstArg = argumentList.arguments.firstOrNull() ?: return args

        if (firstArg is JSObjectLiteralExpression) {
            // Iterate through properties in the object
            for (property in firstArg.properties) {
                if (property is JSProperty) {
                    val propName = property.name ?: continue
                    val propValue = property.value?.text?.trim('"', '\'') ?: continue
                    args[propName] = propValue
                }
            }
        }

        return args
    }

    private fun replacePlaceholders(template: String, args: Map<String, String>): String {
        var result = template

        // Replace {placeholder} with actual values
        for ((key, value) in args) {
            result = result.replace("{$key}", value)
        }

        return result
    }
}