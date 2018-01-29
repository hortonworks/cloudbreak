package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.RecommendationRequestJson;
import com.sequenceiq.cloudbreak.api.model.RecommendationResponse;

public class RecommendationEntity extends AbstractCloudbreakEntity<RecommendationRequestJson, RecommendationResponse> {
    public static final String RECOMMENDATION = "RECOMMENDATION";

    RecommendationEntity(String newId) {
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
}
