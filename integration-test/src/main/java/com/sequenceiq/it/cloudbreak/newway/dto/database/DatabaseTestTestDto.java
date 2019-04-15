package com.sequenceiq.it.cloudbreak.newway.dto.database;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseTestV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseTestV4Response;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.AbstractCloudbreakTestDto;

@Prototype
public class DatabaseTestTestDto extends AbstractCloudbreakTestDto<DatabaseTestV4Request, DatabaseTestV4Response, DatabaseTestTestDto> {

    public static final String DATABASE_TEST = "DATABASE_TEST";

    DatabaseTestTestDto(String newId) {
        super(newId);
        setRequest(new DatabaseTestV4Request());
    }

    DatabaseTestTestDto() {
        this(DATABASE_TEST);
    }

    public DatabaseTestTestDto(TestContext testContext) {
        super(new DatabaseTestV4Request(), testContext);
    }

    public DatabaseTestTestDto valid() {
        return withRequest(new DatabaseV4Request())
                .withName(resourceProperyProvider().getName())
                .withConnectionUserName("user")
                .withConnectionPassword("password")
                .withConnectionURL("jdbc:postgresql://somedb.com:5432/mydb")
                .withType("HIVE");
    }

    public DatabaseTestTestDto withExistingName(String name) {
        DatabaseTestV4Request testRequest = new DatabaseTestV4Request();
        testRequest.setExistingDatabaseName(name);
        setRequest(testRequest);
        return this;
    }

    public DatabaseTestTestDto withRequest(DatabaseV4Request request) {
        DatabaseTestV4Request testRequest = new DatabaseTestV4Request();
        testRequest.setDatabase(request);
        setRequest(testRequest);
        return this;
    }

    public DatabaseTestTestDto withName(String name) {
        getRequest().getDatabase().setName(name);
        return this;
    }

    public DatabaseTestTestDto withConnectionPassword(String password) {
        getRequest().getDatabase().setConnectionPassword(password);
        return this;
    }

    public DatabaseTestTestDto withConnectionUserName(String username) {
        getRequest().getDatabase().setConnectionUserName(username);
        return this;
    }

    public DatabaseTestTestDto withConnectionURL(String connectionURL) {
        getRequest().getDatabase().setConnectionURL(connectionURL);
        return this;
    }

    public DatabaseTestTestDto withType(String type) {
        getRequest().getDatabase().setType(type);
        return this;
    }

    private static Function<IntegrationTestContext, DatabaseTestTestDto> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, DatabaseTestTestDto.class);
    }

    static Function<IntegrationTestContext, DatabaseTestTestDto> getNew() {
        return testContext -> new DatabaseTestTestDto();
    }

    public static DatabaseTestTestDto request() {
        return new DatabaseTestTestDto();
    }
}