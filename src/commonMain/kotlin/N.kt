package space.iseki.ktunstrnorm

enum class NormalizationForm {
    NFD, NFKD, NFC, NFKC,
}

internal expect fun doConvert(input: String, form: NormalizationForm): String

