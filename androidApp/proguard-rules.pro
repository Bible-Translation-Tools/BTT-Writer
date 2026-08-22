# ===== Kotlin =====
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# ===== Kotlinx Serialization =====
-dontnote kotlinx.serialization.AnnotationsKt
-keep @kotlinx.serialization.Serializable class * {
    static kotlinx.serialization.KSerializer serializer(...);
    kotlinx.serialization.descriptors.SerialDescriptor getDescriptor();
}
-keep class **$$serializer { *; }

# ===== JGit + Apache MINA SSHD =====
-keep class org.eclipse.jgit.** { *; }
-dontwarn org.eclipse.jgit.**
-keep class org.apache.sshd.** { *; }
-dontwarn org.apache.sshd.**
-keep class net.i2p.crypto.** { *; }
-dontwarn net.i2p.crypto.**

# ===== BouncyCastle =====
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# ===== iTextG (PDF) =====
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# ===== JMX (javax.management) =====
-dontwarn com.sun.jmx.mbeanserver.GetPropertyAction
