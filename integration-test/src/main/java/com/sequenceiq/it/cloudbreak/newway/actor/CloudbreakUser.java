package com.sequenceiq.it.cloudbreak.newway.actor;

public class CloudbreakUser {

    private String username;

    private String password;

    public CloudbreakUser(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
