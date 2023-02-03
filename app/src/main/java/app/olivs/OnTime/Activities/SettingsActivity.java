package app.olivs.OnTime.Activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import app.olivs.OnTime.BuildConfig;
import app.olivs.OnTime.R;
import app.olivs.OnTime.Utilities.DataManager;
import app.olivs.OnTime.Utilities.DatabaseAccess;
import app.olivs.OnTime.Utilities.UserManager;

public class SettingsActivity extends AppCompatActivity {

    private TextView syncStatus, nextReminder, version;
    private DatabaseAccess databaseAccess;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        TextView userName = findViewById(R.id.userName);
        userName.setText(UserManager.getInstance().getParam(this, "name"));
        TextView email = findViewById(R.id.email);
        email.setText(String.format("Your login: %s", UserManager.getInstance().getParam(this, "email")));
        TextView businessFile = findViewById(R.id.businessFile);
        businessFile.setText(String.format("Registered with:\n%s", UserManager.getInstance().getParam(this, "businessFileName")));
        version = findViewById(R.id.version);
        version.setText(String.format("Version: %s", BuildConfig.VERSION_NAME));
        syncStatus = findViewById(R.id.syncStatus);
        nextReminder = findViewById(R.id.nextReminder);
        String nextReminderDate = DataManager.getInstance().getNotificationTime(this);
        if (nextReminderDate != null) {
            nextReminder.setVisibility(View.VISIBLE);
            @SuppressLint("SimpleDateFormat") SimpleDateFormat format1 = new SimpleDateFormat("dd MMM yyyy HH:mm");
            @SuppressLint("SimpleDateFormat") SimpleDateFormat format2 = new SimpleDateFormat("dd-MM HH:mm");
            Date date = null;
            try {
                date = format1.parse(nextReminderDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            nextReminder.setText(getResources().getString(R.string.next_reminder) + (date!=null ? format2.format(date): nextReminderDate));
        }
        else {
            nextReminder.setVisibility(View.GONE);
            nextReminder.setText("");
        }
        databaseAccess = DatabaseAccess.getInstance(getApplicationContext());
        TextView link = findViewById(R.id.link);
        link.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://help.olivs.com/ontime/"));
                browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplicationContext().startActivity(browserIntent);
            }
        });
        TextView privacyPolicyLink = findViewById(R.id.privacyPolicy);
        privacyPolicyLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://olivs.com/privacy-policy/"));
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
    }

    public void changeFile(View v){
        Intent intent = new Intent (SettingsActivity.this, BusinessFileActivity.class);
        startActivity(intent);
    }

    public void sync(View v) throws JSONException {
        syncStatus.setVisibility(View.VISIBLE);
        syncStatus.setTextColor(ContextCompat.getColor(this,R.color.colorMain));
        syncStatus.setText(R.string.syncing);
        databaseAccess.open();
        databaseAccess.sync(this, new DatabaseAccess.onSyncCompleteListener() {
            @Override
            public void showMessage(boolean isError, String message) {
                syncStatus.setTextColor(ContextCompat.getColor(getBaseContext(),isError ? R.color.colorError : R.color.colorMain));
                syncStatus.setText(message);
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        syncStatus.setText("");
                    }
                }, 3000);
            }
        });
    }
}