package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

public class CloudCredential extends DynamicModel {

    public static final String GOV_CLOUD = "govCloud";

    private final String id;

    private final String name;

    public CloudCredential(String id, String name) {
        this(id, name, new HashMap<>());
    }

    public CloudCredential(String id, String name, Map<String, Object> parameters) {
        super(parameters);
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }
}
