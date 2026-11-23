// Versiones estables y mutuamente compatibles:
//   Android Gradle Plugin 8.7.3  ->  requiere Gradle 8.9+ (el wrapper usa 8.9) y JDK 17+
//   Kotlin 2.0.21                ->  compatible con AGP 8.7.x
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
}
