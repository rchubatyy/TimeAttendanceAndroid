package app.olivs.OnTime.Utilities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import app.olivs.OnTime.Model.BusinessFile;
import app.olivs.OnTime.Receivers.CheckOutNotifier;

import static android.content.Context.MODE_PRIVATE;

public class UserManager {
    private static UserManager instance = null;


    private UserManager() {
    }

    public static UserManager getInstance(){
        if (instance == null)
            instance = new UserManager();
        return instance;
    }


    private SharedPreferences getSharedPreferences(@NotNull Context context){
        return context.getSharedPreferences("prefs", MODE_PRIVATE);
    }

    public synchronized void login(Context context, String name, String email, String userToken){
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putBoolean("loggedIn", true);
        editor.putString("name", name);
        editor.putString("email", email);
        editor.putString("userToken", userToken);
        editor.apply();
    }

    public void logout(Context context){
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putBoolean("loggedIn", false);
        editor.putString("name", "");
        editor.putString("email", "");
        editor.putString("userToken", "");
        editor.apply();
        removedBusinessFile(context);
        DataManager.getInstance().clearData(context);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, CheckOutNotifier.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,0,intent,0);
        am.cancel(pendingIntent);
    }

    public synchronized void saveBusinessFiles(Context context, @NotNull ArrayList<BusinessFile> files){
        Set<String> names = new LinkedHashSet<>();
        Set<String> tokens = new LinkedHashSet<>();
        for (BusinessFile file: files){
            names.add(file.getName());
            tokens.add(file.getToken());
        }
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putStringSet("businessFileNames", names);
        editor.putStringSet("businessFileTokens", tokens);
        editor.apply();
    }

    public ArrayList<BusinessFile> getBusinessFilesOffline(Context context){
        ArrayList<BusinessFile> files = new ArrayList<>();

        Set<String> names = getSharedPreferences(context).getStringSet("businessFileNames", new LinkedHashSet<String>());
        Set<String> tokens = getSharedPreferences(context).getStringSet("businessFileTokens", new LinkedHashSet<String>());
        assert names != null;
        assert tokens!= null;
        ArrayList<String> namesList = new ArrayList<>(names);
        ArrayList<String> tokensList = new ArrayList<>(tokens);
        for (int i=0; i<namesList.size(); i++)
            files.add(new BusinessFile(namesList.get(i),tokensList.get(i)));
        return files;
    }

    public boolean isLoggedIn(Context context){
        return getSharedPreferences(context).getBoolean("loggedIn", false);
    }

    public int fileSelected(Context context){
        return getSharedPreferences(context).getInt("businessFileSelected", -1);
    }


    public void saveSelectedBusinessFile(Context context, int index, @NotNull BusinessFile file){
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putInt("businessFileSelected", index);
        editor.putString("businessFileName", file.getName());
        editor.putString("businessFileToken", file.getToken());
        editor.apply();
    }

    public void removedBusinessFile(Context context){
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putInt("businessFileSelected", -1);
        editor.putString("businessFileName", "");
        editor.putString("businessFileToken", "");
        editor.apply();
    }

    public String getParam(Context context, String param){
        return getSharedPreferences(context).getString(param, "");
    }

}