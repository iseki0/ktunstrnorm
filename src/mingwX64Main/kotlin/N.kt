package space.iseki.ktunstrnorm

import kotlinx.cinterop.CFunction
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKStringFromUtf16
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.posix.wchar_tVar
import platform.windows.ERROR_SUCCESS
import platform.windows.FORMAT_MESSAGE_ALLOCATE_BUFFER
import platform.windows.FORMAT_MESSAGE_FROM_SYSTEM
import platform.windows.FORMAT_MESSAGE_IGNORE_INSERTS
import platform.windows.FormatMessageW
import platform.windows.GetLastError
import platform.windows.GetProcAddress
import platform.windows.LANG_NEUTRAL
import platform.windows.LPWSTRVar
import platform.windows.LoadLibraryW
import platform.windows.LocalFree
import platform.windows.SUBLANG_DEFAULT

@OptIn(ExperimentalForeignApi::class)
private val libNormalize = checkNotNull(LoadLibraryW("Normaliz.dll")) { "LoadLibraryW(\"Normaliz.dll\") == null" }

@OptIn(ExperimentalForeignApi::class)
private val addr: NormalizeStringFn = checkNotNull(GetProcAddress(libNormalize, "NormalizeString")?.reinterpret()) {
    "GetProcAddress(_, \"NormalizeString\") == null"
}


@OptIn(ExperimentalForeignApi::class)
internal actual fun doConvert(input: String, form: NormalizationForm): String {
    if (input.isEmpty()) return ""
    memScoped {
        val inputArr = UShortArray(input.length) { input[it].code.toUShort() }
        val winForm = when (form) {
            NormalizationForm.NFC -> 1u
            NormalizationForm.NFD -> 2u
            NormalizationForm.NFKC -> 5u
            NormalizationForm.NFKD -> 6u
        }

        val n = inputArr.usePinned { pinned ->
            addr(
                p1 = winForm,
                p2 = pinned.addressOf(0),
                p3 = inputArr.size,
                p4 = null,
                p5 = 0,
            ).also { if (it == 0) handleErrorImmediately() }
        }
        if (n == 0) return ""
        val resultArr = UShortArray(n)
        val rn = inputArr.usePinned { p1 ->
            resultArr.usePinned { p2 ->
                addr(
                    p1 = winForm,
                    p2 = p1.addressOf(0),
                    p3 = inputArr.size,
                    p4 = p2.addressOf(0),
                    p5 = resultArr.size,
                ).also { if (it == 0) handleErrorImmediately() }
            }
        }
        check(rn != 0)
        val builder = StringBuilder(rn)
        for (i in 0 until rn) {
            builder.append(resultArr[i].toInt().toChar())
        }
        return builder.toString()
    }
}

private fun makeLangId(primary: Int, sub: Int) = (sub shl 10) or primary

@OptIn(ExperimentalForeignApi::class)
internal fun handleErrorImmediately() {
    val errorCode = GetLastError()
    if (errorCode == ERROR_SUCCESS.toUInt()) {
        return
    }
    var message = ""
    memScoped {
        val buffer = alloc<LPWSTRVar>()
        val size = FormatMessageW(
            dwFlags = (FORMAT_MESSAGE_ALLOCATE_BUFFER or FORMAT_MESSAGE_FROM_SYSTEM or FORMAT_MESSAGE_IGNORE_INSERTS).toUInt(),
            lpSource = null,
            dwMessageId = errorCode,
            dwLanguageId = makeLangId(LANG_NEUTRAL, SUBLANG_DEFAULT).toUInt(),
            lpBuffer = buffer.ptr.reinterpret(),
            nSize = 0u,
            Arguments = null,
        )
        if (size > 0u) {
            val m = buffer.value?.toKStringFromUtf16().orEmpty()
            LocalFree(buffer.value)
            message = m.trim()
        }
    }
    throw IllegalStateException("Error code: $errorCode, message: $message")
}

@OptIn(ExperimentalForeignApi::class)
typealias NormalizeStringFn = CPointer<CFunction<(UInt, CPointer<wchar_tVar>?, Int, CPointer<wchar_tVar>?, Int) -> Int>>