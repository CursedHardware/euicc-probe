package app.septs.euiccprobe

object SystemProperties {
    private val properties by lazy {
        val properties = mutableMapOf<String, String>()
        val p = Runtime.getRuntime().exec("getprop")
        for (line in p.inputStream.reader().readLines()) {
            val name = line.indexOf('[')
                .let { (it + 1)..<line.indexOf(']', it) }
            val value = line.indexOf('[', name.last)
                .let { (it + 1)..<line.indexOf(']', it) }
            if (value.isEmpty()) continue
            properties[line.slice(name)] = line.slice(value)
        }
        return@lazy properties
    }

    operator fun get(name: String): String {
        return properties[name].orEmpty()
    }

    fun pick(vararg names: String) = buildMap {
        for (name in names) {
            if (properties[name] == null) continue
            put(name, properties[name])
        }
    }

    fun boolean(name: String, matcher: (value: String) -> Boolean): Boolean {
        return matcher(properties[name] ?: return false)
    }

    fun isEnabled(name: String) = boolean(name) {
        val value = it.lowercase()
        value == "1" || value == "y" || value == "true" || value == "yes" || value == "on"
    }
}