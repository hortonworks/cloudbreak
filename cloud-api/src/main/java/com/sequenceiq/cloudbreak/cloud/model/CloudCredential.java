package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

public class CloudCredential extends DynamicModel {

    public static final String GOV_CLOUD = "govCloud";

    private final Long id;

    private final String name;

    public CloudCredential(Long id, String name) {
        this(id, name, new HashMap<>());
    }

    public CloudCredential(Long id, String name, Map<String, Object> parameters) {
        super(parameters);
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
