package dev.kenowi.watson.intention

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.InputStream

@OptIn(ExperimentalSerializationApi::class)
class HumanID {
    val words: WordsJson


    @Serializable
    data class WordsJson(
        val animals: List<String>,
        val adjectives: List<String>,
        val adverbs: List<String>,
        val verbs: List<String>
    )

    init {
        val inputStream: InputStream = javaClass.classLoader.getResourceAsStream("words.json")
            ?: throw IllegalArgumentException("Resource not found")

        words = Json.decodeFromStream<WordsJson>(inputStream)
    }

    fun generate(): String {
        return "${words.adjectives.random()}_" +
                "${words.adjectives.random()}_" +
                "${words.animals.random()}_${words.verbs.random()}"
    }
}