package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.BlueprintRequest;
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;

public class BlueprintEntity extends AbstractCloudbreakEntity<BlueprintRequest, BlueprintResponse> {
    public static final String BLUEPRINT = "BLUEPRINT";

    BlueprintEntity(String newId) {
        super(newId);
        setRequest(new BlueprintRequest());
    }

    BlueprintEntity() {
        this(BLUEPRINT);
    }

    public BlueprintEntity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public BlueprintEntity withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    public BlueprintEntity withUrl(String url) {
        getRequest().setUrl(url);
        return this;
    }

    public BlueprintEntity withAmbariBlueprint(String blueprint) {
        getRequest().setAmbariBlueprint(blueprint);
        return this;
    }
}
