package com.sequenceiq.it.cloudbreak.newway.entity.database;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseTestV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseTestV4Response;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.database.DatabaseTestConnectionAction;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class DatabaseTestEntity extends AbstractCloudbreakEntity<DatabaseTestV4Request, DatabaseTestV4Response, DatabaseTestEntity> {

    public static final String DATABASE_TEST = "DATABASE_TEST";

    DatabaseTestEntity(String newId) {
        super(newId);
        setRequest(new DatabaseTestV4Request());
    }

    DatabaseTestEntity() {
        this(DATABASE_TEST);
    }

    public DatabaseTestEntity(TestContext testContext) {
        super(new DatabaseTestV4Request(), testContext);
    }

    public DatabaseTestEntity valid() {
        return withRequest(new DatabaseV4Request())
                .withName(getNameCreator().getRandomNameForResource())
                .withConnectionUserName("user")
                .withConnectionPassword("password")
                .withConnectionURL("jdbc:postgresql://somedb.com:5432/mydb")
                .withType("HIVE");
    }

    public DatabaseTestEntity withExistingName(String name) {
        DatabaseTestV4Request testRequest = new DatabaseTestV4Request();
        testRequest.setExistingDatabaseName(name);
        setRequest(testRequest);
        return this;
    }

    public DatabaseTestEntity withRequest(DatabaseV4Request request) {
        DatabaseTestV4Request testRequest = new DatabaseTestV4Request();
        testRequest.setDatabase(request);
        setRequest(testRequest);
        return this;
    }

    public DatabaseTestEntity withName(String name) {
        getRequest().getDatabase().setName(name);
        return this;
    }

    public DatabaseTestEntity withConnectionPassword(String password) {
        getRequest().getDatabase().setConnectionPassword(password);
        return this;
    }

    public DatabaseTestEntity withConnectionUserName(String username) {
        getRequest().getDatabase().setConnectionUserName(username);
        return this;
    }

    public DatabaseTestEntity withConnectionURL(String connectionURL) {
        getRequest().getDatabase().setConnectionURL(connectionURL);
        return this;
    }

    public DatabaseTestEntity withType(String type) {
        getRequest().getDatabase().setType(type);
        return this;
    }

    private static Function<IntegrationTestContext, DatabaseTestEntity> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, DatabaseTestEntity.class);
    }

    static Function<IntegrationTestContext, DatabaseTestEntity> getNew() {
        return testContext -> new DatabaseTestEntity();
    }

    public static DatabaseTestEntity request() {
        return new DatabaseTestEntity();
    }

    public static Action<DatabaseTestEntity> testConnection() {
        return new DatabaseTestConnectionAction();
    }
}