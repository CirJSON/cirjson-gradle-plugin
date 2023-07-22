import java.io.File
import java.io.StringReader
import java.util.*

internal class LocalPropertiesModifier(private val localProperties: File) {

    private val initialUserConfiguredPropertiesContent = getUserConfiguredPropertiesContent()

    private fun getUserConfiguredPropertiesContent(): String {
        if (!localProperties.exists()) return ""
        var insideAutomaticallyConfiguredSection = false // filter out the automatically configured lines
        return localProperties.readLines().filter { line ->
            if (line == SYNCED_PROPERTIES_START_LINE) {
                insideAutomaticallyConfiguredSection = true
            }
            val shouldIncludeThisLine = !insideAutomaticallyConfiguredSection
            if (line == SYNCED_PROPERTIES_END_LINE) {
                insideAutomaticallyConfiguredSection = false
            }
            shouldIncludeThisLine
        }.joinToString("\n")
    }

    fun applySetup(setupFile: SetupFile) {
        localProperties.parentFile.apply {
            if (!exists()) {
                mkdirs()
            }
        }
        if (localProperties.exists() && !localProperties.isFile) {
            error("$localProperties is not a file!")
        }
        val content = getUserConfiguredPropertiesContent()
        val manuallyConfiguredProperties = Properties().apply {
            StringReader(content).use {
                load(it)
            }
        }
        val propertiesToSetup = setupFile.properties.mapValues {
            val overridingValue = manuallyConfiguredProperties[it.key]
            if (overridingValue != null) {
                PropertyValue.Overridden(it.value, overridingValue.toString())
            } else {
                PropertyValue.Configured(it.value)
            }
        }
        localProperties.writeText("""
            |${content.addSuffix("\n")}
            |$SYNCED_PROPERTIES_START_LINES
            |${propertiesToSetup.asPropertiesLines}
            |$SYNCED_PROPERTIES_END_LINE
            |
            """.trimMargin())
    }

    fun initiallyContains(line: String) = initialUserConfiguredPropertiesContent.contains(line)

    fun putLine(line: String) = localProperties.appendText("\n$line")

}