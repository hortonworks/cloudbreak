package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.RecommendationRequestJson;
import com.sequenceiq.cloudbreak.api.model.RecommendationResponse;

public class RecommendationEntity extends AbstractCloudbreakEntity<RecommendationRequestJson, RecommendationResponse, RecommendationEntity> {

    static final String RECOMMENDATION = "RECOMMENDATION";

    private RecommendationEntity(String newId) {
        super(newId);
        setRequest(new RecommendationRequestJson());
    }

    RecommendationEntity() {
        this(RECOMMENDATION);
    }

    public RecommendationEntity withBlueprintId(Long id) {
        getRequest().setBlueprintId(id);
        return this;
    }

    public RecommendationEntity withBlueprintName(String name) {
        getRequest().setBlueprintName(name);
        return this;
    }

    public RecommendationEntity withCredentialId(Long id) {
        getRequest().setCredentialId(id);
        return this;
    }

    public RecommendationEntity withCredentialName(String name) {
        getRequest().setCredentialName(name);
        return this;
    }

    public RecommendationEntity withRegion(String regionName) {
        getRequest().setRegion(regionName);
        return this;
    }

    public RecommendationEntity withAvailabilityZone(String availabilityZone) {
        getRequest().setAvailabilityZone(availabilityZone);
        return this;
    }
}
