package app.olivs.OnTime.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;

import app.olivs.OnTime.R;
import app.olivs.OnTime.Utilities.DatabaseAccess;
import app.olivs.OnTime.Utilities.UserManager;

public class SettingsActivity extends AppCompatActivity {

    private TextView syncStatus;
    private DatabaseAccess databaseAccess;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        TextView userName = findViewById(R.id.userName);
        userName.setText(UserManager.getInstance().getParam(this, "name"));
        TextView email = findViewById(R.id.email);
        email.setText("Your login: " + UserManager.getInstance().getParam(this, "email"));
        TextView businessFile = findViewById(R.id.businessFile);
        businessFile.setText("Registered with:\n" + UserManager.getInstance().getParam(this, "businessFileName"));
        syncStatus = findViewById(R.id.syncStatus);
        databaseAccess = DatabaseAccess.getInstance(getApplicationContext());
        TextView link = findViewById(R.id.link);
        link.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://know.olivs.app/time-attendance/mobile-app/"));
                browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplicationContext().startActivity(browserIntent);
            }
        });
    }

    protected void onResume() {
        super.onResume();
        syncStatus.setVisibility(View.INVISIBLE);
    }

    public void logOut(View v){
        UserManager.getInstance().logout(this);
        Intent intent = new Intent (SettingsActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        //finish();
    }

    public void changeFile(View v){
        Intent intent = new Intent (SettingsActivity.this, BusinessFileActivity.class);
        startActivity(intent);
    }

    public void sync(View v) throws JSONException {
        syncStatus.setVisibility(View.VISIBLE);
        syncStatus.setTextColor(ContextCompat.getColor(this,R.color.colorMain));
        syncStatus.setText("Syncing...");
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