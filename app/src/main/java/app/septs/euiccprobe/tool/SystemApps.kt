package app.septs.euiccprobe.tool

import android.content.pm.PackageManager

object SystemApps {
    private val requiredPermissions = setOf(
        "android.permission.MODIFY_PHONE_STATE",
        "android.permission.READ_PRIVILEGED_PHONE_STATE",
        "android.permission.WRITE_EMBEDDED_SUBSCRIPTIONS",
    )

    private val optionalPermissions = setOf(
        "android.permission.BIND_EUICC_SERVICE",
        "android.permission.SECURE_ELEMENT_PRIVILEGED_OPERATION",
        "com.android.permission.WRITE_EMBEDDED_SUBSCRIPTIONS",
    )

    fun getSystemLPAs(): List<PrivAppPermissionParser.Companion.PrivAppPermission> {
        val permissions = PrivAppPermissionParser.loadPermissions()
        return permissions.filter { perm ->
            perm.allowedPermissions.containsAll(requiredPermissions) &&
                    perm.allowedPermissions.any(optionalPermissions::contains)
        }
    }

    fun getApplicationLabel(pm: PackageManager, packageName: String): String? {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_ACTIVITIES)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            null
        }
    }
}