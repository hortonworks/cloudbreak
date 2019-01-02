package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.RegionV4Response;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.v4.RegionV4Action;

public class Region extends Entity {
    public static final String REGION = "REGION";

    private PlatformResourceParameters platformResourceRequest;

    private RegionV4Response regionV4Response;

    public Region(String id) {
        super(id);
        platformResourceRequest = new PlatformResourceParameters();
    }

    public Region() {
        this(REGION);
    }

    public void setPlatformResourceRequest(PlatformResourceParameters platformResourceRequest) {
        this.platformResourceRequest = platformResourceRequest;
    }

    public RegionV4Response getRegionV4Response() {
        return regionV4Response;
    }

    public Region withCredential(String credentialName) {
        platformResourceRequest.setCredentialName(credentialName);
        return this;
    }

    static Function<IntegrationTestContext, Region> getTestContextRegion(String key) {
        return testContext -> testContext.getContextParam(key, Region.class);
    }

    public static Function<IntegrationTestContext, Region> getTestContextRegion() {
        return getTestContextRegion(REGION);
    }

    static Function<IntegrationTestContext, Region> getNew() {
        return testContext -> new Region();
    }

    public static Region request() {
        return new Region();
    }

    public static Action<Region> getPlatformRegions(String key) {
        return new Action<>(getTestContextRegion(key), RegionV4Action::getRegionsByCredentialId);
    }

    public static Action<Region> getPlatformRegionsWithRetry(int retryQuantity) {
        return getPlatformRegionsWithRetry(REGION, retryQuantity);
    }

    public static Action<Region> getPlatformRegionsWithRetry(String key, int retryQuantity) {
        return new Action<>(getTestContextRegion(key), (testContext, entity) -> RegionV4Action.getRegionsByCredentialId(testContext, entity, retryQuantity));
    }

    public static Action<Region> getPlatformRegions() {
        return getPlatformRegions(REGION);
    }

    public static Assertion<Region> assertThis(BiConsumer<Region, IntegrationTestContext> check) {
        return new Assertion<>(getTestContextRegion(GherkinTest.RESULT), check);
    }

    public PlatformResourceParameters getPlatformResourceRequest() {
        return platformResourceRequest;
    }

    public void setRegionV4Response(RegionV4Response regionV4Response) {
        this.regionV4Response = regionV4Response;
    }
}
