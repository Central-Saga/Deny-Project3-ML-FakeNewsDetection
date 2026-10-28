# ProGuard / R8 Rules for FakeNews AI

# Preserve line numbers and source file attributes for crash diagnostics
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# ===========================================================================
# ONNX Runtime Android & JNI Rules
# ===========================================================================
-keep class ai.onnxruntime.** { *; }
-keepclassmembers class ai.onnxruntime.** {
    native <methods>;
    *;
}
-dontwarn ai.onnxruntime.**

# Preserve all native methods across the application
-keepclasseswithmembernames class * {
    native <methods>;
}

# ===========================================================================
# ML Pipeline & Classifier Classes
# ===========================================================================
-keep class com.example.fakenewsdetection.FakeNewsClassifier** { *; }
-keep class com.example.fakenewsdetection.OnnxSvmScorer** { *; }
-keep class com.example.fakenewsdetection.TextPreprocessor** { *; }
-keep class com.example.fakenewsdetection.LocalHistoryManager** { *; }