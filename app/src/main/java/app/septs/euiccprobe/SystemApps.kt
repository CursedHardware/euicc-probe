package app.septs.euiccprobe

import java.io.File

object SystemApps {
    private val perms = setOf(
        "android.permission.MODIFY_PHONE_STATE",
        "android.permission.READ_PRIVILEGED_PHONE_STATE",
        "android.permission.WRITE_EMBEDDED_SUBSCRIPTIONS",
        "com.android.permission.WRITE_EMBEDDED_SUBSCRIPTIONS",
    )

    fun getSystemLPAs(): List<PrivAppPermissionParser.Companion.PrivAppPermission> {
        val directories = listOf("/", "/system", "/vendor", "/product")
        val parser = PrivAppPermissionParser()
        for (directory in directories) {
            val permissions = File(directory, "etc/permissions/")
            if (!permissions.exists()) continue
            val files = permissions.listFiles() ?: continue
            for (file in files) {
                if (!file.canRead()) continue
                if (!file.name.startsWith("privapp-permissions")) continue
                if (file.extension != "xml") continue
                if (!file.name.contains("permission")) continue
                file.inputStream().use(parser::parse)
            }
        }
        return parser.filter { perm -> perm.allowedPermissions.containsAll(perms) }
    }
}