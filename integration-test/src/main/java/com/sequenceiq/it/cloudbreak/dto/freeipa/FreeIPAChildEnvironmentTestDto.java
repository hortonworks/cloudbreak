package com.sequenceiq.it.cloudbreak.dto.freeipa;

import javax.inject.Inject;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.attachchildenv.AttachChildEnvironmentRequest;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.client.FreeIPATestClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIPATestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Prototype
public class FreeIPAChildEnvironmentTestDto extends AbstractFreeIPATestDto<AttachChildEnvironmentRequest, Void, FreeIPAChildEnvironmentTestDto> {

    public static final String CHILD_ENVIRONMENT_KEY = "childEnv";

    @Inject
    private FreeIPATestClient freeIPATestClient;

    public FreeIPAChildEnvironmentTestDto(TestContext testContext) {
        super(new AttachChildEnvironmentRequest(), testContext);
    }

    @Override
    public FreeIPAChildEnvironmentTestDto valid() {
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
