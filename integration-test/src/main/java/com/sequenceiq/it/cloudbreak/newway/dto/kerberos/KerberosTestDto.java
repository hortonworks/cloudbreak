package com.sequenceiq.it.cloudbreak.newway.dto.kerberos;

import javax.ws.rs.WebApplicationException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.AbstractCloudbreakTestDto;

@Prototype
public class KerberosTestDto extends AbstractCloudbreakTestDto<KerberosV4Request, KerberosV4Response, KerberosTestDto> {

    public static final String DEFAULT_MASTERKEY = "masterkey";

    public static final String DEFAULT_ADMIN_USER = "admin";

    public static final String DEFAULT_ADMIN_PASSWORD = "password";

    public KerberosTestDto(TestContext testContext) {
        super(new KerberosV4Request(), testContext);
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        try {
            cloudbreakClient.getCloudbreakClient().kerberosConfigV4Endpoint().delete(cloudbreakClient.getWorkspaceId(), getName());
        } catch (WebApplicationException ignore) {
            LOGGER.info("Something happend during the kerberos resource delete operation.");
        }
    }

    @Override
    public int order() {
        return 500;
    }

    @Override
    public KerberosTestDto valid() {
        return withName(resourceProperyProvider().getName());
    }

    public KerberosTestDto withRequest(KerberosV4Request request) {
        setRequest(request);
        return this;
    }

    public KerberosTestDto withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
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
