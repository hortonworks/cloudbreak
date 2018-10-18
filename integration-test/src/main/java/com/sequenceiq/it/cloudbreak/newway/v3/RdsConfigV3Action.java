package com.sequenceiq.it.cloudbreak.newway.v3;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import java.io.IOException;

import com.sequenceiq.cloudbreak.api.model.rds.RDSTestRequest;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.RdsConfigEntity;

public class RdsConfigV3Action {
    private RdsConfigV3Action() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        RdsConfigEntity rdsconfigEntity = (RdsConfigEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        rdsconfigEntity.setResponse(
                client.getCloudbreakClient()
                        .rdsConfigV3Endpoint()
                        .createInWorkspace(workspaceId, rdsconfigEntity.getRequest()));
        logJSON("Rds config post request: ", rdsconfigEntity.getRequest());
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        RdsConfigEntity rdsconfigEntity = (RdsConfigEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        rdsconfigEntity.setResponse(
                client.getCloudbreakClient()
                        .rdsConfigV3Endpoint()
                        .getByNameInWorkspace(workspaceId, rdsconfigEntity.getRequest().getName()));
        logJSON(" get rds config response: ", rdsconfigEntity.getResponse());
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        RdsConfigEntity rdsconfigEntity = (RdsConfigEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        rdsconfigEntity.setResponses(
                client.getCloudbreakClient()
                        .rdsConfigV3Endpoint()
                        .listByWorkspace(workspaceId, null, null));
        logJSON(" get all rds config response: ", rdsconfigEntity.getResponse());
    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        RdsConfigEntity rdsconfigEntity = (RdsConfigEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        client.getCloudbreakClient()
                .rdsConfigV3Endpoint()
                .deleteInWorkspace(workspaceId, rdsconfigEntity.getName());
    }

    public static void testConnect(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {

        RdsConfigEntity rdsConfigEntity = (RdsConfigEntity) entity;

        RDSTestRequest rdsTestRequest = new RDSTestRequest();
        rdsTestRequest.setRdsConfig(rdsConfigEntity.getRequest());

        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        rdsConfigEntity.setResponseTestResult(
                client.getCloudbreakClient()
                        .rdsConfigV3Endpoint()
                        .testRdsConnection(workspaceId, rdsTestRequest));
        logJSON("Rds test post request: ", rdsConfigEntity.getRequest());
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
