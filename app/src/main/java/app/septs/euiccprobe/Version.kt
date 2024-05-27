package app.septs.euiccprobe

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
        val properties = SystemProperties.getAll()
        return properties["ro.mi.os.version.code"]?.let { "HyperOS $it" }
            ?: properties["ro.miui.ui.version.name"]?.let { "MIUI $it" }
            ?: properties["ro.build.version.oneui"]?.let { "OneUI $it" }
    }
}