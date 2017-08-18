/*
 * Copyright 2014 Devmil Solutions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.devmil.muzei.bingimageoftheday

import java.util.ArrayList
import java.util.Arrays

/**
 * Created by devmil on 16.02.14.

 * This enum represents all the known Bing markets.
 * It contains the market code (used by Bing), a name (for selection in the UI) and
 * a drawable resource containing the flag
 */
enum class BingMarket private constructor(val marketCode: String, private val marketName: String, val logoResourceId: Int) {
    Unknown("", "Random", R.drawable.unknown),
    AR_XA("ar-XA", "Arabic – Arabia", R.drawable.united_arab_emirates),
    BG_BG("bg-BG", "Bulgarian – Bulgaria", R.drawable.bulgaria),
    CS_CZ("cs-CZ", "Czech – Czech Republic", R.drawable.czech_republic),
    DA_DK("da-DK", "Danish – Denmark", R.drawable.denmark),
    DE_AT("de-AT", "German – Austria", R.drawable.austria),
    DE_CH("de-CH", "German – Switzerland", R.drawable.switzerland),
    DE_DE("de-DE", "German – Germany", R.drawable.germany),
    EL_GR("el-GR", "Greek – Greece", R.drawable.greece),
    EN_AU("en-AU", "English – Australia", R.drawable.australia),
    EN_CA("en-CA", "English – Canada", R.drawable.canada),
    EN_GB("en-GB", "English – United Kingdom", R.drawable.united_kingdom),
    EN_ID("en-ID", "English – Indonesia", R.drawable.indonesia),
    EN_IE("en-IE", "English – Ireland", R.drawable.ireland),
    EN_IN("en-IN", "English – India", R.drawable.india),
    EN_MY("en-MY", "English – Malaysia", R.drawable.malaysia),
    EN_NZ("en-NZ", "English – New Zealand", R.drawable.new_zealand),
    EN_PH("en-PH", "English – Philippines", R.drawable.philippines),
    EN_SG("en-SG", "English – Singapore", R.drawable.singapore),
    EN_US("en-US", "English – United States", R.drawable.united_states),
    EN_XA("en-XA", "English – Arabia", R.drawable.united_arab_emirates),
    EN_ZA("en-ZA", "English – South Africa", R.drawable.south_africa),
    ES_AR("es-AR", "Spanish – Argentina", R.drawable.argentina),
    ES_CL("es-CL", "Spanish – Chile", R.drawable.chile),
    ES_ES("es-ES", "Spanish – Spain", R.drawable.spain),
    ES_MX("es-MX", "Spanish – Mexico", R.drawable.mexico),
    ES_US("es-US", "Spanish – United States", R.drawable.united_states),
    ES_XL("es-XL", "Spanish – Latin America", R.drawable.latin_america),
    ET_EE("et-EE", "Estonian – Estonia", R.drawable.estonia),
    FI_FI("fi-FI", "Finnish – Finland", R.drawable.finland),
    FR_BE("fr-BE", "French – Belgium", R.drawable.belgium),
    FR_CA("fr-CA", "French – Canada", R.drawable.canada),
    FR_CH("fr-CH", "French – Switzerland", R.drawable.switzerland),
    FR_FR("fr-FR", "French – France", R.drawable.france),
    HE_IL("he-IL", "Hebrew – Israel", R.drawable.israel),
    HR_HR("hr-HR", "Croatian – Croatia", R.drawable.croatia),
    HU_HU("hu-HU", "Hungarian – Hungary", R.drawable.hungary),
    IT_IT("it-IT", "Italian – Italy", R.drawable.italy),
    JA_JP("ja-JP", "Japanese – Japan", R.drawable.japan),
    KO_KR("ko-KR", "Korean – Korea", R.drawable.south_korea),
    LT_LT("lt-LT", "Lithuanian – Lithuania", R.drawable.lithuania),
    LV_LV("lv-LV", "Latvian – Latvia", R.drawable.latvia),
    NB_NO("nb-NO", "Norwegian – Norway", R.drawable.norway),
    NL_BE("nl-BE", "Dutch – Belgium", R.drawable.belgium),
    NL_NL("nl-NL", "Dutch – Netherlands", R.drawable.netherlands),
    PL_PL("pl-PL", "Polish – Poland", R.drawable.poland),
    PT_BR("pt-BR", "Portuguese – Brazil", R.drawable.brazil),
    PT_PT("pt-PT", "Portuguese – Portugal", R.drawable.portugal),
    RO_RO("ro-RO", "Romanian – Romania", R.drawable.romania),
    RU_RU("ru-RU", "Russian – Russia", R.drawable.russia),
    SK_SK("sk-SK", "Slovak – Slovak Republic", R.drawable.slovakia),
    SL_SL("sl-SL", "Slovenian – Slovenia", R.drawable.slovenia),
    SV_SE("sv-SE", "Swedish – Sweden", R.drawable.sweden),
    TH_TH("th-TH", "Thai – Thailand", R.drawable.thailand),
    TR_TR("tr-TR", "Turkish – Turkey", R.drawable.turkey),
    UK_UA("uk-UA", "Ukrainian – Ukraine", R.drawable.ukraine),
    ZH_CN("zh-CN", "Chinese – China", R.drawable.china),
    ZH_HK("zh-HK", "Chinese – Hong Kong SAR", R.drawable.hong_kong),
    ZH_TW("zh-TW", "Chinese – Taiwan", R.drawable.taiwan);

    override fun toString(): String {
        return marketName
    }

    companion object {

        fun fromMarketCode(marketCode: String): BingMarket {
            return values().firstOrNull { it.marketCode == marketCode } ?: Unknown
        }

        fun selectableValues(): Array<BingMarket> {
            return values().copyOfRange(1, values().size - 1);
        }
    }

}
