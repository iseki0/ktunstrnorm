package space.iseki.ktunstrnorm

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFIndexVar
import platform.CoreFoundation.CFRangeMake
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateMutableCopy
import platform.CoreFoundation.CFStringCreateWithBytesNoCopy
import platform.CoreFoundation.CFStringGetBytes
import platform.CoreFoundation.CFStringGetLength
import platform.CoreFoundation.CFStringNormalize
import platform.CoreFoundation.kCFAllocatorNull


private const val CFStringEncodingUTF8 = 0x08000100u
private const val CFStringNormalizationFormD = 0L
private const val CFStringNormalizationFormKD = 1L
private const val CFStringNormalizationFormC = 2L
private const val CFStringNormalizationFormKC = 3L

@OptIn(ExperimentalForeignApi::class)
internal actual fun doConvert(input: String, form: NormalizationForm): String {
    println("Before ALL")
    memScoped {
        println("in MemScope")
        val bytes = input.encodeToByteArray()
        val m = bytes.usePinned { pinned ->
            val s = CFStringCreateWithBytesNoCopy(
                alloc = null,
                bytes = pinned.addressOf(0).reinterpret(),
                numBytes = bytes.size.toLong(),
                encoding = CFStringEncodingUTF8,
                isExternalRepresentation = false,
                contentsDeallocator = kCFAllocatorNull,
            )
            println("CFStringCreateWithBytesNoCopy: $s")
            val m = CFStringCreateMutableCopy(
                alloc = null,
                maxLength = 0,
                theString = s,
            )
            println("CFStringCreateMutableCopy: $m")
            CFRelease(s)
            println("CFRelease(s)")
            m
        }
        try {
            CFStringNormalize(
                theString = m,
                theForm = when(form) {
                    NormalizationForm.NFD -> CFStringNormalizationFormD
                    NormalizationForm.NFKD -> CFStringNormalizationFormKD
                    NormalizationForm.NFC -> CFStringNormalizationFormC
                    NormalizationForm.NFKC -> CFStringNormalizationFormKC
                },
            )
            println("CFStringNormalize")
            val usedBufLen = alloc<CFIndexVar>()
            val range = CFRangeMake(0, CFStringGetLength(m))
            CFStringGetBytes(
                theString = m,
                range = range,
                encoding = CFStringEncodingUTF8,
                lossByte = 0u,
                isExternalRepresentation = false,
                buffer = null,
                maxBufLen = 0,
                usedBufLen = usedBufLen.ptr,
            )
            println("CFStringGetBytes1")
            if (input.isNotEmpty()) check(usedBufLen.value > 0)
            if (usedBufLen.value > Int.MAX_VALUE) {
                throw IllegalArgumentException("result String is too long")
            }
            val convertedBytes = ByteArray(usedBufLen.value.toInt())
            convertedBytes.usePinned { pinned ->
                CFStringGetBytes(
                    theString = m,
                    range = range,
                    encoding = CFStringEncodingUTF8,
                    lossByte = 0u,
                    isExternalRepresentation = false,
                    buffer = pinned.addressOf(0).reinterpret(),
                    maxBufLen = convertedBytes.size.toLong(),
                    usedBufLen = null,
                )
            }
            println("CFStringGetBytes2")
            return convertedBytes.decodeToString()
        } finally {
            CFRelease(m)
        }
    }
}
