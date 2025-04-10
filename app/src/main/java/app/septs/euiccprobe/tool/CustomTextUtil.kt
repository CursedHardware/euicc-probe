package app.septs.euiccprobe.tool

object CustomTextUtil {
    /**
     * Extension function: Returns the original string if it is not null or blank; otherwise, returns the default value.
     *
     * @param defaultValue The value to return if the string is null or blank
     * @return A non-null string
     */
    fun String?.orDefault(defaultValue: String = ""): String {
        return this?.takeIf { it.isNotBlank() } ?: defaultValue
    }
}