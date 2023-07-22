internal sealed class PropertyValue(val value: String) {

    class Configured(value: String) : PropertyValue(value)

    class Overridden(value: String, val overridingValue: String) : PropertyValue(value)

}