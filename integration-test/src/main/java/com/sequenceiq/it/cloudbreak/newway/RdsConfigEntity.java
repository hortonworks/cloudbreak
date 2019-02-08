package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.newway.util.ResponseUtil.getErrorMessage;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.DatabaseV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseTestV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.it.cloudbreak.newway.context.Purgable;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class RdsConfigEntity extends AbstractCloudbreakEntity<DatabaseV4Request, DatabaseV4Response, RdsConfigEntity> implements Purgable<DatabaseV4Response> {

    public static final String RDS_CONFIG = "RDS_CONFIG";

    private DatabaseTestV4Response response;

    RdsConfigEntity(String newId) {
        super(newId);
        setRequest(new DatabaseV4Request());
    }

    RdsConfigEntity() {
        this(RDS_CONFIG);
    }

    public RdsConfigEntity(TestContext testContext) {
        super(new DatabaseV4Request(), testContext);
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        try {
            cloudbreakClient.getCloudbreakClient().databaseV4Endpoint().delete(cloudbreakClient.getWorkspaceId(), getName());
        } catch (WebApplicationException ignore) {
            LOGGER.info("Something happend.");
        }
    }

    public RdsConfigEntity valid() {
        return withName(getNameCreator().getRandomNameForMock())
                .withConnectionUserName("user")
                .withConnectionPassword("password")
                .withConnectionURL("jdbc:postgresql://somedb.com:5432/mydb")
                .withType("HIVE");
    }

    public RdsConfigEntity withRequest(DatabaseV4Request request) {
        setRequest(request);
        return this;
    }

    public RdsConfigEntity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public RdsConfigEntity withConnectionPassword(String password) {
        getRequest().setConnectionPassword(password);
        return this;
    }

    public RdsConfigEntity withConnectionUserName(String username) {
        getRequest().setConnectionUserName(username);
        return this;
    }

    public RdsConfigEntity withConnectionURL(String connectionURL) {
        getRequest().setConnectionURL(connectionURL);
        return this;
    }

    public RdsConfigEntity withType(String type) {
        getRequest().setType(type);
        return this;
    }

    public DatabaseTestV4Response getResponseTestResult() {
        return response;
    }

    public void setResponseTestResult(DatabaseTestV4Response response) {
        this.response = response;
    }

    @Override
    public List<DatabaseV4Response> getAll(CloudbreakClient client) {
        DatabaseV4Endpoint databaseV4Endpoint = client.getCloudbreakClient().databaseV4Endpoint();
        return databaseV4Endpoint.list(client.getWorkspaceId(), null, Boolean.FALSE).getResponses().stream()
                .filter(s -> s.getName() != null)
                .collect(Collectors.toList());
    }

    @Override
    public boolean deletable(DatabaseV4Response entity) {
        return entity.getName().startsWith("mock-");
    }

    @Override
    public void delete(TestContext testContext, DatabaseV4Response entity, CloudbreakClient client) {
        try {
            client.getCloudbreakClient().stackV4Endpoint().delete(client.getWorkspaceId(), entity.getName(), true, false);
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on {} purge. {}", entity.getName(), getErrorMessage(e), e);
        }
    }

    @Override
    public int order() {
        return 500;
    }
}