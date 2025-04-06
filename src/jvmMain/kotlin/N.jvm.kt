package space.iseki.ktunstrnorm

import java.text.Normalizer


internal actual fun doConvert(input: String, form: NormalizationForm): String {
    val jvmForm = when (form) {
        NormalizationForm.NFKC -> Normalizer.Form.NFKC
        NormalizationForm.NFKD -> Normalizer.Form.NFKD
        NormalizationForm.NFC -> Normalizer.Form.NFC
        NormalizationForm.NFD -> Normalizer.Form.NFD
    }
    return Normalizer.normalize(input, jvmForm)
}
