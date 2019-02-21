package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformAccessConfigsV4Response;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.v4.AccessConfigV4Action;

public class AccessConfigEntity extends AbstractCloudbreakEntity<PlatformResourceParameters, PlatformAccessConfigsV4Response, AccessConfigEntity> {

    private static final String ACCESS_CONFIG_ID = "ACCESS_CONFIG";

    private AccessConfigEntity(String newId) {
        super(newId);
        setRequest(new PlatformResourceParameters());
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
        accessConfig.setCreationStrategy(AccessConfigV4Action::createInGiven);
        return accessConfig;
    }
}
