package app.septs.euiccprobe

object SystemProperties {
    fun get(name: String): String? {
        val p = Runtime.getRuntime().exec(arrayOf("getprop", name))
        return p.inputStream.reader().readText().trim().ifEmpty { null }
    }

    fun getAll(): Map<String, String> {
        val p = Runtime.getRuntime().exec("getprop")
        return buildMap {
            p.inputStream.reader().forEachLine { line ->
                val name = line.indexOf('[')
                    .let { (it + 1)..<line.indexOf(']', it) }
                val value = line.indexOf('[', name.last)
                    .let { (it + 1)..<line.indexOf(']', it) }
                if (value.isEmpty()) return@forEachLine
                put(line.slice(name), line.slice(value))
            }
        }
    }

    fun pick(vararg names: String) = buildMap {
        val properties = getAll()
        for (name in names) {
            if (properties[name] == null) continue
            put(name, properties[name])
        }
    }
}