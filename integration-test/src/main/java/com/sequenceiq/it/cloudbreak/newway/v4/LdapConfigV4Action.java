package com.sequenceiq.it.cloudbreak.newway.v4;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import java.io.IOException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests.LdapTestV4Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.LdapConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.LdapTestEntity;

public class LdapConfigV4Action {
    private LdapConfigV4Action() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        LdapConfigEntity ldapconfigEntity = (LdapConfigEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        ldapconfigEntity.setResponse(
                client.getCloudbreakClient()
                        .ldapConfigV4Endpoint()
                        .post(workspaceId, ldapconfigEntity.getRequest()));
        logJSON("Ldap config post request: ", ldapconfigEntity.getRequest());
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        LdapConfigEntity ldapconfigEntity = (LdapConfigEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        ldapconfigEntity.setResponse(
                client.getCloudbreakClient()
                        .ldapConfigV4Endpoint()
                        .get(workspaceId, ldapconfigEntity.getRequest().getName()));
        logJSON(" get ldap config response: ", ldapconfigEntity.getResponse());
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        LdapConfigEntity ldapconfigEntity = (LdapConfigEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        ldapconfigEntity.setResponses(
                client.getCloudbreakClient()
                        .ldapConfigV4Endpoint()
                        .list(workspaceId, null, Boolean.FALSE).getResponses());
        logJSON(" get all ldap config response: ", ldapconfigEntity.getResponse());
    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        LdapConfigEntity ldapconfigEntity = (LdapConfigEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        client.getCloudbreakClient()
                .ldapConfigV4Endpoint()
                .delete(workspaceId, ldapconfigEntity.getName());
    }

    public static void testConnect(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {

        LdapTestEntity ldapTestEntity = (LdapTestEntity) entity;

        LdapTestV4Request ldapTestV4Request = new LdapTestV4Request();
        ldapTestV4Request.setLdap(ldapTestEntity.getRequest());

        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        ldapTestEntity.setResponse(
                client.getCloudbreakClient()
                        .ldapConfigV4Endpoint()
                        .test(workspaceId, ldapTestV4Request));
        logJSON("Ldap test post request: ", ldapTestEntity.getRequest());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        try {
            get(integrationTestContext, entity);
        } catch (Exception e) {
            post(integrationTestContext, entity);
        }
    }

    public static void createDeleteInGiven(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        post(integrationTestContext, entity);
        delete(integrationTestContext, entity);
    }
}
