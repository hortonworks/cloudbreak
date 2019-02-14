package com.sequenceiq.cloudbreak.cloud.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @deprecated will be replaced by {@link com.sequenceiq.cloudbreak.template.views.RdsView}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Deprecated
public class AmbariDatabase {

    private String vendor;

    private String fancyName;

    private String name;

    private String host;

    private Integer port;

    private String userName;

    private String password;

    public AmbariDatabase() {

    }

    public AmbariDatabase(String vendor, String fancyName, String name, String host, Integer port, String userName, String password) {
        this.vendor = vendor;
        this.fancyName = fancyName;
        this.name = name;
        this.host = host;
        this.port = port;
        this.userName = userName;
        this.password = password;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getFancyName() {
        return fancyName;
    }

    public void setFancyName(String fancyName) {
        this.fancyName = fancyName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
