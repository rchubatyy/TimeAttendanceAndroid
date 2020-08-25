package au.com.btmh.timeattendance.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import au.com.btmh.timeattendance.Model.ActivityState;
import au.com.btmh.timeattendance.Model.CheckInInfo;
import au.com.btmh.timeattendance.Utilities.DatabaseAccess;
import au.com.btmh.timeattendance.R;
import au.com.btmh.timeattendance.Utilities.UserManager;

import static au.com.btmh.timeattendance.Utilities.Constants.*;

public class CheckInActivity extends AppCompatActivity
        implements Response.Listener<JSONObject>, Response.ErrorListener, View.OnClickListener {

    private TextView companyInformation, areWeReady, results;
    private Button[] controlButtons;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationManager manager;
    private String userToken, dbToken;
    private DatabaseAccess databaseAccess;
    private CheckInInfo record;
    private JSONObject body;

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
        manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        BroadcastReceiver locationSwitchStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (Objects.requireNonNull(intent.getAction()).matches("android.location.PROVIDERS_CHANGED"))
                    getLocationReadyStatus(manager);
            }
        };
        this.registerReceiver(locationSwitchStateReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        this.databaseAccess = DatabaseAccess.getInstance(getApplicationContext());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (permissions.length > 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationReadyStatus(manager);
            } else {
                areWeReady.setText("Sorry, you can't check in because location permission is not enabled.");
                setControlButtonsEnabled(false);
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }

    @Override
    public void onResponse(@NotNull JSONObject response) {
        try {
            String success = response.getString("cmpSuccess");
            if (success.equals("Y")) {
                String infoHTML = response.getString("cmpInfoMessage");
                companyInformation.setText(Html.fromHtml(infoHTML));
            } else {
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
        if (!getLocationReadyStatus(manager) ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            areWeReady.setText("Sorry, you can't use the service because location is not available.");
            setControlButtonsEnabled(false);
            return;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        final String time = dateFormat.format(Calendar.getInstance().getTime());
        final String activityType = view.getResources().getResourceEntryName(view.getId());
        ActivityState state = ActivityState.valueOf(activityType);
        record = new CheckInInfo(userToken, dbToken, time, state, true);
        body = new JSONObject();
        try {
            body.put("UserToken", userToken);
            body.put("DBToken", dbToken);
            body.put("ActivityType", activityType);
            body.put("PhDateTime", time);
            body.put("isLiveDataOrSync", "L");
            body.put("OSVersion", "Android " + Build.VERSION.RELEASE);
            body.put("PhoneModel", Build.MANUFACTURER + " " + Build.MODEL);
            //record.setLocation(location.getLatitude(), location.getLongitude());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        results.setText("Getting location...");
        getLastLocation();



    }

    private void setControlButtonsEnabled(boolean state) {
        for (Button button : controlButtons) {
            button.setBackgroundTintList(ContextCompat.getColorStateList(this, state ? R.color.colorMain : R.color.colorDisabled));
            button.setEnabled(state);
            button.setOnClickListener(state ? this : null);
        }
    }

    private synchronized void prepareActivity(Location location){
        results.setText("Uploading data...");
        try {
            body.put("GPSLat", location.getLatitude());
            body.put("GPSLon", location.getLongitude());
            record.setLocation(location.getLatitude(), location.getLongitude());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        uploadActivity();
    }

    private synchronized void uploadActivity() {
        final Map<String, String> map = new HashMap();
        map.put(ActivityState.CHECKIN.name(), "Checked in");
        map.put(ActivityState.BREAKSTART.name(), "Started break");
        map.put(ActivityState.BREAKEND.name(), "Ended break");
        map.put(ActivityState.CHECKOUT.name(), "Checked out");
        final JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, REGISTER_USER_ACTIVITY, body, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    boolean isSite = response.getInt("acdSiteID") == 1;
                    String siteName = response.getString("acdSiteName");
                    if (response.getString("acdSuccess").equals("Y")) {


                        String result = map.get(body.getString("ActivityType"));
                        if (isSite) {
                            result += ("\n at " + siteName);
                        } else
                            result += ("!\n" + siteName);

                        record.setResult(siteName, response.getString("acdID"));
                        results.setText(result);
                    } else {
                        results.setText(response.getString("acdErrorMessage"));
                        record.setResult("", "");
                    }

                    databaseAccess.open();
                    databaseAccess.insertRecord(record);
                    databaseAccess.close();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                try {
                    record.setResult("", "");
                    String result = map.get(body.getString("ActivityType"));
                    if (error instanceof ServerError)
                        result += "!\nServer error.";
                    else if (error instanceof TimeoutError)
                        result += "!\nConnection timeout.";
                    else
                        result += "!\nFailed to connect.";
                        result +=  " Saving activity on the phone.";
                    databaseAccess.open();
                    databaseAccess.insertRecord(record);
                    databaseAccess.close();
                    results.setText(result);
                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        });
        request.setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Volley.newRequestQueue(this).add(request);
    }

    public void toMyHistory(View v) {
        Intent intent = new Intent(this, RecordsActivity.class);
        startActivity(intent);
    }

    public void toSettings(View v) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }


    private boolean getLocationReadyStatus(LocationManager manager) {
        List<String> providers = manager.getProviders(true);
        switch (manager.getProviders(true).size()) {
            case 3:
                areWeReady.setText("Location is available through GPS and network.");
                setControlButtonsEnabled(true);
                return true;
            case 2:
                String provider = providers.get(1);
                if (provider.equals("gps"))
                    areWeReady.setText("Location is available through GPS.");
                else
                    areWeReady.setText("Location is available through network.");
                setControlButtonsEnabled(true);
                return true;
            default:
                areWeReady.setText("Sorry, you can't use the service because location is not available.");
                setControlButtonsEnabled(false);
                return false;
        }
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    Location location = task.getResult();
                    if (location == null) {
                        LocationRequest locationRequest = new LocationRequest()
                                .setInterval(5)
                                .setFastestInterval(0)
                                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                                .setNumUpdates(1);
                        final LocationCallback locationCallback = new LocationCallback() {
                            @Override
                            public void onLocationResult(LocationResult locationResult) {
                                super.onLocationResult(locationResult);
                                Location location = locationResult.getLastLocation();
                                prepareActivity(location);
                            }
                        };
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                        }
                    }
                    else
                        prepareActivity(location);
                }

            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    results.setText("Failed to get location.");
                }
            });
        }
    }



}