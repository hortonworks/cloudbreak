package com.sequenceiq.cloudbreak.cloud.model.component;

import java.io.Serializable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StackRepoDetails implements Serializable {

    public static final String REPO_ID_TAG = "repoid";

    public static final String MPACK_TAG = "mpack";

    public static final String REPOSITORY_VERSION = "repository-version";

    public static final String VDF_REPO_KEY_PREFIX = "vdf-";

    public static final String CUSTOM_VDF_REPO_KEY = "vdf-url";

    private static final Pattern MAJOR_VERSION_REGEX_PATTERN = Pattern.compile("(^[0-9]+\\.[0-9]+).*");

    private Map<String, String> stack;

    private Map<String, String> util;

    private boolean enableGplRepo;

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

    public boolean isVerify() {
        return verify;
    }

    public void setVerify(boolean verify) {
        this.verify = verify;
    }

    public String getHdpVersion() {
        return hdpVersion;
    }

    @JsonIgnore
    public String getMajorHdpVersion() {
        String result = hdpVersion;
        if (result == null) {
            result = "";
        }
        Matcher majorVersionRegex = MAJOR_VERSION_REGEX_PATTERN.matcher(result);
        if (majorVersionRegex.matches()) {
            result = majorVersionRegex.group(1);
        }
        return result;
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

    @Override
    public String toString() {
        return "StackRepoDetails{stack='" + stack.get(REPO_ID_TAG) + "'; utils='" + util.get(REPO_ID_TAG) + "'}";
    }
}
