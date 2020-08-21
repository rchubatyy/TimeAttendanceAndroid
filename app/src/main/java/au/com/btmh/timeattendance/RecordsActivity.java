package au.com.btmh.timeattendance;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RecordsActivity extends AppCompatActivity implements Spinner.OnItemSelectedListener {

    private Spinner noDays;
    private String noDaysSelected;
    private List<CheckInInfo> records;
    private ListView recordsList;
    private DatabaseAccess databaseAccess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records);
        noDays = findViewById(R.id.noDays);
        noDays.setOnItemSelectedListener(this);
        recordsList = findViewById(R.id.recordsList);
        ArrayList<Integer> options = new ArrayList<>();
        for (int i=0; i<100; i++)
            options.add(i);
        ArrayAdapter<Integer> arrayAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, options);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        noDays.setAdapter(arrayAdapter);
        databaseAccess = DatabaseAccess.getInstance(getApplicationContext());
        showRecords();
    }


    public void clear(View v){
        databaseAccess.open();
        databaseAccess.clearRecords(this, noDaysSelected, new DatabaseAccess.onClearCompleteListener() {
            @Override
            public void reloadData() {
                showRecords();
            }
        });
    }

    public void toSettings(View v){
        Intent intent = new Intent (this, SettingsActivity.class);
        startActivity(intent);
    }

    private void showRecords(){
        databaseAccess.open();
        records = databaseAccess.getAllRecords(this,false);
        ArrayAdapter<String> recordsAdapter = new ArrayAdapter<String>(this, R.layout.text_view_cell_layout, R.id.listViewItem, getRecordData(records)){
            @Override
            public View getView(int position, View convertView, ViewGroup parent){
                View view = super.getView(position, convertView, parent);
                TextView text = view.findViewById(R.id.listViewItem);
                if(records.get(position).getResultID().equals(""))
                    text.setTextColor(ContextCompat.getColor(getContext(),R.color.colorError));
                else
                    text.setTextColor(Color.BLACK);
                return view;
            }
        };
        recordsList.setAdapter(recordsAdapter);
    }

    @NotNull
    private List<String> getRecordData(@NotNull List<CheckInInfo> db){
        List<String> list = new ArrayList<>();
        for (CheckInInfo record: db)
            list.add("" + record.getTime() + " | " + record.getState().name());
        return list;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        noDaysSelected = adapterView.getItemAtPosition(i).toString();
        System.out.println(noDaysSelected);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}