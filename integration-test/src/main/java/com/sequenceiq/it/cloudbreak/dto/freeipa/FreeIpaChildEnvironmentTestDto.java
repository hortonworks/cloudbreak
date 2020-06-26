package com.sequenceiq.it.cloudbreak.dto.freeipa;

import javax.inject.Inject;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.attachchildenv.AttachChildEnvironmentRequest;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Prototype
public class FreeIpaChildEnvironmentTestDto extends AbstractFreeIpaTestDto<AttachChildEnvironmentRequest, Void, FreeIpaChildEnvironmentTestDto> {

    public static final String CHILD_ENVIRONMENT_KEY = "childEnv";

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    public FreeIpaChildEnvironmentTestDto(TestContext testContext) {
        super(new AttachChildEnvironmentRequest(), testContext);
    }

    @Override
    public FreeIpaChildEnvironmentTestDto valid() {
        getRequest().setParentEnvironmentCrn(getParentEnvironmentCrn());
        getRequest().setChildEnvironmentCrn(getChildEnvironmentCrn());
        return this;
    }

    private String getChildEnvironmentCrn() {
        EnvironmentTestDto childEnvironment = getTestContext().get(CHILD_ENVIRONMENT_KEY);
        return childEnvironment.getResponse().getCrn();
    }

    private String getParentEnvironmentCrn() {
        return getTestContext().get(EnvironmentTestDto.class).getResponse().getCrn();
    }

}
