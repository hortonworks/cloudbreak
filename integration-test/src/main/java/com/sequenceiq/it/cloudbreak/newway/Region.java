package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.PlatformRegionsJson;
import com.sequenceiq.it.IntegrationTestContext;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class Region extends Entity {
    public static final String REGION = "REGION";

    private PlatformRegionsJson platformRegionsResponse;

    private String type;

    private Collection<String> regionRResponse;

    private Map<String, Collection<String>> regionAvResponse;

    public Region(String id) {
        super(id);
    }

    public Region() {
        this(REGION);
    }

    public Collection<String> getRegionRResponse() {
        return regionRResponse;
    }

    public Map<String, Collection<String>> getRegionAvResponse() {
        return regionAvResponse;
    }

    public PlatformRegionsJson getPlatformRegionsResponse() {
        return platformRegionsResponse;
    }

    public void setPlatformRegionsResponse(PlatformRegionsJson platformRegionsResponse) {
        this.platformRegionsResponse = platformRegionsResponse;
    }

    public String getType() {
        return type;
    }

    public void withType(String type) {
        this.type = type;
    }

    public void setRegionRResponse(Collection<String> regionRResponse) {
        this.regionRResponse = regionRResponse;
    }

    public void setRegionAvResponse(Map<String, Collection<String>> regionAvResponse) {
        this.regionAvResponse = regionAvResponse;
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
        return new Action<>(getTestContext(key), RegionAction::getPlatformRegions);
    }

    public static Action<Region> getPlatformRegions() {
        return getPlatformRegions(REGION);
    }

    public static Action<Region> getRegionAvByType(String key) {
        return new Action<>(getTestContext(key), RegionAction::getRegionAvByType);
    }

    public static Action<Region> getRegionAvByType() {
        return getPlatformRegions(REGION);
    }

    public static Action<Region> getRegionRByType(String key) {
        return new Action<>(getTestContext(key), RegionAction::getRegionRByType);
    }

    public static Action<Region> getRegionRByType() {
        return getPlatformRegions(REGION);
    }

    public static Assertion<Region> assertThis(BiConsumer<Region, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }
}
