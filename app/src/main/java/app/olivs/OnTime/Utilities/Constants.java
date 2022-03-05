package app.olivs.OnTime.Utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.Nullable;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import app.olivs.OnTime.R;

public class Constants {

    public static final String BASE_URL = "https://ontimeappservice1.olivs.cloud/api/" + LanguageUtil.getCurrentLanguage() + "/";
    public static final String BASE_URL_V1 = BASE_URL + "app/";
    public static final String BASE_URL_V2 = BASE_URL + "ontimev2/";

    public static final String INIT_USER_AUTHENTIFICATION = BASE_URL_V2 + "init-user-authentification";
    public static final String GET_USER_BUSINESS_FILES_LIST = BASE_URL_V2 + "get-user-business-files-list";
    public static final String GET_COMPANY_INFORMATION = BASE_URL_V1 + "get-company-information";
    public static final String GET_USER_INFO = BASE_URL_V2 + "get-user-info";
    public static final String REGISTER_USER_ACTIVITY = BASE_URL_V2 + "register-user-activity";


    private static String uniqueID = null;
    private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";

    public static Map<String, String> getDefaultHeaders(Context context) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Api-Key", context.getString(R.string.api_key));
        headers.put("Olivs-Root-Password", context.getString(R.string.olivs_root_password));
        headers.put("Olivs-API-Real-IP", "127.0.0.1");
        headers.put("Olivs-API-Computer-Name", "BTMSOFTPC");
        return headers;
    }

    public synchronized static String identifierForVendor(Context context) {
            if (uniqueID == null) {
                SharedPreferences sharedPrefs = context.getSharedPreferences(
                        PREF_UNIQUE_ID, Context.MODE_PRIVATE);
                uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
                if (uniqueID == null) {
                    uniqueID = UUID.randomUUID().toString();
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putString(PREF_UNIQUE_ID, uniqueID);
                    editor.apply();
                }
            }
            return uniqueID;
        }



}
