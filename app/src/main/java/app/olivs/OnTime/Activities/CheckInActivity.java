package app.olivs.OnTime.Activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Html;
import android.text.SpannableString;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
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

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import app.olivs.OnTime.Model.ActivityState;
import app.olivs.OnTime.Model.CheckInInfo;
import app.olivs.OnTime.R;
import app.olivs.OnTime.Receivers.CheckOutNotifier;
import app.olivs.OnTime.Receivers.ConnectivityReceiver;
import app.olivs.OnTime.Utilities.DataManager;
import app.olivs.OnTime.Utilities.DatabaseAccess;
import app.olivs.OnTime.Utilities.NetworkUtil;
import app.olivs.OnTime.Utilities.ServiceRequest;
import app.olivs.OnTime.Utilities.UserManager;

import static app.olivs.OnTime.Utilities.Constants.GET_COMPANY_INFORMATION;
import static app.olivs.OnTime.Utilities.Constants.GET_USER_INFO;
import static app.olivs.OnTime.Utilities.Constants.REGISTER_USER_ACTIVITY;
import static app.olivs.OnTime.Utilities.Constants.getDefaultHeaders;
import static app.olivs.OnTime.Utilities.Constants.identifierForVendor;

public class CheckInActivity extends AppCompatActivity
        implements Response.Listener<JSONObject>, View.OnClickListener, ConnectivityReceiver.ConnectivityReceiverListener,
        LocationListener {

    private TextView companyInformation, areWeReady, results;
    private Button[] controlButtons;
    private int[] questionIds;
    private String[] questions;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationManager manager;
    private String userToken, dbToken;
    private DatabaseAccess databaseAccess;
    private ActivityState lastActivityState;
    private CheckInInfo record;
    private JSONObject body;
    private ServiceRequest companyInfoRequest;
    private boolean infoShown = false;
    private boolean userInfoLoaded = false;
    private boolean locationAvailable = false;
    private boolean gettingLocation = false;
    private Runnable alertMessage;
    public static Handler myHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in);
        ConnectivityReceiver.listener = this;
        createNotificationChannel();
        companyInformation = findViewById(R.id.companyInformation);
        areWeReady = findViewById(R.id.areWeReady);
        results = findViewById(R.id.results);
        alertMessage = new Runnable() {
            @Override
            public void run() {
                showAlertMessage();
            }
        };
        final int BUTTON_COUNT = ActivityState.values().length;
        controlButtons = new Button[BUTTON_COUNT];
        questionIds = new int[BUTTON_COUNT];
        questions = new String[BUTTON_COUNT];
        getLastOfflineData();
        for (int i = 0; i < BUTTON_COUNT; i++) {
            int id = getResources().getIdentifier(ActivityState.values()[i].name(), "id", getPackageName());
            controlButtons[i] = findViewById(id);
        }
        JSONObject body = new JSONObject();
        Intent intent = getIntent();
        dbToken = intent.getStringExtra("token");
        userToken = UserManager.getInstance().getParam(this, "userToken");
        body = new JSONObject();
        try {
            body.put("UserToken", userToken);
            body.put("DBToken", dbToken);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        getCompanyInformation();
        getUserInfo();
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
        startTimer();
    }

    @Override
    public void onStop(){
        super.onStop();
        stopTimer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.cancelAll();
    }

    @Override
    public void onRestart(){
        super.onRestart();
        getUserInfo();
        resetTimer();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (permissions.length > 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationReadyStatus(manager);
            } else {
                areWeReady.setTextColor(ContextCompat.getColor(this, R.color.colorError));
                areWeReady.setText(R.string.location_not_enabled);
                locationAvailable = false;
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
            infoHTML = infoHTML.replaceAll("&lt;", "<");
            infoHTML = infoHTML.replaceAll("&gt;", ">");
            SpannableString infoParsed = new SpannableString(Html.fromHtml(infoHTML).toString().replaceAll("\n", ""));
            companyInformation.setText(infoParsed, TextView.BufferType.SPANNABLE);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onClick(final View view){
        resetTimer();
        getUserInfo();
        if (!getLocationReadyStatus(manager) ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            areWeReady.setTextColor(ContextCompat.getColor(this, R.color.colorError));
            areWeReady.setText(R.string.location_not_available);
            return;
        }
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        final String time = dateFormat.format(Calendar.getInstance().getTime());
        final String activityType = view.getResources().getResourceEntryName(view.getId());
        ActivityState state = ActivityState.valueOf(activityType);
        int index = state.ordinal();
        body = new JSONObject();
        int questionId = questionIds[index];
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
        prepareBody(time, state);
    }

    private void prepareBody(String time, ActivityState state){
        record = new CheckInInfo(userToken, dbToken, time, state, true);
        //try {
            //body.put("QuestionID", questionId);
            //body.put("Answer", answer);
            setControlButtonsEnabled(false);
            gettingLocation = true;
            results.setText(R.string.getting_location);
            getLastLocation();
        //} catch (JSONException e) {
          //  e.printStackTrace();
        //}
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
                getUserInfo();
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
        final ServiceRequest request = new ServiceRequest(this, Request.Method.POST, REGISTER_USER_ACTIVITY, body, new Response.Listener<JSONObject>() {
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
                    if (NetworkUtil.getConnectivityStatus(getBaseContext())!=NetworkUtil.TYPE_NOT_CONNECTED)
                    {
                        databaseAccess.open();
                        databaseAccess.sync(getBaseContext(), null);
                    }
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
        });
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
        int providersCount = providers.size();
        if (providersCount >= 3)
        {
            areWeReady.setText(R.string.location_available_gps_network);
            locationAvailable = true;
            if (userInfoLoaded && !gettingLocation)
                setControlButtonsEnabled(true);
            return true;
        }
        else
        switch (providersCount) {
            case 2:
                String provider = providers.get(1);
                if (provider.equals("gps"))
                    areWeReady.setText(R.string.location_available_gps);
                else
                    areWeReady.setText(R.string.location_available_network);
                locationAvailable = true;
                if (userInfoLoaded && !gettingLocation)
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
            final Handler handler = new Handler(Looper.getMainLooper());
            Runnable runnable;
            LocationRequest locationRequest = LocationRequest.create()
                    .setInterval(500)
                    .setFastestInterval(0)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setNumUpdates(1);
            final LocationCallback locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    handler.removeCallbacksAndMessages(null);
                    gettingLocation = false;
                    Location location = locationResult.getLastLocation();
                    prepareActivity(location);
                }
            };
            runnable = new Runnable(){
                @Override
                public void run() {
                    fusedLocationProviderClient.removeLocationUpdates(locationCallback);
                    results.setText("Failed to get location.");
                    gettingLocation = false;
                    setControlButtonsEnabled(true);
                }
            };
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                //fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 5, this);
                manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 5, this);
            }
            //handler.postDelayed(runnable, 10000);
        }
    }


    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        if (isConnected && !infoShown) {
            companyInformation.setText(R.string.loading_information);
            Volley.newRequestQueue(this).add(companyInfoRequest);
        }
    }

    private void createNotificationChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("OnTime", "OnTime", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void scheduleNotify(String when) throws ParseException {

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getApplicationContext(), CheckOutNotifier.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(),0,intent,PendingIntent.FLAG_MUTABLE);
        am.cancel(pendingIntent);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy HH:mm");
        if (when!=null) {
            Date date = format.parse(when);
            if (date !=null) {
                long diff = date.getTime() - Calendar.getInstance().getTimeInMillis();
                if (diff >= 0)
                    am.set(AlarmManager.RTC_WAKEUP, date.getTime(), pendingIntent);
            }
        }
    }

    private void getCompanyInformation(){
        JSONObject body = new JSONObject();
        try {
            body.put("UserToken", userToken);
            body.put("DBToken", dbToken);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        companyInfoRequest = new ServiceRequest(this, Request.Method.POST, GET_COMPANY_INFORMATION, body, this, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error instanceof ServerError){
                    showErrorThenLogout(new String(error.networkResponse.data, StandardCharsets.UTF_8).replaceAll("\"", ""));
                }
                else {
                    infoShown = false;
                    companyInformation.setText(R.string.failed_to_load_company_information);
                }
            }
        });
        Volley.newRequestQueue(this).add(companyInfoRequest);
    }


    private void getUserInfo(){
        JSONObject body = new JSONObject();
        try {
            body.put("UserToken", userToken);
            body.put("DBToken", dbToken);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ServiceRequest userInfoRequest = new ServiceRequest(this, Request.Method.POST, GET_USER_INFO, body, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                System.out.println(response);
                userInfoLoaded = true;
                if (locationAvailable && !gettingLocation)
                setControlButtonsEnabled(true);
                try {
                    if (response.isNull("usrLastEvnt_otlCheckType"))
                        lastActivityState = ActivityState.CHECKOUT;
                    else {
                        String activityStateValue = response.getString("usrLastEvnt_otlCheckType");
                        if (activityStateValue.equals(""))
                            lastActivityState = ActivityState.CHECKOUT;
                        else
                            lastActivityState = ActivityState.valueOf(activityStateValue);

                    }
                    DataManager.getInstance().setLastActivityType(getApplicationContext(), lastActivityState);
                    String time = null;
                    if (!response.isNull("rsrRosterReminderData")) {
                        JSONObject roster = response.getJSONObject("rsrRosterReminderData");
                        time = roster.getString("rsrReminderDateTime");
                        DataManager.getInstance().setNotificationTime(getApplicationContext(), time);
                    }
                    else{
                        DataManager.getInstance().setNotificationTime(getApplicationContext(), null);
                    }
                    scheduleNotify(time);
                    assert ActivityState.values().length == 4;
                    final String[] ACTIVITY_KEYS = new String[]{"CheckIn", "BreakStart", "BreakEnd", "CheckOut"};
                    final String QUESTION_ID = "QuestionID";
                    final String QUESTION_TEXT = "QuestionText";
                    for (int i = 0; i < ActivityState.values().length; i++) {
                        String key = "qckQuestion" + ACTIVITY_KEYS[i];
                        if (response.isNull(key)){
                            questionIds[i] = 0;
                            questions[i] = null;
                        }
                        else {
                            JSONObject question = response.getJSONObject(key);
                            questionIds[i] = question.isNull(QUESTION_ID)? 0 : question.getInt(QUESTION_ID);
                            questions[i] = question.isNull(QUESTION_TEXT)? null : question.getString(QUESTION_TEXT);
                        }
                        DataManager.getInstance().setQuestionIdFor(getApplicationContext(), ActivityState.values()[i], questionIds[i]);
                        DataManager.getInstance().setQuestionFor(getApplicationContext(), questionIds[i], questions[i]);
                    }
                } catch (JSONException | ParseException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                userInfoLoaded = true;
                if (locationAvailable && !gettingLocation)
                setControlButtonsEnabled(true);
                System.out.println("fail");
            }
        });
        userInfoRequest.setRetryPolicy(new DefaultRetryPolicy(5000,
                1000,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Volley.newRequestQueue(this).add(userInfoRequest);
    }

    private void showAlertMessage(){
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(R.string.we_havent_seen_you);
        alertDialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                resetTimer();
                getCompanyInformation();
                getUserInfo();
            }
        });
        final AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface arg0) {
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
                    }
                });
        alertDialog.show();
    }

    private void startTimer(){
        myHandler.postDelayed(alertMessage,60000);
    }

    private void stopTimer(){
        myHandler.removeCallbacks(alertMessage);
    }

    private void resetTimer(){
        getUserInfo();
        stopTimer();
        startTimer();
    }

    private void getLastOfflineData(){
        lastActivityState = DataManager.getInstance().getLastActivityType(this);
        for (int i=0; i<ActivityState.values().length; i++){
            questionIds[i] = DataManager.getInstance().getQuestionIdFor(this, ActivityState.values()[i]);
            questions[i] = DataManager.getInstance().getQuestionFor(this, questionIds[i]);
        }
    }


    @Override
    public void onLocationChanged(@NonNull Location location) {
        manager.removeUpdates(this);
        gettingLocation = false;
        prepareActivity(location);
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {

    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    private void showErrorThenLogout(String message){
        UserManager.getInstance().removedBusinessFile(this);
        UserManager.getInstance().logout(this);
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent (CheckInActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
        alertDialogBuilder.setMessage(message);
        final AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface arg0) {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
            }
        });
        alertDialog.show();

    }
}