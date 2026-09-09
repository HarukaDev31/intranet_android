# Reglas por defecto de Android; agregar reglas específicas del proyecto aquí si son necesarias.
-keepattributes *Annotation*
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class com.probusiness.intranet.data.remote.dto.** { *; }
