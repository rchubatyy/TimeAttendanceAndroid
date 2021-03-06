package app.olivs.OnTime.Activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import app.olivs.OnTime.Model.ActivityState;
import app.olivs.OnTime.Model.CheckInInfo;
import app.olivs.OnTime.R;
import app.olivs.OnTime.Utilities.ConnectivityReceiver;
import app.olivs.OnTime.Utilities.DatabaseAccess;
import app.olivs.OnTime.Utilities.UserManager;

import static app.olivs.OnTime.Utilities.Constants.GET_COMPANY_INFORMATION;
import static app.olivs.OnTime.Utilities.Constants.REGISTER_USER_ACTIVITY;
import static app.olivs.OnTime.Utilities.Constants.getDefaultHeaders;
import static app.olivs.OnTime.Utilities.Constants.identifierForVendor;

public class CheckInActivity extends AppCompatActivity
        implements Response.Listener<JSONObject>, View.OnClickListener, ConnectivityReceiver.ConnectivityReceiverListener {

    private TextView companyInformation, areWeReady, results;
    private Button[] controlButtons;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationManager manager;
    private String userToken, dbToken;
    private DatabaseAccess databaseAccess;
    private CheckInInfo record;
    private JSONObject body;
    private JsonObjectRequest companyInfoRequest;
    private boolean infoShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in);
        ConnectivityReceiver.listener = this;
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
        companyInfoRequest = new JsonObjectRequest(Request.Method.POST, GET_COMPANY_INFORMATION, body, this, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                infoShown = false;
                companyInformation.setText(R.string.failed_to_load_company_information);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                return getDefaultHeaders(getApplicationContext());
            }
        };
        Volley.newRequestQueue(this).add(companyInfoRequest);
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
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        this.databaseAccess = DatabaseAccess.getInstance(getApplicationContext());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (permissions.length > 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationReadyStatus(manager);
            } else {
                areWeReady.setTextColor(ContextCompat.getColor(this, R.color.colorError));
                areWeReady.setText(R.string.location_not_enabled);
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
            infoShown = true;
            String infoHTML = response.getString("InfoMessage");
            companyInformation.setText(Html.fromHtml(infoHTML).toString().replaceAll("\n", ""));
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onClick(final View view) {
        if (!getLocationReadyStatus(manager) ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            areWeReady.setTextColor(ContextCompat.getColor(this, R.color.colorError));
            areWeReady.setText(R.string.location_not_available);

            return;
        }
        setControlButtonsEnabled(false);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
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
            body.put("IsLiveDataOrSync", "L");
            body.put("OSVersion", "Android " + Build.VERSION.RELEASE);
            body.put("PhoneModel", Build.MANUFACTURER + " " + Build.MODEL);
            body.put("IdentifierForVendor", identifierForVendor(this));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        results.setText(R.string.getting_location);
        getLastLocation();


    }

    private void setControlButtonsEnabled(boolean state) {
        for (Button button : controlButtons) {
            //button.setBackgroundTintList(ContextCompat.getColorStateList(this, state ? R.color.colorButton : R.color.colorDisabled));
            button.setEnabled(state);
            button.setOnClickListener(state ? this : null);
        }
        for (int i = 0; i < 4; i += 3)
            controlButtons[i].setBackgroundResource(state ? R.drawable.button_shape : R.color.colorDisabled);
        for (int i = 1; i < 3; i++)
            controlButtons[i].setBackgroundResource(state ? R.drawable.colorless_button_shape : R.color.colorDisabled);
    }

    private synchronized void prepareActivity(Location location) {
        results.setText(R.string.uploading_data);
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
        setControlButtonsEnabled(false);
        final Map<String, String> map = new HashMap<>();
        map.put(ActivityState.CHECKIN.name(), "Checked in");
        map.put(ActivityState.BREAKSTART.name(), "Started break");
        map.put(ActivityState.BREAKEND.name(), "Ended break");
        map.put(ActivityState.CHECKOUT.name(), "Checked out");
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        alertDialogBuilder.setMessage("Could not connect to service, but your activity is saved on your phone. \n \n" +
                "You need to sync the activity later. To do this, go to Settings > Sync all activity.");
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                setControlButtonsEnabled(true);
            }
        };
        final JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, REGISTER_USER_ACTIVITY, body, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                new Handler(Looper.getMainLooper()).postDelayed(runnable, 3000);
                try {
                    boolean isSite = response.getInt("acdSiteID") == 1;
                    String siteName = response.getString("acdSiteName");
                    //if (response.getString("acdSuccess").equals("Y")) {


                    String result = map.get(body.getString("ActivityType"));
                    if (isSite) {
                        result += ("\n at " + siteName);
                    } else
                        result += ("!\n" + siteName);

                    record.setResult(siteName, response.getString("acdID"));
                    results.setText(result);
                    alertDialogBuilder.setMessage(result);
                    /*} else {
                        String message = response.getString("acdErrorMessage");
                        results.setText(message);
                        record.setResult("", "");
                        alertDialogBuilder.setMessage(message);
                    }*/

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
                    new Handler(Looper.getMainLooper()).postDelayed(runnable, 3000);
                    record.setResult("", "");
                    String result = map.get(body.getString("ActivityType"));
                    result += "!\nCould not connect to service.";
                    results.setText(result);
                    databaseAccess.open();
                    databaseAccess.insertRecord(record);
                    databaseAccess.close();
                    final AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface arg0) {
                            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
                        }
                    });
                    alertDialog.show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                return getDefaultHeaders(getApplicationContext());
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(10000,
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
        areWeReady.setTextColor(Color.WHITE);
        List<String> providers = manager.getProviders(true);
        switch (manager.getProviders(true).size()) {
            case 3:
                areWeReady.setText(R.string.location_available_gps_network);
                setControlButtonsEnabled(true);
                return true;
            case 2:
                String provider = providers.get(1);
                if (provider.equals("gps"))
                    areWeReady.setText(R.string.location_available_gps);
                else
                    areWeReady.setText(R.string.location_available_network);
                setControlButtonsEnabled(true);
                return true;
            default:
                areWeReady.setTextColor(ContextCompat.getColor(this, R.color.colorError));
                areWeReady.setText(R.string.location_not_available);
                setControlButtonsEnabled(false);
                return false;
        }
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            LocationRequest locationRequest = LocationRequest.create()
                    .setInterval(500)
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
    }


    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        if (isConnected && !infoShown) {
            companyInformation.setText(R.string.loading_information);
            Volley.newRequestQueue(this).add(companyInfoRequest);
        }
    }
}