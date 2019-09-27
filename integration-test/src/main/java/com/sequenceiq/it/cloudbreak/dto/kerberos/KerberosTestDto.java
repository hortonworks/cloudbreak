package com.sequenceiq.it.cloudbreak.dto.kerberos;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import com.sequenceiq.freeipa.api.v1.kerberos.model.create.CreateKerberosConfigRequest;
import com.sequenceiq.freeipa.api.v1.kerberos.model.describe.DescribeKerberosConfigResponse;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.client.KerberosTestClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIPATestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Prototype
public class KerberosTestDto extends AbstractFreeIPATestDto<CreateKerberosConfigRequest, DescribeKerberosConfigResponse, KerberosTestDto> {

    public static final String DEFAULT_MASTERKEY = "masterkey";

    public static final String DEFAULT_ADMIN_USER = "admin";

    public static final String DEFAULT_ADMIN_PASSWORD = "password";

    @Inject
    private KerberosTestClient kerberosTestClient;

    public KerberosTestDto(TestContext testContext) {
        super(new CreateKerberosConfigRequest(), testContext);
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up kerberos config with name: {}", getName());
        try {
            when(kerberosTestClient.deleteV1());
        } catch (WebApplicationException ignore) {
            LOGGER.info("Something happend during the kerberos resource delete operation.");
        }
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
        String name = getResourcePropertyProvider().getName();
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
        getRequest().setActiveDirectory(getTestContext().get(ActiveDirectoryKerberosDescriptorTestDto.class).getRequest());
        return this;
    }

    public KerberosTestDto withActiveDirectoryDescriptor(ActiveDirectoryKerberosDescriptorTestDto activeDirectoryDescriptor) {
        getRequest().setActiveDirectory(activeDirectoryDescriptor.getRequest());
        return this;
    }

    public KerberosTestDto withFreeIPADescriptor() {
        getRequest().setFreeIpa(getTestContext().get(FreeIPAKerberosDescriptorTestDto.class).getRequest());
        return this;
    }

    public KerberosTestDto withFreeIPADescriptor(FreeIPAKerberosDescriptorTestDto freeIPADescriptor) {
        getRequest().setFreeIpa(freeIPADescriptor.getRequest());
        return this;
    }
}
