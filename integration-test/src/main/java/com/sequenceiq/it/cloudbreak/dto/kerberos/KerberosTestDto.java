package com.sequenceiq.it.cloudbreak.dto.kerberos;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import com.sequenceiq.freeipa.api.v1.kerberos.model.create.CreateKerberosConfigRequest;
import com.sequenceiq.freeipa.api.v1.kerberos.model.describe.DescribeKerberosConfigResponse;
import com.sequenceiq.it.cloudbreak.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.client.KerberosTestClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Prototype
public class KerberosTestDto extends AbstractFreeIpaTestDto<CreateKerberosConfigRequest, DescribeKerberosConfigResponse, KerberosTestDto> {

    public static final String DEFAULT_MASTERKEY = "masterkey";

    public static final String DEFAULT_ADMIN_USER = "admin";

    public static final String DEFAULT_ADMIN_PASSWORD = "password";

    @Inject
    private KerberosTestClient kerberosTestClient;

    public KerberosTestDto(TestContext testContext) {
        super(new CreateKerberosConfigRequest(), testContext);
    }

    @Override
    public void cleanUp(TestContext context, MicroserviceClient client) {
        LOGGER.info("Cleaning up kerberos config with name: {}", getName());
        try {
            when(kerberosTestClient.deleteV1(), key("delete-kerberos-" + getName()).withSkipOnFail(false));
        } catch (WebApplicationException ignore) {
            LOGGER.warn("Something went wrong during {} kerberos config delete, because of: {}", getName(), ignore.getMessage(), ignore);
        }
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

    public KerberosTestDto withActiveDirectoryDescriptor(ActiveDirectoryKerberosDescriptorTestDto activeDirectoryDescriptor) {
        getRequest().setActiveDirectory(activeDirectoryDescriptor.getRequest());
        return this;
    }

    public KerberosTestDto withFreeIpaDescriptor() {
        getRequest().setFreeIpa(getTestContext().get(FreeIpaKerberosDescriptorTestDto.class).getRequest());
        return this;
    }

    public KerberosTestDto withFreeIpaDescriptor(FreeIpaKerberosDescriptorTestDto freeIpaDescriptor) {
        getRequest().setFreeIpa(freeIpaDescriptor.getRequest());
        return this;
    }
}
