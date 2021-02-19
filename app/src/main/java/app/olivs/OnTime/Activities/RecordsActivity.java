package app.olivs.OnTime.Activities;

import android.content.DialogInterface;
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import app.olivs.OnTime.Model.CheckInInfo;
import app.olivs.OnTime.R;
import app.olivs.OnTime.Utilities.DatabaseAccess;

public class RecordsActivity extends AppCompatActivity implements Spinner.OnItemSelectedListener {

    private String noDaysSelected;
    private List<CheckInInfo> records;
    private ListView recordsList;
    private DatabaseAccess databaseAccess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records);
        Spinner noDays = findViewById(R.id.noDays);
        noDays.setOnItemSelectedListener(this);
        recordsList = findViewById(R.id.recordsList);
        ArrayList<Integer> options = new ArrayList<>();
        for (int i=0; i<100; i++)
            options.add(i);
        ArrayAdapter<Integer> arrayAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, options);
        arrayAdapter.setDropDownViewResource(R.layout.spinner_layout);
        noDays.setAdapter(arrayAdapter);
        databaseAccess = DatabaseAccess.getInstance(getApplicationContext());
    }

    protected void onResume() {
        super.onResume();
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

    public void syncData(View v) throws JSONException {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        databaseAccess.open();
        databaseAccess.sync(this, new DatabaseAccess.onSyncCompleteListener() {
            @Override
            public void showMessage(boolean isError, String message) {
                if (!isError){
                    showRecords();
                }
                alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }

                });
                alertDialogBuilder.setMessage(message);
                final AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.setOnShowListener( new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface arg0) {
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
                    }
                });
                if(!isFinishing())
                alertDialog.show();
            }
        });
    }

    private void showRecords(){
        databaseAccess.open();
        records = databaseAccess.getAllRecords(this,false);
        ArrayAdapter<String> recordsAdapter = new ArrayAdapter<String>(this, R.layout.text_view_cell_layout, R.id.listViewItem, getRecordData(records)){
            @Override
            public View getView(int position, View convertView, @NotNull ViewGroup parent){
                View view = super.getView(position, convertView, parent);
                TextView text = view.findViewById(R.id.listViewItem);
                if(records.get(position).getResultID().equals(""))
                    text.setTextColor(ContextCompat.getColor(getContext(),R.color.colorError));
                else
                    text.setTextColor(Color.WHITE);
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
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}