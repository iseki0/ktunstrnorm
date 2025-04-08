# ktunstrnorm

Unicode normalization function in Kotlin/Multiplatform, use platform APIs.

## Supported targets

- Kotlin/JVM
- Kotlin/JavaScript
- Kotlin/WasmJs
- Kotlin/Native
  - macosX64 —Tier 1
  - macosArm64
  - iosSimulatorArm64
  - iosX64
  - iosArm64
  - linuxX64 —Tier 2
  - linuxArm64
  - watchosArm32
  - watchosArm64
  - watchosX64
  - watchosSimulatorArm64
  - tvosSimulatorArm64
  - tvosX64
  - tvosArm64
  - androidNativeArm32 —Tier 3
  - androidNativeArm64
  - androidNativeX64
  - androidNativeX86
  - mingwX64
  - watchosDeviceArm64

## Samples

```kotlin
fun main() {
    val input = "\u0065\u0301"     // e + ́ = U+0065 U+0301
    val expected = "\u00E9"        // é = U+00E9
    val result = input.normalize(NormalizationForm.NFC)
    println(result)
    check(expected == result)
}
```
