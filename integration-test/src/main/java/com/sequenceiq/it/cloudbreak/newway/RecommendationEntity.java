package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.filters.RecommendationV4Filter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses.RecommendationV4Response;

public class RecommendationEntity extends AbstractCloudbreakEntity<RecommendationV4Filter, RecommendationV4Response, RecommendationEntity> {

    static final String RECOMMENDATION = "RECOMMENDATION";

    private RecommendationEntity(String newId) {
        super(newId);
        setRequest(new RecommendationV4Filter());
    }

    RecommendationEntity() {
        this(RECOMMENDATION);
    }

    public RecommendationEntity withBlueprintName(String name) {
        getRequest().setBlueprintName(name);
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
