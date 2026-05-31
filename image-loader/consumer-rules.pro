# Consumer ProGuard rules for image-loader library.
# These rules are applied to any app that includes this library.

# Keep the public entry point so R8 cannot rename or strip it.
-keep public class com.zipoapps.imageloader.ImageLoader { *; }

# Keep the public configuration class and its companion constants.
-keep public class com.zipoapps.imageloader.ImageLoaderConfig { *; }
-keepclassmembers class com.zipoapps.imageloader.ImageLoaderConfig$Companion { *; }

# Keep the fluent builder — consumers chain calls on it; renaming breaks Java callers.
-keep public class com.zipoapps.imageloader.RequestBuilder { *; }

# Keep the Target interface and the bundled ImageView implementation so that library
# consumers can implement their own Target without obfuscation breaking the contract.
-keep public interface com.zipoapps.imageloader.Target { *; }
-keep public class com.zipoapps.imageloader.ImageViewTarget { *; }

# Keep resource IDs used as View tags for request lifecycle tracking.
-keepclassmembers class com.zipoapps.imageloader.R$id { *; }
