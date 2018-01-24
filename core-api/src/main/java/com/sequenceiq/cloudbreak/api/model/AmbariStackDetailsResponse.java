package com.sequenceiq.cloudbreak.api.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AmbariStackDetailsResponse implements JsonEntity {

    public static final String REPO_ID_TAG = "repoid";

    private Map<String, String> stack;

    private Map<String, String> util;

    private Boolean enableGplRepo;

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

    public Boolean getEnableGplRepo() {
        return enableGplRepo;
    }

    public void setEnableGplRepo(Boolean enableGplRepo) {
        this.enableGplRepo = enableGplRepo;
    }

    @Override
    public String toString() {
        return "StackRepoDetails{stack='" + stack.get(REPO_ID_TAG) + "'; utils='" + util.get(REPO_ID_TAG) + "'}";
    }
}
