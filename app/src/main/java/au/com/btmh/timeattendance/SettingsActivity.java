package au.com.btmh.timeattendance;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class SettingsActivity extends AppCompatActivity {

    private TextView userName, email, businessFile, syncStatus;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        userName = findViewById(R.id.userName);
        userName.setText(UserManager.getInstance().getParam(this, "name"));
        email = findViewById(R.id.email);
        email.setText("Your login: " + UserManager.getInstance().getParam(this, "email"));
        businessFile = findViewById(R.id.businessFile);
        businessFile.setText("Registered with: " + UserManager.getInstance().getParam(this, "email"));
        syncStatus = findViewById(R.id.syncStatus);
    }

    public void logOut(View v){
        Intent intent = new Intent (SettingsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        //finish();
    }

    public void changeFile(View v){

        Intent intent = new Intent (SettingsActivity.this, BusinessFileActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public void sync(View v){

    }
}