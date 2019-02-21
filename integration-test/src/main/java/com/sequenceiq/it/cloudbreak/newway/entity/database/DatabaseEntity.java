package com.sequenceiq.it.cloudbreak.newway.entity.database;

import static com.sequenceiq.it.cloudbreak.newway.util.ResponseUtil.getErrorMessage;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.DatabaseV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.Assertion;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.GherkinTest;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.ResourceAction;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.database.DatabaseCreateAction;
import com.sequenceiq.it.cloudbreak.newway.action.database.DatabaseDeleteAction;
import com.sequenceiq.it.cloudbreak.newway.action.database.DatabaseListAction;
import com.sequenceiq.it.cloudbreak.newway.context.Purgable;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.v4.DatabaseAction;

@Prototype
public class DatabaseEntity extends AbstractCloudbreakEntity<DatabaseV4Request, DatabaseV4Response, DatabaseEntity> implements Purgable<DatabaseV4Response> {

    public static final String DATABASE = "DATABASE";

    DatabaseEntity(String newId) {
        super(newId);
        setRequest(new DatabaseV4Request());
    }

    DatabaseEntity() {
        this(DATABASE);
    }

    public DatabaseEntity(TestContext testContext) {
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

    public DatabaseEntity valid() {
        return withName(getNameCreator().getRandomNameForResource())
                .withConnectionUserName("user")
                .withConnectionPassword("password")
                .withConnectionURL("jdbc:postgresql://somedb.com:5432/mydb")
                .withType("HIVE");
    }

    public DatabaseEntity withRequest(DatabaseV4Request request) {
        setRequest(request);
        return this;
    }

    public DatabaseEntity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public DatabaseEntity withConnectionPassword(String password) {
        getRequest().setConnectionPassword(password);
        return this;
    }

    public DatabaseEntity withConnectionUserName(String username) {
        getRequest().setConnectionUserName(username);
        return this;
    }

    public DatabaseEntity withConnectionURL(String connectionURL) {
        getRequest().setConnectionURL(connectionURL);
        return this;
    }

    public DatabaseEntity withType(String type) {
        getRequest().setType(type);
        return this;
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

    private static Function<IntegrationTestContext, DatabaseEntity> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, DatabaseEntity.class);
    }

    static Function<IntegrationTestContext, DatabaseEntity> getNew() {
        return testContext -> new DatabaseEntity();
    }

    public static DatabaseEntity request() {
        return new DatabaseEntity();
    }

    public static DatabaseEntity isCreated(String id) {
        var database = new DatabaseEntity();
        database.setCreationStrategy(DatabaseAction::createInGiven);
        return database;
    }

    public static ResourceAction getAll() {
        return new ResourceAction(getNew(), DatabaseAction::getAll);
    }

    public static ResourceAction delete(String key) {
        return new ResourceAction(getTestContext(key), DatabaseAction::delete);
    }

    public static ResourceAction delete() {
        return delete(DATABASE);
    }

    public static DatabaseEntity delete(TestContext testContext, DatabaseEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().databaseV4Endpoint().delete(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }

    public static Assertion<DatabaseEntity> assertThis(BiConsumer<DatabaseEntity, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

    public static Action<DatabaseEntity> post() {
        return new DatabaseCreateAction();
    }

    public static Action<DatabaseEntity> list() {
        return new DatabaseListAction();
    }

    public static Action<DatabaseEntity> deleteV2() {
        return new DatabaseDeleteAction();
    }
}