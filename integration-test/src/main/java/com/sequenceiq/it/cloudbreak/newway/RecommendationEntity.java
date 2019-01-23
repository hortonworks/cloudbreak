package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses.RecommendationV4Response;

public class RecommendationEntity extends AbstractCloudbreakEntity<Object, RecommendationV4Response, RecommendationEntity> {

    static final String RECOMMENDATION = "RECOMMENDATION";

    private String credentialName;

    private String region;

    private String availabilityZone;

    private String blueprintName;

    private RecommendationEntity(String newId) {
        super(newId);
    }

    RecommendationEntity() {
        this(RECOMMENDATION);
    }

    public RecommendationEntity withBlueprintName(String name) {
        this.blueprintName = name;
        return this;
    }

    public RecommendationEntity withCredentialName(String name) {
        this.credentialName = name;
        return this;
    }

    public RecommendationEntity withRegion(String regionName) {
        this.region = regionName;
        return this;
    }

    public RecommendationEntity withAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
        return this;
    }

    public String getCredentialName() {
        return credentialName;
    }

    public String getRegion() {
        return region;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public String getBlueprintName() {
        return blueprintName;
    }
}
