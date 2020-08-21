package au.com.btmh.timeattendance;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseAccess {
    private SQLiteOpenHelper openHelper;
    private SQLiteDatabase database;
    private static DatabaseAccess instance;

    //private String userToken, dbToken;


    private DatabaseAccess(Context context) {
        this.openHelper = new DatabaseOpenHelper(context);
        //this.userToken = UserManager.getInstance().getParam(context,"userToken");
        //this.dbToken = UserManager.getInstance().getParam(context,"dbToken"));
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
        database.insert("tblRecords", null, values);

    }

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
            System.out.println("" + cursor.getDouble(6)+ " " + cursor.getString(9));
            list.add(record);
            cursor.moveToNext();
        }
        cursor.close();
        return list;
    }

    public void clearRecords(Context context, String days, onClearCompleteListener listener){
        database.delete("tblRecords",checkTokensQuery(context) + "AND RecordTime < DATETIME('now', '-" + days + " days', 'localtime')"
                ,null);
        listener.reloadData();
    }

    public interface onClearCompleteListener{
        void reloadData();
    }

    private String checkTokensQuery(Context context){
        return " usrToken = '" + UserManager.getInstance().getParam(context,"userToken") + "' " +
                "AND dbToken = '" + UserManager.getInstance().getParam(context,"businessFileToken") + "' ";
    }


}
