import kotlinx.serialization.Serializable

@Serializable
internal data class SetupFile(val properties: Map<String, String>, val consentDetailsLink: String? = null)
