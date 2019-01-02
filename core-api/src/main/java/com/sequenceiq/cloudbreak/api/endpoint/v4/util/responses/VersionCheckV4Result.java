package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import io.swagger.annotations.ApiModel;

@ApiModel
public class VersionCheckV4Result {
    private boolean versionCheckOk;

    private String message;

    public VersionCheckV4Result() {
    }

    public VersionCheckV4Result(boolean versionCheckOk) {
        this.versionCheckOk = versionCheckOk;
    }

    public VersionCheckV4Result(boolean versionCheckOk, String message) {
        this.versionCheckOk = versionCheckOk;
        this.message = message;
    }

    public boolean isVersionCheckOk() {
        return versionCheckOk;
    }

    public void setVersionCheckOk(boolean versionCheckOk) {
        this.versionCheckOk = versionCheckOk;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

