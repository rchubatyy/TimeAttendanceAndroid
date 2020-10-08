package app.olivs.OnTime.Activities;

import android.content.Intent;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.Display;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import app.olivs.OnTime.R;
import app.olivs.OnTime.Utilities.UserManager;

public class SplashScreenActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent;
        if (UserManager.getInstance().isLoggedIn(this)){
            if (UserManager.getInstance().fileSelected(this) >= 0) {
                String token = UserManager.getInstance().getParam(this,"businessFileToken");
                intent = new Intent(this, CheckInActivity.class);
                intent.putExtra("token",token);
            }
            else {
                String token = UserManager.getInstance().getParam(this, "userToken");
                intent = new Intent(this, BusinessFileActivity.class);
                intent.putExtra("userToken", token);
            }
            startActivity(intent);
        }
        else
        startActivity(new Intent(SplashScreenActivity.this, LoginActivity.class));
        finish();
    }
}