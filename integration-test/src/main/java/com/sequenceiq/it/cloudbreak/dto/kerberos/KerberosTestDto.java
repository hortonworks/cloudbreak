package com.sequenceiq.it.cloudbreak.dto.kerberos;

import com.sequenceiq.freeipa.api.v1.kerberos.model.create.CreateKerberosConfigRequest;
import com.sequenceiq.freeipa.api.v1.kerberos.model.describe.DescribeKerberosConfigResponse;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Prototype
public class KerberosTestDto extends AbstractFreeIpaTestDto<CreateKerberosConfigRequest, DescribeKerberosConfigResponse, KerberosTestDto> {

    public KerberosTestDto(TestContext testContext) {
        super(new CreateKerberosConfigRequest(), testContext);
    }

    @Override
    public void deleteForCleanup() {
        getClientForCleanup().getDefaultClient(getTestContext()).getKerberosConfigV1Endpoint().delete(getResponse().getEnvironmentCrn());
    }

    @Override
    public String getCrn() {
        return getResponse().getCrn();
    }

    @Override
    public int order() {
        return 500;
    }

    @Override
    public String getName() {
        return getRequest().getEnvironmentCrn();
    }

    @Override
    public KerberosTestDto valid() {
        String name = getResourcePropertyProvider().getName(getCloudPlatform());
        return withName(name).withEnvironment(EnvironmentTestDto.class);
    }

    public KerberosTestDto withRequest(CreateKerberosConfigRequest request) {
        setRequest(request);
        return this;
    }

    public KerberosTestDto withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public KerberosTestDto withEnvironmentCrn(String environmentCrn) {
        getRequest().setEnvironmentCrn(environmentCrn);
        return this;
    }

    public KerberosTestDto withEnvironment(Class<EnvironmentTestDto> environmentKey) {
        return withEnvironmentKey(environmentKey.getSimpleName());
    }

    public KerberosTestDto withEnvironmentKey(String environmentKey) {
        EnvironmentTestDto env = getTestContext().get(environmentKey);
        if (env == null) {
            throw new IllegalArgumentException("Env is null with given key: " + environmentKey);
        }
        return withEnvironmentCrn(env.getResponse().getCrn());
    }

    public KerberosTestDto withActiveDirectoryDescriptor() {
        getRequest().setActiveDirectory(getTestContext().given(ActiveDirectoryKerberosDescriptorTestDto.class).getRequest());
        return this;
    }

    public KerberosTestDto withFreeIpaDescriptor() {
        getRequest().setFreeIpa(getTestContext().get(FreeIpaKerberosDescriptorTestDto.class).getRequest());
        return this;
    }
}
