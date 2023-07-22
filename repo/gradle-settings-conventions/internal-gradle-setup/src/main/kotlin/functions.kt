import kotlinx.serialization.decodeFromString
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.streams.asSequence

internal fun String.formatWithLink(link: String) = replace(linkPlaceholder,
        link) // can't use decodeFromStream: https://github.com/Kotlin/kotlinx.serialization/issues/2218

internal fun parseSetupFile(inputStream: InputStream): SetupFile =
        json.decodeFromString(BufferedReader(InputStreamReader(inputStream)).lines().asSequence().joinToString("\n"))

internal fun String.addSuffix(suffix: String): String {
    if (this.endsWith(suffix)) return this
    return "$this$suffix"
}
