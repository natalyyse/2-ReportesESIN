# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# ----nuevas reglas----
# --- Reglas para Firebase y modelos de datos ---
# Conserva los nombres de los campos de tus clases de modelo que usas con Firestore.
# Si no lo haces, Firestore no podrá leer/escribir los datos correctamente.
-keep class com.example.reportes.modelo.** { *; }

# --- Reglas para Glide ---
# Necesarias para que Glide encuentre sus módulos generados.
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$ImageType {
  **[] $VALUES;
  public *;
}

# --- Reglas para Apache POI ---
# Esta librería usa reflexión intensivamente y se romperá sin estas reglas.
# Estas son reglas generales; pueden ser más específicas si conoces las clases exactas que usas.
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**

# --- Reglas para clases de Java Desktop (AWT, etc.) ---
# Librerías como POI o MPAndroidChart pueden referenciar clases que no existen en Android.
# Con -dontwarn le decimos a R8 que ignore estas referencias faltantes.
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn java.beans.**