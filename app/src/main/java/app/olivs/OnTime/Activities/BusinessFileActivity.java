package app.olivs.OnTime.Activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Map;

import app.olivs.OnTime.Model.BusinessFile;
import app.olivs.OnTime.R;
import app.olivs.OnTime.Utilities.UserManager;

import static app.olivs.OnTime.Utilities.Constants.GET_USER_BUSINESS_FILES_LIST;
import static app.olivs.OnTime.Utilities.Constants.getDefaultHeaders;

public class BusinessFileActivity extends AppCompatActivity implements Response.Listener<JSONArray>, Response.ErrorListener, ListView.OnItemClickListener{

    private ListView businessFileList;
    private ArrayList<BusinessFile> businessFileArray;
    private SwipeRefreshLayout swipeContainer;
    private Button okButton;
    private int selected = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //continueIfSelectedFile();
        setContentView(R.layout.activity_business_file);
        businessFileList = findViewById(R.id.recordsList);
        okButton = findViewById(R.id.okButton);
        swipeContainer = findViewById(R.id.swipeContainer);
        businessFileArray = new ArrayList<>();
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchNames();
            }
        });
        fetchNames();
    }

    private void fetchNames(){
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
        final JsonObjectToArrayRequest request = new JsonObjectToArrayRequest(Request.Method.POST, GET_USER_BUSINESS_FILES_LIST, body, this, this){
            @Override
            public Map<String, String> getHeaders() {
                return getDefaultHeaders(getApplicationContext());
            }
        };
        Volley.newRequestQueue(this).add(request);
    }

    private static class JsonObjectToArrayRequest extends JsonRequest<JSONArray> {

        public JsonObjectToArrayRequest(int method, String url, JSONObject jsonRequest,
                                      Response.Listener<JSONArray> listener, Response.ErrorListener errorListener) {
            super(method, url, (jsonRequest == null) ? null : jsonRequest.toString(), listener,
                    errorListener);
        }

        @Override
        protected Response<JSONArray> parseNetworkResponse(NetworkResponse response) {
            try {
                String jsonString = new String(response.data,
                        HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
                return Response.success(new JSONArray(jsonString),
                        HttpHeaderParser.parseCacheHeaders(response));
            } catch (UnsupportedEncodingException | JSONException e) {
                return Response.error(new ParseError(e));
            }
        }

    }


    @Override
    public void onResponse(@NotNull JSONArray response) {
        businessFileArray.clear();
        try {
            //String success = response.getString("dbtSuccess");
            //if (success.equals("Y")){
                for (int i=0; i<response.length(); i++){
                    JSONObject businessFile = response.getJSONObject(i);
                    String name = businessFile.getString("xxbBusinessName");
                    String token = businessFile.getString("xxbDBToken");
                    businessFileArray.add(new BusinessFile(name, token));
                }
                if (businessFileArray.isEmpty()) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                    alertDialogBuilder.setPositiveButton("Return", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            UserManager.getInstance().logout(getApplicationContext());
                            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    });
                    alertDialogBuilder.setMessage("There are no business files associated to this user.");
                    final AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.setOnShowListener( new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface arg0) {
                            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
                        }
                    });
                    alertDialog.show();
                }
                UserManager.getInstance().saveBusinessFiles(this,businessFileArray);
                showNames();
                okButton.setBackgroundResource(R.color.colorDisabled);
                okButton.setEnabled(false);
                swipeContainer.setRefreshing(false);
            //
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onErrorResponse(VolleyError error) {
        businessFileArray = UserManager.getInstance().getBusinessFilesOffline(this);
        showNames();
        swipeContainer.setRefreshing(false);
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
        selected = i;
        okButton.setBackgroundResource(R.drawable.button_shape);
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
        intent.addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("token",businessFileArray.get(selected).getToken());
        startActivity(intent);
    }

    private void continueIfSelectedFile(){
        if (UserManager.getInstance().fileSelected(this) >= 0) {
            String token = UserManager.getInstance().getParam(this,"businessFileToken");
            Intent intent = new Intent(this, CheckInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("token",token);
            startActivity(intent);
        }
    }
}