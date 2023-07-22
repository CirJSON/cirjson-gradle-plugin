import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

internal val isInIdeaSync
    get() = System.getProperty("idea.sync.active").toBoolean()

internal const val linkPlaceholder = "%LINK%"

internal const val USER_CONSENT_MARKER =
        "# This line indicates that you have chosen to enable automatic configuration of properties."

internal const val USER_CONSENT_MARKER_WITH_DETAILS_LINK = "$USER_CONSENT_MARKER Details: $linkPlaceholder"

internal const val USER_REFUSAL_MARKER =
        "# This line indicates that you have chosen to disable automatic configuration of properties. If you want to enable it, remove this line."

internal val USER_CONSENT_REQUEST = """

    ! ATTENTION REQUIRED !
    Most probably you're a developer from the Kotlin team. We are asking for your consent for automatic configuration of local.properties file
    for providing some optimizations and collecting additional debug information.

""".trimIndent()

internal val USER_CONSENT_DETAILS_LINK_TEMPLATE = "You can read more details here: $linkPlaceholder"

internal const val PROMPT_REQUEST = "Do you agree with this? Please answer with 'yes' or 'no': "

internal const val SYNCED_PROPERTIES_START_LINE = "# Automatically configured by the `internal-gradle-setup` plugin"

internal val SYNCED_PROPERTIES_START_LINES = """
    $SYNCED_PROPERTIES_START_LINE
    # Please do not edit these properties manually, the changes will be lost
    # If you want to override some values, put them before this section and remove from this section
""".trimIndent()

internal const val SYNCED_PROPERTIES_END_LINE = "# the end of automatically configured properties"

@OptIn(ExperimentalSerializationApi::class)
internal val json = Json {
    ignoreUnknownKeys = true
    namingStrategy = JsonNamingStrategy.SnakeCase
}

internal val Map<String, PropertyValue>.asPropertiesLines: String
    get() = map { (key, valueWrapper) ->
        when (valueWrapper) {
            is PropertyValue.Overridden -> "#$key=${valueWrapper.value} the property is overridden by '${valueWrapper.overridingValue}'"
            is PropertyValue.Configured -> "$key=${valueWrapper.value}"
        }
    }.joinToString("\n")