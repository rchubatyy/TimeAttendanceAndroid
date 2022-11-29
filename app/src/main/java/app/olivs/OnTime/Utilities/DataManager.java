package app.olivs.OnTime.Utilities;

import android.content.Context;
import android.content.SharedPreferences;

import org.jetbrains.annotations.NotNull;

import app.olivs.OnTime.Model.ActivityState;

import static android.content.Context.MODE_PRIVATE;

public class DataManager {
    private static DataManager instance = null;


    private DataManager() {
    }

    public static DataManager getInstance(){
        if (instance == null)
            instance = new DataManager();
        return instance;
    }


    private SharedPreferences getSharedPreferences(@NotNull Context context){
        return context.getSharedPreferences("prefs", MODE_PRIVATE);
    }

    public void setNotificationTime(Context context, String time){
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString("NotificationTime", time);
        editor.apply();
    }


    public String getNotificationTime(Context context){
        return getSharedPreferences(context).getString("NotificationTime", null);
    }

    public void setQuestionFor(Context context, int id, String question){
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString("question" + id, question);
        editor.apply();
    }

    public String getQuestionFor(Context context, int id){
        return getSharedPreferences(context).getString("question"+id, "");
    }

    public void setQuestionIdFor(Context context, ActivityState type, int id){
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putInt("qId" + type.name(), id);
        editor.apply();
    }

    public int getQuestionIdFor(Context context, ActivityState type){
        return getSharedPreferences(context).getInt("qId" + type.name(), -1);
    }

    public void setLastActivityType(Context context, ActivityState type){
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString("lastActivity", type.name());
        editor.apply();
    }

    public ActivityState getLastActivityType(Context context){
        return ActivityState.valueOf(getSharedPreferences(context).getString("lastActivity", "CHECKOUT"));
    }

    public void clearData(Context context){
        setNotificationTime(context, null);
        for (int i = 0; i < ActivityState.values().length; i++) {
            int id = getQuestionIdFor(context, ActivityState.values()[i]);
            if (id > 0)
                setQuestionFor(context, id, null);
            setQuestionIdFor(context, ActivityState.values()[i], 0);
        }
    }

}
