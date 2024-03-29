package app.olivs.OnTime.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

import app.olivs.OnTime.R;
import app.olivs.OnTime.Utilities.LanguageUtil;
import app.olivs.OnTime.Utilities.ServiceRequest;
import app.olivs.OnTime.Utilities.UserManager;

import static app.olivs.OnTime.Utilities.Constants.INIT_USER_AUTHENTIFICATION;

public class LoginActivity extends AppCompatActivity implements Response.Listener<JSONObject>, Response.ErrorListener {

    private EditText emailField, passwordField;
    private TextView errorMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        setContentView(R.layout.activity_login);
        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        errorMessage = findViewById(R.id.errorMessage);
        TextView forgotLink = findViewById(R.id.forgotLink);
        TextView privacyPolicyLink = findViewById(R.id.privacyPolicy);
        TextView registerLink = findViewById(R.id.registerLink);
        forgotLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://s1.olivs.com/0/"
                                + LanguageUtil.getCurrentLanguage() + "/olivs/forgot-password"));
                browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplicationContext().startActivity(browserIntent);
            }
        });
        privacyPolicyLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://olivs.com/privacy-policy/"));
                browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplicationContext().startActivity(browserIntent);
            }
        });
        registerLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://s1.olivs.com/0/"
                                + LanguageUtil.getCurrentLanguage() + "/olivs/create-new-user"));
                browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplicationContext().startActivity(browserIntent);
            }
        });
        passwordField.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    hideKeyboard();
                    return true;
                }
                return false;
            }
        });
    }

    protected void onResume() {
        super.onResume();
        errorMessage.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onResponse(@NonNull JSONObject response){
        try {
                String userToken = response.getString("usrUserToken");
                String name = response.getString("usrFirstName");
                String surname = response.getString("usrLastName");
                UserManager.getInstance().login(this, name + " " + surname, emailField.getText().toString(), userToken);
                toBusinessFileSelect(userToken);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        passwordField.setText("");
        if (error instanceof NetworkError || error instanceof AuthFailureError)
            showMessage(true, "No Internet - Failed to log in");
        else if (error instanceof ServerError) {
            String message = new String (error.networkResponse.data, StandardCharsets.UTF_8);
            showMessage(true, message.replaceAll("^\"|\"$", ""));
        }
        else if (error instanceof TimeoutError)
            showMessage(true, "Connection timeout - Failed to log in");
        else
        showMessage(true, "Failed to log in");
    }

    public void onLoginPressed(View view) throws JSONException {
        hideKeyboard();
        showMessage(false, "Logging in...");
        JSONObject body = new JSONObject();
        body.put("LoginEmail",emailField.getText());
        body.put("LoginPassword",passwordField.getText());
        final ServiceRequest request = new ServiceRequest(this, Request.Method.POST, INIT_USER_AUTHENTIFICATION, body, this, this) ;
        Volley.newRequestQueue(this).add(request);
    }

    private void showMessage(boolean isError, String message){
        errorMessage.setVisibility(View.VISIBLE);
        errorMessage.setTextColor(ContextCompat.getColor(this, isError ? R.color.colorError : R.color.colorMain));
        errorMessage.setText(message);
    }

    private void toBusinessFileSelect(String token){
        Intent intent = new Intent(this, BusinessFileActivity.class);
        intent.putExtra("userToken",token);
        startActivity(intent);
        finish();
    }


    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}