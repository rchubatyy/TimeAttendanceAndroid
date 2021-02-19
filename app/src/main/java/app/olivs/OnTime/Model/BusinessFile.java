package app.olivs.OnTime.Model;

public class BusinessFile {


    private final String name;
    private final String token;

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
