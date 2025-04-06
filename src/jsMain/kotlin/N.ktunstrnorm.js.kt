package space.iseki.ktunstrnorm

@Suppress("UNUSED_VARIABLE")
internal actual fun doConvert(input: String, form: NormalizationForm): String {
    val formArg = when (form) {
        NormalizationForm.NFC -> "NFC"
        NormalizationForm.NFD -> "NFD"
        NormalizationForm.NFKC -> "NFKC"
        NormalizationForm.NFKD -> "NFKD"
    }
    return js("input.normalize(formArg)").unsafeCast<String>()
}
