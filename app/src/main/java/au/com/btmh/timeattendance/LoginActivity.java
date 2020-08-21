package au.com.btmh.timeattendance;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;
import static au.com.btmh.timeattendance.Constants.*;

public class LoginActivity extends AppCompatActivity implements Response.Listener<JSONObject>, Response.ErrorListener {

    private EditText emailField, passwordField;
    private TextView errorMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (UserManager.getInstance().isLoggedIn(this)){
            String token = UserManager.getInstance().getParam(this,"userToken");
            toBusinessFileSelect(token);
        }
        setContentView(R.layout.activity_login);
        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        errorMessage = findViewById(R.id.errorMessage);
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
        //finish();
    }
}