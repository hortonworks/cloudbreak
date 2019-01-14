package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.filter;

import javax.ws.rs.QueryParam;

public class AuthCodeGrantFlowFilter {

    @QueryParam("code")
    private String code;

    @QueryParam("state")
    private String state;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

}