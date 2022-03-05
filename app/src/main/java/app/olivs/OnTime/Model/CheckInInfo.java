package app.olivs.OnTime.Model;

import java.io.Serializable;

public class CheckInInfo implements Serializable {

    private int id;
    private String userToken;
    private String dbToken;
    private final String time;
    private String site;
    private String resultID;
    private Double lat, lon;
    private final ActivityState state;
    private boolean isLiveData;
    private int questionId;
    private String answer;


    public CheckInInfo(String time, ActivityState state){
        this.time = time;
        this.state = state;
    }

    public CheckInInfo(String userToken, String dbToken, String time, ActivityState state, boolean isLiveData, int questionId, String answer){
        this.userToken = userToken;
        this.dbToken = dbToken;
        this.time = time.replace('/', '-');
        this.state = state;
        this.isLiveData = isLiveData;
        this.questionId = questionId;
        this.answer = answer;
    }

    public CheckInInfo(int id, String userToken, String dbToken, String time,  Double lat, Double lon, String site, ActivityState state, boolean isLiveData, String resultID, int questionId, String answer) {
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
        this.questionId = questionId;
        this.answer = answer;
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

    public int getQuestionId() {
        return questionId;
    }

    public String getAnswer() {
        return answer;
    }

    public void setLocation(Double lat, Double lon){
        this.lat = lat;
        this.lon = lon;
    }

    public void setResult(String site, String resultID){
        this.site = site;
        this.resultID = resultID;
    }
}
