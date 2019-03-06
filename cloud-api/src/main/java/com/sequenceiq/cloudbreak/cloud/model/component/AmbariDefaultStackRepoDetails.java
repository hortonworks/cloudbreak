package com.sequenceiq.cloudbreak.cloud.model.component;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AmbariDefaultStackRepoDetails implements Serializable {

    public static final String REPO_ID_TAG = "repoid";

    private Map<String, String> stack;

    private Map<String, String> util;

    private boolean enableGplRepo;

    private boolean verify = true;

    private String hdpVersion;

    private Map<String, List<ManagementPackComponent>> mpacks = new HashMap<>();

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

    public boolean isEnableGplRepo() {
        return enableGplRepo;
    }

    public void setEnableGplRepo(boolean enableGplRepo) {
        this.enableGplRepo = enableGplRepo;
    }

    public Map<String, List<ManagementPackComponent>> getMpacks() {
        return mpacks;
    }

    public void setMpacks(Map<String, List<ManagementPackComponent>> mpacks) {
        this.mpacks = mpacks;
    }

    @Override
    public String toString() {
        return "StackRepoDetails{stack='" + stack.get(REPO_ID_TAG) + "'; utils='" + util.get(REPO_ID_TAG) + "'}";
    }
}
