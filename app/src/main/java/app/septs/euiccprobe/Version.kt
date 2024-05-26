package app.septs.euiccprobe

object Version {
    fun getVersion(): String? {
        val miVersion = SystemProperties.get("ro.mi.os.version.code")
        val miuiVersion = SystemProperties.get("ro.miui.ui.version.name")
        val oneuiVersion = SystemProperties.get("ro.build.version.oneui")
        return when {
            miVersion != null -> "HyperOS $miVersion"
            miuiVersion != null -> "MIUI $miuiVersion"
            oneuiVersion != null -> "OneUI $oneuiVersion"
            else -> null
        }
    }
}