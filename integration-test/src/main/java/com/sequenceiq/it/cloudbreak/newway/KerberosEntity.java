package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.Function;

import javax.ws.rs.WebApplicationException;

import com.sequenceiq.cloudbreak.api.model.KerberosResponse;
import com.sequenceiq.cloudbreak.api.model.kerberos.FreeIPAKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.model.kerberos.KerberosRequest;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class KerberosEntity extends AbstractCloudbreakEntity<KerberosRequest, KerberosResponse, KerberosEntity>  {

    public static final String KERBEROS = "KERBEROS";

    public static final String DEFAULT_MASTERKEY = "masterkey";

    public static final String DEFAULT_ADMIN_USER = "admin";

    public static final String DEFAULT_ADMIN_PASSWORD = "password";

    private KerberosRequest request;

    KerberosEntity() {
        this(KERBEROS);
    }

    KerberosEntity(String newId) {
        super(newId);
        request = new KerberosRequest();
        FreeIPAKerberosDescriptor ipa = new FreeIPAKerberosDescriptor();
        ipa.setPassword(DEFAULT_ADMIN_PASSWORD);
        ipa.setAdminUrl("http://someurl.com");
        ipa.setRealm("somerealm");
        ipa.setUrl("someUrl");
        request.setName("FreeIpaKdc");
        request.setFreeIpa(ipa);
    }

    public KerberosEntity(TestContext testContext) {
        super(new KerberosRequest(), testContext);
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        try {
            cloudbreakClient.getCloudbreakClient().kerberosConfigV3Endpoint().deleteInWorkspace(cloudbreakClient.getWorkspaceId(), getName());
        } catch (WebApplicationException ignore) {
            LOGGER.info("Something happend during the kerberos resource delete operation.");
        }
    }

    public KerberosRequest getRequest() {
        return request;
    }

    public void setRequest(KerberosRequest request) {
        this.request = request;
    }

    @Override
    public KerberosEntity valid() {
        setRequest(request);
        return this;
    }

    public KerberosEntity withRequest(KerberosRequest request) {
        setRequest(request);
        return this;
    }

    public KerberosEntity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public static KerberosEntity request(String key) {
        return new KerberosEntity(key);
    }

    public static KerberosEntity request() {
        return new KerberosEntity();
    }

    public static Function<IntegrationTestContext, KerberosEntity> getTestContextCluster(String key) {
        return testContext -> testContext.getContextParam(key, KerberosEntity.class);
    }

    public static Function<IntegrationTestContext, KerberosEntity> getTestContextCluster() {
        return getTestContextCluster(KERBEROS);
    }

}
