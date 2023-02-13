package com.sequenceiq.it.cloudbreak.dto;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.RecommendationV4Response;
import com.sequenceiq.it.cloudbreak.context.TestContext;

public class RecommendationTestDto extends AbstractCloudbreakTestDto<Object, RecommendationV4Response, RecommendationTestDto> {

    private String credentialName;

    private String region;

    private String availabilityZone;

    private String blueprintName;

    protected RecommendationTestDto(Object request, TestContext testContext) {
        super(request, testContext);
    }

    public RecommendationTestDto withBlueprintName(String name) {
        this.blueprintName = name;
        return this;
    }

    public RecommendationTestDto withCredentialName(String name) {
        this.credentialName = name;
        return this;
    }

    public RecommendationTestDto withRegion(String regionName) {
        this.region = regionName;
        return this;
    }

    public RecommendationTestDto withAvailabilityZone(String availabilityZone) {
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
