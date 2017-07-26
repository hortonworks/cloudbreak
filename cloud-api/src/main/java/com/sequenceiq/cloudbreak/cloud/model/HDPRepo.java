package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HDPRepo {
    public static final String REPO_ID_TAG = "repoid";

    public static final String MPACK_TAG = "mpack";

    private Map<String, String> stack;

    private Map<String, String> util;

    private Map<String, String> knox;

    private boolean verify = true;

    private String hdpVersion;

    public Map<String, String> getStack() {
        return stack;
    }

    public void setStack(Map<String, String> stack) {
        this.stack = stack;
    }

    public Map<String, String> getUtil() {
        return util;
    }

    public void setUtil(Map<String, String> util) {
        this.util = util;
    }

    public Map<String, String> getKnox() {
        return knox;
    }

    public void setKnox(Map<String, String> knox) {
        this.knox = knox;
    }

    public boolean isVerify() {
        return verify;
    }

    public void setVerify(boolean verify) {
        this.verify = verify;
    }

    public String getHdpVersion() {
        return hdpVersion;
    }

    public void setHdpVersion(String hdpVersion) {
        this.hdpVersion = hdpVersion;
    }

    @Override
    public String toString() {
        return "HDPRepo{stack='" + stack.get(REPO_ID_TAG) + "'; utils='" + util.get(REPO_ID_TAG) + "'}";
    }
}
