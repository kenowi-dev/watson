package dev.kenowi.watson.intention

data class Translation(var singular: String, var plural: String, var pluralEnabled: Boolean) {
    companion object {
        fun default(): Translation {
            return Translation("", "", false)
        }
    }

    fun toJsonString(): String {
        if (!pluralEnabled) {
            return "\"$singular\""
        }
        return """
            |[{
            |   "declarations": ["input count", "local countPlural = count: plural"],
            |   "selectors": ["countPlural"],
            |   "match": {
            |       "countPlural=one": "$singular",
            |       "countPlural=other": "$plural",
            |   },
            |}]
            """.trimMargin("|")
    }
}

data class Translations(val translations: MutableMap<String, Translation>) {

    fun get(locale: String): Translation = this.translations.getOrPut(locale) { Translation.default() }

    fun hasPlural(): Boolean = translations.values.any { it.pluralEnabled }

    fun singular(locale: String, singular: String) {
        this.get(locale).singular = singular
    }

    fun plural(locale: String, plural: String) {
        this.get(locale).plural = plural
    }

    fun enablePlural(locale: String, enablePlural: Boolean) {
        this.get(locale).pluralEnabled = enablePlural
    }
}
