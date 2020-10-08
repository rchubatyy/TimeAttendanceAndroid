package app.olivs.OnTime.Model;

public class BusinessFile {


    private String name;
    private String token;

    public BusinessFile(String name, String token){
        this.name = name;
        this.token = token;
    }

    public String getName() {
        return name;
    }

    public String getToken() {
        return token;
    }
}
