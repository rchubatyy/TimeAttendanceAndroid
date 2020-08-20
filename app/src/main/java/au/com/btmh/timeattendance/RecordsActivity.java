package au.com.btmh.timeattendance;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;

public class RecordsActivity extends AppCompatActivity {

    private Spinner noDays;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records);
        noDays = findViewById(R.id.noDays);
        ArrayList<Integer> options = new ArrayList<>();
        for (int i=0; i<100; i++)
            options.add(i);
        ArrayAdapter<Integer> arrayAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, options);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        noDays.setAdapter(arrayAdapter);
    }


    public void clear(View v){
        //int days = noDays.
    }

    public void toSettings(View v){
        Intent intent = new Intent (this, SettingsActivity.class);
        startActivity(intent);
    }
}