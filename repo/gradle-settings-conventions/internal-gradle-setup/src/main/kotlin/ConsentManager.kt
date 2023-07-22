import java.io.BufferedReader
import java.io.PrintStream

internal class ConsentManager(private val modifier: LocalPropertiesModifier,
        private val input: BufferedReader = System.`in`.bufferedReader(),
        private val output: PrintStream = System.out) {

    fun getUserDecision() = when {
        modifier.initiallyContains(USER_REFUSAL_MARKER) -> false
        modifier.initiallyContains(USER_CONSENT_MARKER) -> true
        isInIdeaSync -> false
        else -> null
    }

    fun askForConsent(consentDetailsLink: String? = null): Boolean {
        output.println(USER_CONSENT_REQUEST)
        if (consentDetailsLink != null) {
            output.println(USER_CONSENT_DETAILS_LINK_TEMPLATE.formatWithLink(consentDetailsLink))
        }
        while (true) {
            output.println(PROMPT_REQUEST)
            when (input.readLine()) {
                "yes" -> {
                    output.println("You've given the consent")
                    modifier.putLine(if (consentDetailsLink != null) {
                        USER_CONSENT_MARKER_WITH_DETAILS_LINK.formatWithLink(consentDetailsLink)
                    } else {
                        USER_CONSENT_MARKER
                    })
                    return true
                }
                "no" -> {
                    output.println("You've refused to give the consent")
                    modifier.putLine(USER_REFUSAL_MARKER)
                    return false
                }
            }
        }
    }
}