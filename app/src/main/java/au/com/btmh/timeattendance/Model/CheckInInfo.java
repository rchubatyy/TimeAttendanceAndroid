package au.com.btmh.timeattendance.Model;

import java.io.Serializable;

import au.com.btmh.timeattendance.Model.ActivityState;

public class CheckInInfo implements Serializable {

    private int id;
    private String userToken, dbToken, time, site, resultID;
    private Double lat, lon;
    private ActivityState state;
    private boolean isLiveData;


    public CheckInInfo(String time, ActivityState state){
        this.time = time;
        this.state = state;
    }

    public CheckInInfo(int id, String userToken, String dbToken, String time,  Double lat, Double lon, String site, ActivityState state, boolean isLiveData, String resultID) {
        this.id = id;
        this.userToken = userToken;
        this.dbToken = dbToken;
        this.time = time.replace('/', '-');
        this.site = site;
        this.lat = lat;
        this.lon = lon;
        this.state = state;
        this.isLiveData = isLiveData;
        this.resultID = resultID;
    }

    public int getId(){
        return id;
    }

    public String getUserToken() {
        return userToken;
    }

    public String getDbToken() {
        return dbToken;
    }

    public String getTime() {
        return time;
    }

    public String getSite() {
        return site;
    }

    public String getResultID() {
        return resultID;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLon() {
        return lon;
    }

    public ActivityState getState() {
        return state;
    }

    public boolean isLiveData() {
        return isLiveData;
    }
}