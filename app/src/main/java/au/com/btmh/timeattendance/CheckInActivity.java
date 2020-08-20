package au.com.btmh.timeattendance;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static au.com.btmh.timeattendance.Constants.*;

public class CheckInActivity extends AppCompatActivity
        implements Response.Listener<JSONObject>, Response.ErrorListener, View.OnClickListener {

    private TextView companyInformation, areWeReady, results;
    private Button[] controlButtons;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private String userToken, dbToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in);
        companyInformation = findViewById(R.id.companyInformation);
        areWeReady = findViewById(R.id.areWeReady);
        results = findViewById(R.id.results);
        controlButtons = new Button[4];
        for (int i = 0; i < ActivityState.values().length; i++) {
            int id = getResources().getIdentifier(ActivityState.values()[i].name(), "id", getPackageName());
            controlButtons[i] = findViewById(id);
        }
        JSONObject body = new JSONObject();
        Intent intent = getIntent();
        dbToken = intent.getStringExtra("token");
        userToken = UserManager.getInstance().getParam(this, "userToken");
        try {
            body.put("UserToken", userToken);
            body.put("DBToken", dbToken);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, GET_COMPANY_INFORMATION, body, this, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                companyInformation.setText("Failed to load company information.");
            }
        });
        Volley.newRequestQueue(this).add(request);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION)
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    areWeReady.setText("Location is available.");
                    setControlButtonsEnabled(true);
                } else {
                    areWeReady.setText("Sorry, you can't check in because location is not available.");
                    setControlButtonsEnabled(false);
                }
            }
        }
    }

    @Override
    public void onResponse(@NotNull JSONObject response) {
        try {
                String success = response.getString("cmpSuccess");
                if (success.equals("Y")) {
                    String infoHTML = response.getString("cmpInfoMessage");
                    companyInformation.setText(Html.fromHtml(infoHTML));
                }
                else {
                    String error = response.getString("cmpError");
                    companyInformation.setText(error);
                }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onErrorResponse(VolleyError error) {

    }


    @Override
    public void onClick(final View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        results.setText("Getting location...");
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                results.setText("Updating location...");
                JSONObject body = new JSONObject();
                try {
                    body.put("UserToken", userToken);
                    body.put("DBToken", dbToken);
                    String activityType = view.getResources().getResourceEntryName(view.getId());
                    body.put("ActivityType", activityType);
                    body.put("GPSLat",location.getLatitude());
                    body.put("GPSLon",location.getLongitude());
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    body.put("PhDateTime",dateFormat.format(Calendar.getInstance().getTime()));
                    body.put("isLiveDataOrSync", "L");
                    body.put("OSVersion", "Android " + Build.VERSION.RELEASE);
                    body.put("PhoneModel", Build.MANUFACTURER + " " + Build.MODEL);
                }
             catch (JSONException e) {
                e.printStackTrace();
            }
                uploadActivity(body);
            }
        });
    }

    private void setControlButtonsEnabled(boolean state){
        for (Button button: controlButtons){
            button.setBackgroundTintList(ContextCompat.getColorStateList(this,state ? R.color.colorMain : R.color.colorDisabled));
            button.setEnabled(state);
            button.setOnClickListener(state ? this : null);
        }
    }

    private void uploadActivity(final JSONObject body){
        final JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, REGISTER_USER_ACTIVITY, body, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                System.out.println(response);
                try {
                    if (response.getString("acdSuccess").equals("Y")){
                        boolean isSite = response.getInt("acdSiteID") == 1;
                        String siteName = response.getString("acdSiteName");
                        Map<String, String> map = new HashMap();
                        map.put(ActivityState.CHECKIN.name(), "Checked in");
                        map.put(ActivityState.BREAKSTART.name(), "Started break");
                        map.put(ActivityState.BREAKEND.name(), "Ended break");
                        map.put(ActivityState.CHECKOUT.name(), "Checked out");
                        String result = map.get(body.getString("ActivityType"));
                        if (isSite)
                            result += ("\n at " + siteName);
                        else
                            result += ("!\n" + siteName);
                        results.setText(result);
                    }
                    else
                        results.setText(response.getString("acdErrorMessage"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, this);
        Volley.newRequestQueue(this).add(request);
    }

    public void toMyHistory(View v){
        Intent intent = new Intent (this, RecordsActivity.class);
        startActivity(intent);
    }

    public void toSettings(View v){
        Intent intent = new Intent (this, SettingsActivity.class);
        startActivity(intent);
    }
}