package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import java.io.IOException;

import com.sequenceiq.cloudbreak.api.model.ldap.LDAPTestRequest;
import com.sequenceiq.it.IntegrationTestContext;

public class LdapConfigAction {
    private LdapConfigAction() {
    }

    static void post(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        LdapConfigEntity ldapconfigEntity = (LdapConfigEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        ldapconfigEntity.setResponse(
                client.getCloudbreakClient()
                        .ldapConfigEndpoint()
                        .postPrivate(ldapconfigEntity.getRequest()));
        logJSON("Ldap config post request: ", ldapconfigEntity.getRequest());
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        LdapConfigEntity ldapconfigEntity = (LdapConfigEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        ldapconfigEntity.setResponse(
                client.getCloudbreakClient()
                        .ldapConfigEndpoint()
                        .getPrivate(ldapconfigEntity.getRequest().getName()));
        logJSON(" get ldap config response: ", ldapconfigEntity.getResponse());
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        LdapConfigEntity ldapconfigEntity = (LdapConfigEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        ldapconfigEntity.setResponses(
                client.getCloudbreakClient()
                        .ldapConfigEndpoint()
                        .getPrivates());
        logJSON(" get all ldap config response: ", ldapconfigEntity.getResponse());
    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        LdapConfigEntity ldapconfigEntity = (LdapConfigEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        client.getCloudbreakClient()
                .ldapConfigEndpoint()
                .deletePrivate(ldapconfigEntity.getName());
    }

    static void testConnect(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {

        LdapTestEntity ldapTestEntity = (LdapTestEntity) entity;

        LDAPTestRequest ldapTestRequest = new LDAPTestRequest();
        ldapTestRequest.setValidationRequest(ldapTestEntity.getRequest());

        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        ldapTestEntity.setResponse(
                client.getCloudbreakClient()
                        .ldapConfigEndpoint()
                        .testLdapConnection(ldapTestRequest));
        logJSON("Ldap test post request: ", ldapTestEntity.getRequest());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        try {
            get(integrationTestContext, entity);
        } catch (Exception e) {
            post(integrationTestContext, entity);
        }
    }

    static void createDeleteInGiven(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        post(integrationTestContext, entity);
        delete(integrationTestContext, entity);
    }
}