@file:OptIn(ExperimentalForeignApi::class)

package space.iseki.ktunstrnorm

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ByteVarOf
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaque
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValues
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.UShortVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKStringFromUtf8
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.posix.RTLD_LAZY
import platform.posix.dlclose
import platform.posix.dlerror
import platform.posix.dlopen
import platform.posix.dlsym

internal const val UNORM2_COMPOSE = 0
internal const val UNORM2_DECOMPOSE = 1
internal const val UNORM2_FCD = 2
internal const val UNORM2_COMPOSE_CONTIGUOUS = 3
internal const val U_BUFFER_OVERFLOW_ERROR = 15
internal const val U_STRING_NOT_TERMINATED_WARNING = -124

// 对应：UErrorCode 是一个 int32_t（通常就是 C int*）
internal typealias UErrorCodeVar = CPointer<IntVar>

// 对应 const UNormalizer2*
internal typealias UNormalizer2 = COpaque

internal typealias UErrorNameFn = CPointer<CFunction<(Int) -> CPointer<ByteVar>?>>


// typedef for: int32_t unorm2_normalize(...)
internal typealias Unorm2NormalizeFn = CPointer<CFunction<(
    norm2: CPointer<UNormalizer2>,
    src: CPointer<UShortVar>,
    length: Int,
    dest: CPointer<UShortVar>?,
    capacity: Int,
    pErrorCode: UErrorCodeVar,
) -> Int>>

// typedef for: const UNormalizer2* unorm2_getInstance(...)
internal typealias Unorm2GetInstanceFn = CPointer<CFunction<(
    packageName: CPointer<ByteVar>?,
    name: CValues<ByteVarOf<Byte>>?,
    mode: Int, // UNormalization2Mode 是 enum，可传 Int
    pErrorCode: UErrorCodeVar,
) -> CPointer<UNormalizer2>?>>

internal val normalizer by lazy { LibLoader.loadLib() }


@OptIn(ExperimentalForeignApi::class)
internal object LibLoader {

    data class Symbols(
        val getInstance: Unorm2GetInstanceFn,
        val normalize: Unorm2NormalizeFn,
        val uerrorName: UErrorNameFn,
    )

    data class Normalizers(
        val normalizeFn: Unorm2NormalizeFn,
        val uerrorNameFn: UErrorNameFn,
        val nfc: CPointer<UNormalizer2>,
        val nfd: CPointer<UNormalizer2>,
        val nfkc: CPointer<UNormalizer2>,
        val nfkd: CPointer<UNormalizer2>,
    )

    fun loadLib(): Normalizers {
        val libNameSeq = sequence {
            // https://developer.android.com/guide/topics/resources/internationalization?hl=zh-cn#icu4c
            yield("libicu.so" to null)
            // Linux
            yield("libicuuc.so" to null)
            (79 downTo 60).forEach { yield("libicuuc.so.$it" to it) }
        }
        for ((name, ver) in libNameSeq) {
            val lib = dlopenOrNull(name) ?: continue
            val symbols = loadSymbols(lib, ver)
            if (symbols == null) {
                dlclose(lib)
                continue
            }
            fun get(name: String, mode: Int): CPointer<UNormalizer2>? {
                memScoped {
                    val errCode = alloc<IntVar>()
                    val normalizer = symbols.getInstance.invoke(null, name.cstr, mode, errCode.ptr)
                    if (errCode.value != 0) {
                        return null
                    }
                    return normalizer
                }
            }

            val nfc = get("nfc", UNORM2_COMPOSE)
            val nfd = get("nfc", UNORM2_DECOMPOSE)
            val nfkc = get("nfkc", UNORM2_COMPOSE)
            val nfkd = get("nfkc", UNORM2_DECOMPOSE)
            if (nfc == null || nfd == null || nfkc == null || nfkd == null) {
                dlclose(lib)
                continue
            }
            return Normalizers(
                normalizeFn = symbols.normalize,
                uerrorNameFn = symbols.uerrorName,
                nfc = nfc,
                nfd = nfd,
                nfkc = nfkc,
                nfkd = nfkd,
            )
        }
        error("Failed to load ICU library")
    }

    private fun loadSymbols(lib: COpaquePointer, version: Int?): Symbols? {
        val getInstance = sequence {
            if (version != null) yield("unorm2_getInstance_$version")
            yield("unorm2_getInstance")
        }.firstNotNullOfOrNull { dlsymOrNull(lib, it) }
        val normalize = sequence {
            if (version != null) yield("unorm2_normalize_$version")
            yield("unorm2_normalize")
        }.firstNotNullOfOrNull { dlsymOrNull(lib, it) }
        val uerrorName = sequence {
            if (version != null) yield("u_errorName_$version")
            yield("u_errorName")
        }.firstNotNullOfOrNull { dlsymOrNull(lib, it) }
        return Symbols(
            getInstance = getInstance?.reinterpret() ?: return null,
            normalize = normalize?.reinterpret() ?: return null,
            uerrorName = uerrorName?.reinterpret() ?: return null,
        )
    }

    private fun dlopenOrNull(name: String): COpaquePointer? {
        return dlopen(name, RTLD_LAZY)
    }

    private fun dlsymOrNull(lib: COpaquePointer, name: String): COpaquePointer? {
        return dlsym(lib, name)
    }
}

internal fun errorMessage(e: Int): String? {
    return normalizer.uerrorNameFn.invoke(e)?.toKStringFromUtf8()
}

internal actual fun doConvert(input: String, form: NormalizationForm): String {
    if (input.isEmpty()) return ""

    val normalizers = normalizer
    val normalizeFn = normalizers.normalizeFn

    // 1. 选择对应的 normalizer
    val norm: CPointer<UNormalizer2> = when (form) {
        NormalizationForm.NFC -> normalizers.nfc
        NormalizationForm.NFD -> normalizers.nfd
        NormalizationForm.NFKC -> normalizers.nfkc
        NormalizationForm.NFKD -> normalizers.nfkd
    }

    // 2. 将 input 转为 UTF-16 编码
    val inBuf = input.encodeToShortArrayInUtf16()
    val srcLength = inBuf.size

    memScoped {
        val error = alloc<IntVar>()
        error.value = 0

        val requiredLength = inBuf.usePinned { utf16 ->
            // 3. 第一遍调用，获得所需 buffer 大小
            normalizeFn(
                p1 = norm,
                p2 = utf16.addressOf(0).reinterpret(),
                p3 = srcLength,
                p4 = null,
                p5 = 0,
                p6 = error.ptr,
            )

        }

        if (error.value != U_BUFFER_OVERFLOW_ERROR && error.value != 0) {
            error(
                "ICU normalize failed (stage 1): ${error.value} - ${errorMessage(error.value)}"
            )
        }

        error.value = 0

        // 4. 分配目标缓冲区，第二次调用
        val outBuf = ShortArray(requiredLength)
        val actualLen = inBuf.usePinned { inBuf ->
            outBuf.usePinned { outBuf ->
                normalizeFn(
                    p1 = norm,
                    p2 = inBuf.addressOf(0).reinterpret(),
                    p3 = srcLength,
                    p4 = outBuf.addressOf(0).reinterpret(),
                    p5 = requiredLength,
                    p6 = error.ptr,
                )
            }
        }

        if (error.value != 0 && error.value != U_STRING_NOT_TERMINATED_WARNING) {
            error("ICU normalize failed (stage 2): ${error.value} - ${errorMessage(error.value)}")
        }

        // 5. 把 UTF-16 转回 Kotlin 字符串
        return outBuf.decodeToStringInUtf16(actualLen)
    }
}

internal fun String.encodeToShortArrayInUtf16() = ShortArray(this.length) { this[it].code.toShort() }

internal fun ShortArray.decodeToStringInUtf16(n: Int) = CharArray(n) { this[it].toInt().toChar() }.concatToString()
