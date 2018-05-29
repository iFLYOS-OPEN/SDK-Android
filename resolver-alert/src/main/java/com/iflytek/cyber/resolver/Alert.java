package com.iflytek.cyber.resolver;

public class Alert {

    public String type;
    public String token;
    public String scheduledTime;

    public Alert(String type, String token, String scheduledTime) {
        this.type = type;
        this.token = token;
        this.scheduledTime = scheduledTime;
    }
}
