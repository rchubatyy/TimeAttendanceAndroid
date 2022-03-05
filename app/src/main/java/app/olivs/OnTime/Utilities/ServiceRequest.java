package app.olivs.OnTime.Utilities;

import android.content.Context;

import androidx.annotation.Nullable;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.Map;

import static app.olivs.OnTime.Utilities.Constants.getDefaultHeaders;

public class ServiceRequest extends JsonObjectRequest {

    private final Context context;

    public ServiceRequest(Context context, int method, String url, @Nullable JSONObject jsonRequest, Response.Listener<JSONObject> listener, @Nullable Response.ErrorListener errorListener) {
        super(method, url, jsonRequest, listener, errorListener);
        this.context = context;
        this.setRetryPolicy(new DefaultRetryPolicy(10000,
                1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    }
    @Override
    public Map<String, String> getHeaders() {
        return getDefaultHeaders(context);
    }

}
