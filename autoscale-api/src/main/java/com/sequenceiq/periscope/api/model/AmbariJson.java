package com.sequenceiq.periscope.api.model;

import com.sequenceiq.periscope.doc.ApiDescription.AmbariJsonProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("AmbariJson")
public class AmbariJson implements Json {

    @ApiModelProperty(AmbariJsonProperties.HOST)
    private String host;
    @ApiModelProperty(AmbariJsonProperties.PORT)
    private String port;
    @ApiModelProperty(AmbariJsonProperties.USERNAME)
    private String user;
    @ApiModelProperty(AmbariJsonProperties.PASSWORD)
    private String pass;

    public AmbariJson() {
    }

    public AmbariJson(String host, String port, String user, String pass) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.pass = pass;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }
}
