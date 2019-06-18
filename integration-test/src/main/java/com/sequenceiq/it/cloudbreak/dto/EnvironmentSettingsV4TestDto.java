package com.sequenceiq.it.cloudbreak.dto;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.EnvironmentSettingsV4Request;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;

@Prototype
public class EnvironmentSettingsV4TestDto extends AbstractCloudbreakTestDto<EnvironmentSettingsV4Request, DetailedEnvironmentResponse,
        EnvironmentSettingsV4TestDto> {

    public static final String ENVIRONMENT = "ENVIRONMENT";

    private Set<SimpleEnvironmentResponse> response;

    private SimpleEnvironmentResponse simpleResponse;

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
        return withName(resourceProperyProvider().getName());
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