package space.iseki.ktunstrnorm

internal actual fun doConvert(input: String, form: NormalizationForm): String {
    val formArg = when (form) {
        NormalizationForm.NFC -> "NFC"
        NormalizationForm.NFD -> "NFD"
        NormalizationForm.NFKC -> "NFKC"
        NormalizationForm.NFKD -> "NFKD"
    }
    return doConvert0(input, formArg)
}

@Suppress("UNUSED_PARAMETER")
private fun doConvert0(input: String, from: String): String = js("input.normalize(from)")
