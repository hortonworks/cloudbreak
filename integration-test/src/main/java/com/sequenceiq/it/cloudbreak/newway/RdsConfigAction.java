package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import java.io.IOException;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseTestV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
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
                        .databaseV4Endpoint()
                        .create(client.getWorkspaceId(), rdsconfigEntity.getRequest()));
        logJSON("Rds config post request: ", rdsconfigEntity.getRequest());
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        RdsConfigEntity rdsconfigEntity = (RdsConfigEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        rdsconfigEntity.setResponse(
                client.getCloudbreakClient()
                        .databaseV4Endpoint()
                        .get(client.getWorkspaceId(), rdsconfigEntity.getRequest().getName()));
        logJSON(" get rds config response: ", rdsconfigEntity.getResponse());
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        RdsConfigEntity rdsconfigEntity = (RdsConfigEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        rdsconfigEntity.setResponses(
                (Set<DatabaseV4Response>) client.getCloudbreakClient()
                        .databaseV4Endpoint()
                        .list(client.getWorkspaceId(), null, Boolean.FALSE)
                        .getResponses());
        logJSON(" get all rds config response: ", rdsconfigEntity.getResponse());
    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        RdsConfigEntity rdsconfigEntity = (RdsConfigEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        client.getCloudbreakClient()
                .databaseV4Endpoint()
                .delete(client.getWorkspaceId(), rdsconfigEntity.getName());
    }

    static void testConnect(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {

        RdsConfigEntity rdsConfigEntity = (RdsConfigEntity) entity;

        DatabaseTestV4Request databaseTestV4Request = new DatabaseTestV4Request();
        databaseTestV4Request.setDatabase(rdsConfigEntity.getRequest());

        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        rdsConfigEntity.setResponseTestResult(
                client.getCloudbreakClient()
                        .databaseV4Endpoint()
                        .test(client.getWorkspaceId(), databaseTestV4Request));
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