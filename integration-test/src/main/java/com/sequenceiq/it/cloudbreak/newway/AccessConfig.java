package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.model.PlatformAccessConfigsResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.v3.AccessConfigV3Action;

public class AccessConfig extends AbstractCloudbreakEntity<PlatformResourceRequestJson, PlatformAccessConfigsResponse> {

    private static final String ACCESS_CONFIG_ID = "ACCESS_CONFIG";

    private AccessConfig(String newId) {
        super(newId);
        setRequest(new PlatformResourceRequestJson());
    }

    protected AccessConfig() {
        this(ACCESS_CONFIG_ID);
    }

    public static Function<IntegrationTestContext, AccessConfig> getTestContextAccessConfig(String key) {
        return testContext -> testContext.getContextParam(key, AccessConfig.class);
    }

    public static Function<IntegrationTestContext, AccessConfig> getTestContextAccessConfig() {
        return getTestContextAccessConfig(ACCESS_CONFIG_ID);
    }

    public static Function<IntegrationTestContext, AccessConfig> getNewAccessConfig() {
        return testContext -> new AccessConfig();
    }

    public static AccessConfig isGot() {
        AccessConfig accessConfig = new AccessConfig();
        accessConfig.setCreationStrategy(AccessConfigV3Action::createInGiven);
        return accessConfig;
    }
}
