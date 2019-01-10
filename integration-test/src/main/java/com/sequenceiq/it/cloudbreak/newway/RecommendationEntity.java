package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.RecommendationV4Request;
import com.sequenceiq.cloudbreak.api.model.RecommendationV4Response;

public class RecommendationEntity extends AbstractCloudbreakEntity<RecommendationV4Request, RecommendationV4Response, RecommendationEntity> {

    static final String RECOMMENDATION = "RECOMMENDATION";

    private RecommendationEntity(String newId) {
        super(newId);
        setRequest(new RecommendationV4Request());
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
