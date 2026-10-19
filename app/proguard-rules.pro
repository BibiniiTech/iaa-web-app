# Preserve data classes used with GSON and Firebase
-keepclassmembers class com.bibiniitech.iaaapp.UserProfile { *; }
-keepclassmembers class com.bibiniitech.iaaapp.Seminar { *; }
-keepclassmembers class com.bibiniitech.iaaapp.ContactConfig { *; }
-keepclassmembers class com.bibiniitech.iaaapp.WelcomeConfig { *; }
-keepclassmembers class com.bibiniitech.iaaapp.NoticeConfig { *; }
-keepclassmembers class com.bibiniitech.iaaapp.DbDocument { *; }
-keepclassmembers class com.bibiniitech.iaaapp.CloudConfig { *; }
-keepclassmembers class com.bibiniitech.iaaapp.SubmissionRecord { *; }
-keepclassmembers class com.bibiniitech.iaaapp.BrevoEmailRequest { *; }
-keepclassmembers class com.bibiniitech.iaaapp.BrevoPerson { *; }
-keepclassmembers class com.bibiniitech.iaaapp.BrevoAttachment { *; }
-keepclassmembers class com.bibiniitech.iaaapp.ui.screens.SubmissionDeadline { *; }
-keepclassmembers class com.bibiniitech.iaaapp.ui.screens.AuditFinding { *; }
-keepclassmembers class com.bibiniitech.iaaapp.ui.screens.PFMSection { *; }

# General GSON rules
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.stream.** { *; }

# Security Hardening: Obfuscate and prevent tampering with SecurityUtils
-keep class com.bibiniitech.iaaapp.SecurityUtils {
    public *;
}
# We don't use -keep for methods to allow R8 to rename them,
# but we want to ensure the class exists if we reflectively check it (rarely).
# Actually, standard obfuscation is better. I'll just leave it to default R8 behavior
# which is already strong if isMinifyEnabled is true.

# Prevent DexGuard-like tools or simple static analysis from finding these strings easily
-repackageclasses ''
-allowaccessmodification

# JavaMail (javax.mail) rules for R8/ProGuard
-keep class javax.mail.** { *; }
-keep class com.sun.mail.** { *; }
-keep class activation.** { *; }
-dontwarn javax.mail.**
-dontwarn com.sun.mail.**
-dontwarn activation.**



