package app.septs.euiccprobe.tool

import android.os.Build

object Version {
    fun getModelName(): String {
        val parts = ArrayList<String>()
        if (!Build.MODEL.startsWith(Build.BRAND, ignoreCase = true)) {
            parts.add(Build.BRAND)
        }
        parts.add(Build.MODEL)
        if (!Build.BRAND.contentEquals(Build.MANUFACTURER, ignoreCase = true)) {
            parts.add("(${Build.MANUFACTURER})")
        }
        return parts.joinToString(" ")
    }

    fun getFirmwareVersion(): String? {
        val fields = listOf(
            Pair("ro.mi.os.version.code", "HyperOS"),
            Pair("ro.miui.ui.version.name", "MIUI"),
            Pair("ro.build.version.oneui", "OneUI"),
        )
        for (pair in fields) {
            val version = SystemProperties[pair.first]
            if (version.isNotEmpty()) {
                return "${pair.second} $version"
            }
        }
        return null
    }
}