package com.sequenceiq.it.cloudbreak.dto.freeipa;

import java.util.List;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate.FreeIpaSecretRotationRequest;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Prototype
public class FreeIpaRotationTestDto extends AbstractFreeIpaTestDto<FreeIpaSecretRotationRequest, FlowIdentifier, FreeIpaRotationTestDto>
        implements EnvironmentAware {

    private String environmentCrn;

    protected FreeIpaRotationTestDto(TestContext testContext) {
        super(new FreeIpaSecretRotationRequest(), testContext);
    }

    @Override
    public CloudbreakTestDto valid() {
        return withEnvironmentCrn(getTestContext().given(EnvironmentTestDto.class).getCrn());
    }

    private FreeIpaRotationTestDto withEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
        return this;
    }

    public FreeIpaRotationTestDto withSecrets(List<FreeIpaSecretType> secretTypes) {
        getRequest().setSecrets(secretTypes.stream().map(Enum::name).collect(Collectors.toList()));
        return this;
    }

    public FreeIpaRotationTestDto withExecutionType(RotationFlowExecutionType executionType) {
        getRequest().setExecutionType(executionType);
        return this;
    }

    @Override
    public String getCrn() {
        return getEnvironmentCrn();
    }

    @Override
    public String getEnvironmentCrn() {
        return environmentCrn;
    }

}