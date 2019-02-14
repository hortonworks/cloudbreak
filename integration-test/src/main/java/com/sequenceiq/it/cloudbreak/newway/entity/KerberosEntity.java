package com.sequenceiq.it.cloudbreak.newway.entity;

import javax.ws.rs.WebApplicationException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.ActiveDirectoryKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosV4Response;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class KerberosEntity extends AbstractCloudbreakEntity<KerberosV4Request, KerberosV4Response, KerberosEntity> {

    public static final String DEFAULT_MASTERKEY = "masterkey";

    public static final String DEFAULT_ADMIN_USER = "admin";

    public static final String DEFAULT_ADMIN_PASSWORD = "password";

    public KerberosEntity(TestContext testContext) {
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
    public KerberosEntity valid() {
        return withName(getNameCreator().getRandomNameForMock());
    }

    public KerberosEntity withRequest(KerberosV4Request request) {
        setRequest(request);
        return this;
    }

    public KerberosEntity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public KerberosEntity withActiveDirectoryDescriptor(ActiveDirectoryKerberosDescriptor activeDirectoryDescriptor) {
        getRequest().setActiveDirectory(activeDirectoryDescriptor);
        return this;
    }

    public KerberosEntity withDefaultAD() {
        ActiveDirectoryKerberosDescriptor activeDirectory = new ActiveDirectoryKerberosDescriptor();
        activeDirectory.setTcpAllowed(true);
        activeDirectory.setPrincipal("admin/principal");
        activeDirectory.setPassword("kerberosPassword");
        activeDirectory.setUrl("someurl.com");
        activeDirectory.setAdminUrl("admin.url.com");
        activeDirectory.setRealm("REALM.EXAMPLE.COM");
        activeDirectory.setLdapUrl("otherurl.com");
        activeDirectory.setContainerDn("{}");
        return  withActiveDirectoryDescriptor(activeDirectory);
    }
}
