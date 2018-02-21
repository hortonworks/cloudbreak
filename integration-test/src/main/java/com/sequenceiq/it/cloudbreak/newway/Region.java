package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.cloudbreak.api.model.RegionResponse;
import com.sequenceiq.it.IntegrationTestContext;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class Region extends Entity {
    public static final String REGION = "REGION";

    private PlatformResourceRequestJson platformResourceRequest;

    private RegionResponse regionResponse;

    public Region(String id) {
        super(id);
        platformResourceRequest = new PlatformResourceRequestJson();
    }

    public Region() {
        this(REGION);
    }

    public void setPlatformResourceRequest(PlatformResourceRequestJson platformResourceRequest) {
        this.platformResourceRequest = platformResourceRequest;
    }

    public RegionResponse getRegionResponse() {
        return regionResponse;
    }

    public Region withCredential(Long credentialId) {
        platformResourceRequest.setCredentialId(credentialId);
        return this;
    }

    static Function<IntegrationTestContext, Region> getTestContext(String key) {
        return (testContext) -> testContext.getContextParam(key, Region.class);
    }

    static Function<IntegrationTestContext, Region> getNew() {
        return (testContext) -> new Region();
    }

    public static Region request() {
        return new Region();
    }

    public static Action<Region> getPlatformRegions(String key) {
        return new Action<>(getTestContext(key), RegionAction::getRegionsByCredentialId);
    }

    public static Action<Region> getPlatformRegions() {
        return getPlatformRegions(REGION);
    }

    public static Assertion<Region> assertThis(BiConsumer<Region, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

    public PlatformResourceRequestJson getPlatformResourceRequest() {
        return platformResourceRequest;
    }

    public void setRegionResponse(RegionResponse regionResponse) {
        this.regionResponse = regionResponse;
    }
}
