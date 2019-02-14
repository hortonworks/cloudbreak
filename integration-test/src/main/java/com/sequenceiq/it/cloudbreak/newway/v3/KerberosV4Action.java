package com.sequenceiq.it.cloudbreak.newway.v3;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosViewV4Response;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.KerberosEntity;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class KerberosV4Action {
    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosV4Action.class);

    private KerberosV4Action() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) {
        KerberosEntity kerberosEntity = (KerberosEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(" post "
                .concat(kerberosEntity.getName())
                .concat(" kerberos. "));
        kerberosEntity.setResponse(
                client.getCloudbreakClient()
                        .kerberosConfigV4Endpoint()
                        .create(workspaceId, kerberosEntity.getRequest()));

        integrationTestContext.putCleanUpParam(kerberosEntity.getName(), kerberosEntity.getResponse().getId());
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        KerberosEntity kerberosEntity = (KerberosEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(" get "
                .concat(kerberosEntity.getName())
                .concat(" private blueprint by Name. "));
        kerberosEntity.setResponse(
                client.getCloudbreakClient()
                        .kerberosConfigV4Endpoint()
                        .get(workspaceId, kerberosEntity.getName()));
        Log.logJSON(" get "
                .concat(kerberosEntity.getName())
                .concat(" blueprint response: "),
                kerberosEntity.getResponse());
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) {
        KerberosEntity kerberosEntity = (KerberosEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(" get all private blueprints. ");
        GeneralCollectionV4Response<KerberosViewV4Response> kerberoses =
                client.getCloudbreakClient().kerberosConfigV4Endpoint().list(workspaceId, null, Boolean.FALSE);
        Set<KerberosV4Response> detailedKerberoses = kerberoses.getResponses().stream().map(krv ->
                client.getCloudbreakClient().kerberosConfigV4Endpoint().get(workspaceId, krv.getName())).collect(Collectors.toSet());
        kerberosEntity.setResponses(detailedKerberoses);
    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        KerberosEntity kerberosEntity = (KerberosEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(" delete "
                .concat(kerberosEntity.getName())
                .concat(" private blueprint with Name. "));
        client.getCloudbreakClient().kerberosConfigV4Endpoint().delete(workspaceId, kerberosEntity.getName());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) {
        try {
            post(integrationTestContext, entity);
        } catch (Exception e) {
            LOGGER.info("Kerberos config probably exist", e);
        }
    }
}
