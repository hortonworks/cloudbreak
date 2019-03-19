package com.sequenceiq.it.cloudbreak.newway.dto;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformAccessConfigsV4Response;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.PlatformResourceParameters;
import com.sequenceiq.it.cloudbreak.newway.v4.AccessConfigV4Action;

public class AccessConfigTestDto extends AbstractCloudbreakTestDto<PlatformResourceParameters, PlatformAccessConfigsV4Response, AccessConfigTestDto> {

    private static final String ACCESS_CONFIG_ID = "ACCESS_CONFIG";

    private AccessConfigTestDto(String newId) {
        super(newId);
        setRequest(new PlatformResourceParameters());
    }

    protected AccessConfigTestDto() {
        this(ACCESS_CONFIG_ID);
    }

    public static Function<IntegrationTestContext, AccessConfigTestDto> getTestContextAccessConfig(String key) {
        return testContext -> testContext.getContextParam(key, AccessConfigTestDto.class);
    }

    public static Function<IntegrationTestContext, AccessConfigTestDto> getTestContextAccessConfig() {
        return getTestContextAccessConfig(ACCESS_CONFIG_ID);
    }

    public static Function<IntegrationTestContext, AccessConfigTestDto> getNewAccessConfig() {
        return testContext -> new AccessConfigTestDto();
    }

    public static AccessConfigTestDto isGot() {
        AccessConfigTestDto accessConfig = new AccessConfigTestDto();
        accessConfig.setCreationStrategy(AccessConfigV4Action::createInGiven);
        return accessConfig;
    }
}
