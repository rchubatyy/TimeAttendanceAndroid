package app.olivs.OnTime.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import app.olivs.OnTime.Model.BusinessFile;
import app.olivs.OnTime.R;
import app.olivs.OnTime.Utilities.UserManager;

import static app.olivs.OnTime.Utilities.Constants.GET_USER_BUSINESS_FILES_LIST;

public class BusinessFileActivity extends AppCompatActivity implements Response.Listener<JSONObject>, Response.ErrorListener, ListView.OnItemClickListener{

    private ListView businessFileList;
    private ArrayList<BusinessFile> businessFileArray;
    private Button okButton;
    private int selected = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        continueIfSelectedFile();
        setContentView(R.layout.activity_business_file);
        businessFileList = findViewById(R.id.recordsList);
        okButton = findViewById(R.id.okButton);
        JSONObject body = new JSONObject();
        Intent intent = getIntent();
        String token = intent.getStringExtra("userToken");
        if (token == null)
            token = UserManager.getInstance().getParam(this, "userToken");
        try {
            body.put("UserToken", token);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, GET_USER_BUSINESS_FILES_LIST, body, this, this);
        Volley.newRequestQueue(this).add(request);
    }

    protected void onResume() {
        super.onResume();
        continueIfSelectedFile();
    }

    @Override
    public void onResponse(@NotNull JSONObject response) {
        try {
            String success = response.getString("dbtSuccess");
            if (success.equals("Y")){
                businessFileArray = new ArrayList<>();
                JSONArray businessFiles = response.getJSONArray("dbtBusinessFiles");
                for (int i=0; i<businessFiles.length(); i++){
                    String name = businessFiles.getJSONObject(i).getString("dbtBusinessName");
                    String token = businessFiles.getJSONObject(i).getString("dbtDBToken");
                    businessFileArray.add(new BusinessFile(name, token));
                }
                UserManager.getInstance().saveBusinessFiles(this,businessFileArray);
                showNames();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onErrorResponse(VolleyError error) {
        businessFileArray = UserManager.getInstance().getBusinessFilesOffline(this);
        showNames();
    }

    @NotNull
    private ArrayList<String> listOfNames (@NotNull ArrayList<BusinessFile> fileList){
        ArrayList<String> names = new ArrayList<>();
        for (BusinessFile file: fileList)
            names.add(file.getName());
        return names;
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, @NotNull View view, int i, long l) {
        view.setSelected(true);
        System.out.println(i);
        selected = i;
        okButton.setBackgroundTintList(ContextCompat.getColorStateList(this,R.color.colorButton));
        okButton.setEnabled(true);
    }

    private void showNames(){
        ArrayAdapter<String> businessFileAdapter = new ArrayAdapter<>(this, R.layout.text_view_cell_layout, R.id.listViewItem, listOfNames(businessFileArray));
        businessFileList.setAdapter(businessFileAdapter);
        businessFileList.setOnItemClickListener(this);
    }

    public void toMainScreen(View v){
        UserManager.getInstance().saveSelectedBusinessFile(this, selected, businessFileArray.get(selected));
        Intent intent = new Intent(this,CheckInActivity.class);
        intent.putExtra("token",businessFileArray.get(selected).getToken());
        startActivity(intent);
    }

    private void continueIfSelectedFile(){
        if (UserManager.getInstance().fileSelected(this) >= 0) {
            String token = UserManager.getInstance().getParam(this,"businessFileToken");
            Intent intent = new Intent(this, CheckInActivity.class);
            intent.putExtra("token",token);
            startActivity(intent);
        }
    }

}