package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.model.PlatformAccessConfigsResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.v3.AccessConfigV3Action;

public class AccessConfigEntity extends AbstractCloudbreakEntity<PlatformResourceRequestJson, PlatformAccessConfigsResponse, AccessConfigEntity,
        PlatformAccessConfigsResponse> {

    private static final String ACCESS_CONFIG_ID = "ACCESS_CONFIG";

    private AccessConfigEntity(String newId) {
        super(newId);
        setRequest(new PlatformResourceRequestJson());
    }

    protected AccessConfigEntity() {
        this(ACCESS_CONFIG_ID);
    }

    public static Function<IntegrationTestContext, AccessConfigEntity> getTestContextAccessConfig(String key) {
        return testContext -> testContext.getContextParam(key, AccessConfigEntity.class);
    }

    public static Function<IntegrationTestContext, AccessConfigEntity> getTestContextAccessConfig() {
        return getTestContextAccessConfig(ACCESS_CONFIG_ID);
    }

    public static Function<IntegrationTestContext, AccessConfigEntity> getNewAccessConfig() {
        return testContext -> new AccessConfigEntity();
    }

    public static AccessConfigEntity isGot() {
        AccessConfigEntity accessConfig = new AccessConfigEntity();
        accessConfig.setCreationStrategy(AccessConfigV3Action::createInGiven);
        return accessConfig;
    }
}
