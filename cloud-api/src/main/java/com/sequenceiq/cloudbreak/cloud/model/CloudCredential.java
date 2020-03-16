package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

public class CloudCredential extends DynamicModel {

    public static final String GOV_CLOUD = "govCloud";

    private String id;

    private String name;

    private boolean verifyPermissions;

    public CloudCredential() {
    }

    public CloudCredential(String id, String name) {
        this(id, name, new HashMap<>(), false);
    }

    public CloudCredential(String id, String name, boolean verifyPermissions) {
        this(id, name, new HashMap<>(), verifyPermissions);
    }

    public CloudCredential(String id, String name, Map<String, Object> parameters, boolean verifyPermissions) {
        super(parameters);
        this.id = id;
        this.name = name;
        this.verifyPermissions = verifyPermissions;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isVerifyPermissions() {
        return verifyPermissions;
    }

    public void setVerifyPermissions(boolean verifyPermissions) {
        this.verifyPermissions = verifyPermissions;
    }

}
