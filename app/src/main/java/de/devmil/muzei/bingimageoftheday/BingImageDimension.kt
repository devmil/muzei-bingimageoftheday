package de.devmil.muzei.bingimageoftheday

/**
 * Created by devmil on 16.02.14.

 * Represents the available Bing Image dimensions and all logic around them (e.g. orientation mapping)
 */
enum class BingImageDimension {
    WVGA(1, 480, 800),
    WXGA(2, 768, 1280),
    HD(3, 1080, 1920),
    UHD(4, "1080x1920", "UHD");

    private val code : Int;
    private val stringRepPortrait: String;
    private val stringRepLandscape: String;

    constructor(code: Int, shortDimension: Int, longDimension: Int) {
        this.code = code;
        stringRepPortrait = String.format("%dx%d", shortDimension, longDimension);
        stringRepLandscape = String.format("%dx%d", longDimension, shortDimension);
    }

    constructor(code: Int, stringRepPortrait: String, stringRepLandscape : String) {
        this.code = code;
        this.stringRepPortrait = stringRepPortrait;
        this.stringRepLandscape = stringRepLandscape;
    }

    fun getStringRepresentation(portrait: Boolean): String {
        return if (portrait) stringRepPortrait else stringRepLandscape
    }

    companion object {

        fun fromCode(code: Int): BingImageDimension {
            if (WVGA.code == code)
                return WVGA
            if (WXGA.code == code)
                return WXGA
            if (HD.code == code)
                return HD
            return UHD
        }
    }
}
