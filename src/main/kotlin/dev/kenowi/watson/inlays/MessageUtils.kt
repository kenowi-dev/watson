package dev.kenowi.watson.inlays

import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.lang.javascript.psi.JSProperty
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import dev.kenowi.watson.services.InlangSettingsService
import dev.kenowi.watson.settings.WatsonSettings
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object MessageUtils {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var lastModificationStamp: Long = -1
    private var cachedMessages: ImmutableMap<String, String>? = null
    private val cachedMessagesLock = Any()

    fun loadMessages(project: Project): Map<String, String> {
        val inlangService = InlangSettingsService.getInstance(project)
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
                            messages[key] = value.jsonPrimitive.content
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
            return loadMessages(resolved.project).containsKey(methodName)
        }

        return false
    }

    fun getMessageText(element: PsiElement): String {
        if (element is JSCallExpression) {
            val messages = this.loadMessages(element.project)
            element.children.find { it is JSReferenceExpression }?.let {
                val key = it.text.replace("m.", "")
                val template = messages[key] ?: return element.text
                val args = extractArguments(element)
                return replacePlaceholders(template, args)
            }
        }
        return element.text
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