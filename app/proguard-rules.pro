# kotlinx.serialization (backup file format)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class io.github.ahmadnayfeh.silah.** {
    *** Companion;
}
-keepclasseswithmembers class io.github.ahmadnayfeh.silah.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class io.github.ahmadnayfeh.silah.**$$serializer { *; }
