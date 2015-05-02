package de.devmil.muzei.bingimageofthedayartsource;

/**
 * Created by devmil on 16.02.14.
 *
 * Represents the available Bing Image dimensions and all logic around them (e.g. orientation mapping)
 */
public enum BingImageDimension {
    WVGA(1, 480, 800),
    WXGA(2, 768, 1280),
    HD(3, 1080, 1920);

    private int _ShortDimension;
    private int _LongDimension;
    private int _Code;

    private BingImageDimension(int code, int shortDimension, int longDimension)
    {
        _Code = code;
        _ShortDimension = shortDimension;
        _LongDimension = longDimension;
    }

    public int getCode()
    {
        return _Code;
    }

    public String getStringRepresentation(boolean portrait)
    {
        int width = portrait ? _ShortDimension : _LongDimension;
        int height = portrait ? _LongDimension : _ShortDimension;
        return String.format("%dx%d", width, height);
    }

    public static BingImageDimension fromCode(int code)
    {
        if(WVGA.getCode() == code)
            return WVGA;
        if(WXGA.getCode() == code)
            return WXGA;
        return WXGA;
    }
}
