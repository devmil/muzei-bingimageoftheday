package de.devmil.muzei.bingimageofthedayartsource

/**
 * Created by devmil on 16.02.14.

 * Represents the available Bing Image dimensions and all logic around them (e.g. orientation mapping)
 */
enum class BingImageDimension constructor(val code: Int, private val _ShortDimension: Int, private val _LongDimension: Int) {
    WVGA(1, 480, 800),
    WXGA(2, 768, 1280),
    HD(3, 1080, 1920);

    fun getStringRepresentation(portrait: Boolean): String {
        val width = if (portrait) _ShortDimension else _LongDimension
        val height = if (portrait) _LongDimension else _ShortDimension
        return String.format("%dx%d", width, height)
    }

    companion object {

        fun fromCode(code: Int): BingImageDimension {
            if (WVGA.code == code)
                return WVGA
            if (WXGA.code == code)
                return WXGA
            return WXGA
        }
    }
}
