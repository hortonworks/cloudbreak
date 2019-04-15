package com.sequenceiq.it.cloudbreak.newway.dto;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.DetailedEnvironmentV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.SimpleEnvironmentV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.EnvironmentSettingsV4Request;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.credential.CredentialTestDto;

@Prototype
public class EnvironmentSettingsV4TestDto extends AbstractCloudbreakTestDto<EnvironmentSettingsV4Request, DetailedEnvironmentV4Response,
        EnvironmentSettingsV4TestDto> {

    public static final String ENVIRONMENT = "ENVIRONMENT";

    private Set<SimpleEnvironmentV4Response> response;

    private SimpleEnvironmentV4Response simpleResponse;

    public EnvironmentSettingsV4TestDto(TestContext testContext) {
        super(new EnvironmentSettingsV4Request(), testContext);
    }

    public EnvironmentSettingsV4TestDto() {
        super(ENVIRONMENT);
    }

    public EnvironmentSettingsV4TestDto(EnvironmentSettingsV4Request environmentV4Request, TestContext testContext) {
        super(environmentV4Request, testContext);
    }

    @Override
    public String getName() {
        return getRequest().getName();
    }

    @Override
    public EnvironmentSettingsV4TestDto valid() {
        CredentialTestDto credentialTestDto = getTestContext().get(CredentialTestDto.class);
        if (credentialTestDto == null) {
            throw new IllegalArgumentException("Credential is mandatory for EnvironmentSettings");
        }
        return withName(resourceProperyProvider().getName())
                .withCredentialName(credentialTestDto.getName());
    }

    public EnvironmentSettingsV4TestDto withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public EnvironmentSettingsV4TestDto withCredentialName(String name) {
        getRequest().setCredentialName(name);
        return this;
    }
}