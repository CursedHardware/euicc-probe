package app.septs.euiccprobe.tool

object SystemProperties {
    operator fun get(name: String) = Runtime.getRuntime()
        .exec(arrayOf("getprop", name))
        .inputStream
        .reader().readText().trim()

    fun pick(vararg names: String) = names
        .map { Pair(it, this[it]) }
        .filter { it.second.isNotEmpty() }
        .let { mapOf(*it.toTypedArray()) }

    fun boolean(name: String, predicate: (value: String) -> Boolean) = this[name]
        .let { if (it.isEmpty()) false else predicate(it) }

    fun isEnabled(name: String) = boolean(name) {
        val value = it.lowercase()
        value == "1" || value == "y" || value == "true" || value == "yes" || value == "on"
    }
}