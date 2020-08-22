package au.com.btmh.timeattendance.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import au.com.btmh.timeattendance.R;
import au.com.btmh.timeattendance.Utilities.DatabaseAccess;
import au.com.btmh.timeattendance.Utilities.UserManager;

public class SettingsActivity extends AppCompatActivity {

    private TextView userName, email, businessFile, syncStatus;
    private DatabaseAccess databaseAccess;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        userName = findViewById(R.id.userName);
        userName.setText(UserManager.getInstance().getParam(this, "name"));
        email = findViewById(R.id.email);
        email.setText("Your login: " + UserManager.getInstance().getParam(this, "email"));
        businessFile = findViewById(R.id.businessFile);
        businessFile.setText("Registered with: " + UserManager.getInstance().getParam(this, "businessFileName"));
        syncStatus = findViewById(R.id.syncStatus);
        databaseAccess = DatabaseAccess.getInstance(getApplicationContext());
    }

    protected void onResume() {
        super.onResume();
        syncStatus.setVisibility(View.INVISIBLE);
    }

    public void logOut(View v){
        UserManager.getInstance().logout(this);
        Intent intent = new Intent (SettingsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        //finish();
    }

    public void changeFile(View v){
        UserManager.getInstance().removedBusinessFile(this);
        Intent intent = new Intent (SettingsActivity.this, BusinessFileActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public void sync(View v) throws JSONException {
        syncStatus.setVisibility(View.VISIBLE);
        syncStatus.setTextColor(ContextCompat.getColor(this,R.color.colorMain));
        databaseAccess.open();
        databaseAccess.sync(this, new DatabaseAccess.onSyncCompleteListener() {
            @Override
            public void showMessage(boolean isError, String message) {
                syncStatus.setTextColor(ContextCompat.getColor(getBaseContext(),isError ? R.color.colorError : R.color.colorMain));
                syncStatus.setText(message);
            }
        });
    }
}