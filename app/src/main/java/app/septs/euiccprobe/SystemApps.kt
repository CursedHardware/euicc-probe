package app.septs.euiccprobe

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

object SystemApps {
    private val perms = arrayOf(
        "android.permission.BIND_EUICC_SERVICE",
        "android.permission.SECURE_ELEMENT_PRIVILEGED_OPERATION",
        "android.permission.WRITE_EMBEDDED_SUBSCRIPTIONS",
        "com.android.permission.WRITE_EMBEDDED_SUBSCRIPTIONS",
    )

    fun getSystemLPAs(context: Context): List<ApplicationInfo> {
        val namePattern = Regex("lpa|euicc|esim")
        return getSystemApps(context.packageManager).filter { app ->
            when {
                app.packageName.startsWith("com.android") -> false
                app.packageName.contains(namePattern) -> perms.any {
                    hasPermission(context.packageManager, it, app.packageName)
                }

                else -> false
            }
        }
    }

    private fun getSystemApps(pm: PackageManager): List<ApplicationInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return emptyList()
        }
        val flags = PackageManager.MATCH_SYSTEM_ONLY
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flags.toLong()))
        } else {
            pm.getInstalledApplications(flags)
        }
    }

    private fun hasPermission(pm: PackageManager, permName: String, pkgName: String): Boolean {
        return pm.checkPermission(permName, pkgName) == PackageManager.PERMISSION_GRANTED
    }
}