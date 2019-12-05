package com.sequenceiq.common.api.util.versionchecker;

import io.swagger.annotations.ApiModel;

@ApiModel
public class VersionCheckResult {
    private boolean versionCheckOk;

    private String message;

    public VersionCheckResult() {
    }

    public VersionCheckResult(boolean versionCheckOk) {
        this.versionCheckOk = versionCheckOk;
    }

    public VersionCheckResult(boolean versionCheckOk, String message) {
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

