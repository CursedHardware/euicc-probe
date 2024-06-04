package app.septs.euiccprobe

object SystemProperties {
    operator fun get(name: String): String {
        val p = Runtime.getRuntime().exec(arrayOf("getprop", name))
        return p.inputStream.reader().readText().trim()
    }

    fun getAll() = buildMap {
        val p = Runtime.getRuntime().exec("getprop")
        for (line in p.inputStream.reader().readLines()) {
            val name = line.indexOf('[')
                .let { (it + 1)..<line.indexOf(']', it) }
            val value = line.indexOf('[', name.last)
                .let { (it + 1)..<line.indexOf(']', it) }
            if (value.isEmpty()) continue
            put(line.slice(name), line.slice(value))
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