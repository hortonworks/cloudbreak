package com.sequenceiq.cloudbreak.controller.json;

public class LoginRequestJson {

    private String email;
    private String password;

    public LoginRequestJson() {

    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
