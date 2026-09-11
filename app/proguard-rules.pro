# Settings are stored as JSON, so the serializer machinery must survive R8.
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations
-dontnote kotlinx.serialization.**
-keepclassmembers class nl.fietsweer.app.data.** {
    *** Companion;
}
-keepclasseswithmembers class nl.fietsweer.app.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class nl.fietsweer.app.data.**$$serializer { *; }

# Entry points Android instantiates by name.
-keep class nl.fietsweer.app.FietsweerApplication
-keep class nl.fietsweer.app.MainActivity
-keep class nl.fietsweer.app.notify.** { *; }
-keep class nl.fietsweer.app.widget.** { *; }
