package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import java.io.IOException;

import com.sequenceiq.cloudbreak.api.model.rds.RDSTestRequest;
import com.sequenceiq.it.IntegrationTestContext;

public class RdsConfigAction {
    private RdsConfigAction() {
    }

    static void post(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        RdsConfigEntity rdsconfigEntity = (RdsConfigEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        rdsconfigEntity.setResponse(
                client.getCloudbreakClient()
                        .rdsConfigEndpoint()
                        .postPrivate(rdsconfigEntity.getRequest()));
        logJSON("Rds config post request: ", rdsconfigEntity.getRequest());
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        RdsConfigEntity rdsconfigEntity = (RdsConfigEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        rdsconfigEntity.setResponse(
                client.getCloudbreakClient()
                        .rdsConfigEndpoint()
                        .getPrivate(rdsconfigEntity.getRequest().getName()));
        logJSON(" get rds config response: ", rdsconfigEntity.getResponse());
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        RdsConfigEntity rdsconfigEntity = (RdsConfigEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        rdsconfigEntity.setResponses(
                client.getCloudbreakClient()
                        .rdsConfigEndpoint()
                        .getPrivates());
        logJSON(" get all rds config response: ", rdsconfigEntity.getResponse());
    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        RdsConfigEntity rdsconfigEntity = (RdsConfigEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        client.getCloudbreakClient()
                .rdsConfigEndpoint()
                .deletePrivate(rdsconfigEntity.getName());
    }

    static void testConnect(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {

        RdsConfigEntity rdsConfigEntity = (RdsConfigEntity) entity;

        RDSTestRequest rdsTestRequest = new RDSTestRequest();
        rdsTestRequest.setRdsConfig(rdsConfigEntity.getRequest());

        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        rdsConfigEntity.setResponseTestResult(
                client.getCloudbreakClient()
                        .rdsConfigEndpoint()
                        .testRdsConnection(rdsTestRequest));
        logJSON("Rds test post request: ", rdsConfigEntity.getRequest());
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