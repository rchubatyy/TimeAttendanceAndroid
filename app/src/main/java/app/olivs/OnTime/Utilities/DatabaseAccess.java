package app.olivs.OnTime.Utilities;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import app.olivs.OnTime.Model.ActivityState;
import app.olivs.OnTime.Model.CheckInInfo;

import static app.olivs.OnTime.Utilities.Constants.REGISTER_USER_ACTIVITY;
import static app.olivs.OnTime.Utilities.Constants.getDefaultHeaders;
import static app.olivs.OnTime.Utilities.Constants.identifierForVendor;

public class DatabaseAccess {
    private final SQLiteOpenHelper openHelper;
    private SQLiteDatabase database;
    private static DatabaseAccess instance;


    private DatabaseAccess(Context context) {
        this.openHelper = new DatabaseOpenHelper(context);
    }


    public static DatabaseAccess getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseAccess(context);
        }
        return instance;
    }

    public void open() {
        this.database = openHelper.getWritableDatabase();
    }

    public void close() {
        if (database != null) {
            this.database.close();
        }
    }

    public void insertRecord(CheckInInfo record) {
        ContentValues values = new ContentValues();
        values.put("usrToken", record.getUserToken());
        values.put("dbToken", record.getDbToken());
        values.put("RecordTime", record.getTime());
        values.put("GPSLat", record.getLat());
        values.put("GPSLon", record.getLon());
        values.put("Site", record.getSite());
        values.put("Type", record.getState().name());
        values.put("isLiveData", record.isLiveData() ? "L" : "S");
        values.put("resultId", record.getResultID());
        values.put("questionId",record.getQuestionId());
        values.put("Answer",record.getAnswer());
        database.insert("tblRecords", null, values);

    }

    /**
     *
     * @param context
     * @param unsyncedOnly
     * @return
     */
    @Deprecated
    public List<String> getAllRecordTexts(Context context, boolean unsyncedOnly) {
        List<String> list = new ArrayList<>();
        Cursor cursor = database.rawQuery("SELECT RecordTime, Type FROM tblRecords " +
                "WHERE usrToken = '" + UserManager.getInstance().getParam(context,"userToken") + "' " +
                "AND dbToken = '" + UserManager.getInstance().getParam(context,"businessFileToken") + "' " +
                        (unsyncedOnly ? "AND resultID = ''" : "") +
        "ORDER BY RecordTime DESC", null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            String data = cursor.getString(0);
            data += " | ";
            data += ActivityState.valueOf(cursor.getString(1));
            list.add(data);
            cursor.moveToNext();
        }
        cursor.close();
        return list;
    }

    public List<CheckInInfo> getAllRecords(Context context, boolean unsyncedOnly) {
        List<CheckInInfo> list = new ArrayList<>();
        Cursor cursor = database.rawQuery("SELECT * FROM tblRecords WHERE" +
                checkTokensQuery(context) +
                (unsyncedOnly ? "AND resultID = ''" : "") +
                "ORDER BY RecordTime DESC", null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            CheckInInfo record = new CheckInInfo(cursor.getInt(0),cursor.getString(1),cursor.getString(2),cursor.getString(3),cursor.getDouble(4),cursor.getDouble(5),
                    cursor.getString(6),ActivityState.valueOf(cursor.getString(7)),cursor.getString(8).equals("L"), cursor.getString(9));
            list.add(record);
            cursor.moveToNext();
        }
        cursor.close();
        return list;
    }

    public void clearRecords(Context context, String days, onClearCompleteListener listener){
        database.delete("tblRecords",checkTokensQuery(context) + "AND RecordTime < DATETIME('now', '-" + days + " days', 'localtime') AND resultId != '' AND resultId IS NOT NULL"
                ,null);
        listener.reloadData();
    }

    public void sync (final Context context, final onSyncCompleteListener listener) throws JSONException {
        List<CheckInInfo> unsynced = new ArrayList<>();
        unsynced = getAllRecords(context,true);
        final boolean[] syncSuccess = {true};
        if (unsynced.isEmpty()){
            if (listener != null)
            listener.showMessage(false, "All records synced");
            return;
        }
        //final int[] unsyncedNo = {unsynced.size()};
        final CheckInInfo record = unsynced.get(unsynced.size()-1);
            JSONObject body = new JSONObject();
            body.put("UserToken", record.getUserToken());
            body.put("DBToken", record.getDbToken());
            body.put("ActivityType", record.getState().name());
            body.put("GPSLat",record.getLat());
            body.put("GPSLon",record.getLon());
            String time = record.getTime().replace('-','/');
            body.put("PhDateTime",time);
            body.put("isLiveDataOrSync", "S");
            body.put("OSVersion", "Android " + Build.VERSION.RELEASE);
            body.put("PhoneModel", Build.MANUFACTURER + " " + Build.MODEL);
            body.put("IdentifierForVendor", identifierForVendor(context));
            body.put("QuestionID",record.getQuestionId());
            body.put("Answer",record.getAnswer());
            final ServiceRequest request = new ServiceRequest(context, Request.Method.POST, REGISTER_USER_ACTIVITY, body, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    int id = record.getId();
                    ContentValues values = new ContentValues();
                    values.put("isLiveData", "S");
                    try {
                        values.put("resultID", response.getString("acdID"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    open();
                    database.update("tblRecords",values,"id = " +id, null);
                    try {
                        sync(context, listener);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    if (listener != null)
                    listener.showMessage(true, "Failed to sync");
                }
            });
            request.setRetryPolicy(new DefaultRetryPolicy(10000,
                    1,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            Volley.newRequestQueue(context).add(request);

    }

    public interface onClearCompleteListener{
        void reloadData();
    }

    public interface onSyncCompleteListener{
        void showMessage(boolean isError, String message);
    }

    private String checkTokensQuery(Context context){
        return " usrToken = '" + UserManager.getInstance().getParam(context,"userToken") + "' " +
                "AND dbToken = '" + UserManager.getInstance().getParam(context,"businessFileToken") + "' ";
    }


}
