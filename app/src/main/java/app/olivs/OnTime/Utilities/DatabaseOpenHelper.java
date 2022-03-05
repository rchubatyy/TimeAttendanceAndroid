package app.olivs.OnTime.Utilities;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseOpenHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "records.db";
    private static final int DB_VERSION = 2;

    public DatabaseOpenHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS tblRecords (" +
                "        id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "        usrToken VARCHAR(32)," +
                "        dbToken VARCHAR(32)," +
                "        RecordTime DATETIME," +
                "        GPSlat DECIMAL (9,6)," +
                "        GPSlon DECIMAL (9,6)," +
                "        Site TEXT," +
                "        Type TEXT," +
                "        isLiveData CHAR," +
                "        resultID TEXT," +
                "        questionID INTEGER," +
                "        Answer VARCHAR(1)" +
                "        );");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1) {
            db.execSQL("ALTER TABLE tblRecords ADD COLUMN questionID INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE tblRecords ADD COLUMN Answer VARCHAR(1) DEFAULT 'X'");
        }
    }
}
