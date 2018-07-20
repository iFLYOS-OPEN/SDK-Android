package com.iflytek.cyber.resolver;

public class ActiveAlert {

    public String type;
    public String token;
    public String scheduledTime;

    public ActiveAlert() {

    }

    public ActiveAlert(String type, String token, String scheduledTime) {
        this.type = type;
        this.token = token;
        this.scheduledTime = scheduledTime;
    }
}
