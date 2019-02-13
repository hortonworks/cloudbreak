package com.sequenceiq.cloudbreak.cloud.model.component;

public class RepositoryDetails {

    private String baseurl;

    private String gpgkey;

    public String getBaseurl() {
        return baseurl;
    }

    public void setBaseurl(String baseurl) {
        this.baseurl = baseurl;
    }

    public String getGpgkey() {
        return gpgkey;
    }

    public void setGpgkey(String gpgkey) {
        this.gpgkey = gpgkey;
    }
}
