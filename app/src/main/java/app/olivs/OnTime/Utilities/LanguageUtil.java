package app.olivs.OnTime.Utilities;

import java.util.Arrays;
import java.util.Locale;

public class LanguageUtil {

    private static final String DEFAULT_LANGUAGE = "en-au";

    public static String[] getAllLanguages(){
        return new String[]{DEFAULT_LANGUAGE};
    }

    public static String getCurrentLanguage(){
        String[] languages = getAllLanguages();
        String current = Locale.getDefault().getLanguage()+ "-" + Locale.getDefault().getCountry().toLowerCase();
        if (Arrays.asList(languages).contains(current))
            return current;
        else
            return DEFAULT_LANGUAGE;

    }

}
