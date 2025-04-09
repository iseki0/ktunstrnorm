package space.iseki.ktunstrnorm

enum class NormalizationForm {
    NFD, NFKD, NFC, NFKC,
}

internal expect fun doConvert(input: String, form: NormalizationForm): String

fun String.normalize(form: NormalizationForm): String = if (isEmpty()) "" else doConvert(this, form)

class UnicodeNormalizationImplNotReadyError : Error {
    constructor(message: String? = null, cause: Throwable? = null) : super(message, cause)
}
