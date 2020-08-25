package au.com.btmh.timeattendance.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import au.com.btmh.timeattendance.R;
import au.com.btmh.timeattendance.Utilities.UserManager;

import static au.com.btmh.timeattendance.Utilities.Constants.*;

public class LoginActivity extends AppCompatActivity implements Response.Listener<JSONObject>, Response.ErrorListener {

    private EditText emailField, passwordField;
    private TextView errorMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        continueIfLoggedIn();
        setContentView(R.layout.activity_login);
        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        errorMessage = findViewById(R.id.errorMessage);
    }

    protected void onResume() {
        super.onResume();
        errorMessage.setVisibility(View.INVISIBLE);
        continueIfLoggedIn();
    }

    @Override
    public void onResponse(@NonNull JSONObject response){
        try {
            String success = response.getString("usrSuccess");
            if (success.equals("Y")){
                String userToken = response.getString("usrUserToken");
                String name = response.getString("usrFirstName");
                String surname = response.getString("usrLastName");
                UserManager.getInstance().login(this, name + " " + surname, emailField.getText().toString(), userToken);
                toBusinessFileSelect(userToken);
            }
            else{
                passwordField.setText("");
                String error = response.getString("usrErrorMessage");
                showMessage(true, error);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        passwordField.setText("");
        String message = null;
        if (error instanceof NetworkError || error instanceof AuthFailureError)
            showMessage(true, "No Internet - Failed to log in");
        else if (error instanceof ServerError)
            showMessage(true, "Server error - Failed to log in");
        else if (error instanceof TimeoutError)
            showMessage(true, "Connection timeout - Failed to log in");
        else
        showMessage(true, "Failed to log in");
    }

    public void onLoginPressed(View view) throws JSONException {
        showMessage(false, "Logging in...");
        JSONObject body = new JSONObject();
        body.put("Email",emailField.getText());
        body.put("Password",passwordField.getText());
        final JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, INIT_USER_AUTHENTIFICATION, body, this, this);
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
    }

    private void continueIfLoggedIn(){
        if (UserManager.getInstance().isLoggedIn(this)){
            String token = UserManager.getInstance().getParam(this,"userToken");
            toBusinessFileSelect(token);
        }
    }
}