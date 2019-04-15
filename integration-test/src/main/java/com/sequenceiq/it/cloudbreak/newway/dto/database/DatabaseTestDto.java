package com.sequenceiq.it.cloudbreak.newway.dto.database;

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
import com.sequenceiq.it.cloudbreak.newway.Assertion;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.GherkinTest;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.DeletableTestDto;

@Prototype
public class DatabaseTestDto extends DeletableTestDto<DatabaseV4Request, DatabaseV4Response, DatabaseTestDto, DatabaseV4Response> {

    public static final String DATABASE = "DATABASE";

    DatabaseTestDto(String newId) {
        super(newId);
        setRequest(new DatabaseV4Request());
    }

    DatabaseTestDto() {
        this(DATABASE);
    }

    public DatabaseTestDto(TestContext testContext) {
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

    public DatabaseTestDto valid() {
        return withName(resourceProperyProvider().getName())
                .withDescription(resourceProperyProvider().getDescription("database"))
                .withConnectionUserName("user")
                .withConnectionPassword("password")
                .withConnectionURL("jdbc:postgresql://somedb.com:5432/mydb")
                .withType("HIVE");
    }

    public DatabaseTestDto withRequest(DatabaseV4Request request) {
        setRequest(request);
        return this;
    }

    public DatabaseTestDto withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public DatabaseTestDto withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    public DatabaseTestDto withConnectionPassword(String password) {
        getRequest().setConnectionPassword(password);
        return this;
    }

    public DatabaseTestDto withConnectionUserName(String username) {
        getRequest().setConnectionUserName(username);
        return this;
    }

    public DatabaseTestDto withConnectionURL(String connectionURL) {
        getRequest().setConnectionURL(connectionURL);
        return this;
    }

    public DatabaseTestDto withType(String type) {
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
    protected String name(DatabaseV4Response entity) {
        return entity.getName();
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

    private static Function<IntegrationTestContext, DatabaseTestDto> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, DatabaseTestDto.class);
    }

    static Function<IntegrationTestContext, DatabaseTestDto> getNew() {
        return testContext -> new DatabaseTestDto();
    }

    public static DatabaseTestDto request() {
        return new DatabaseTestDto();
    }

    public static Assertion<DatabaseTestDto> assertThis(BiConsumer<DatabaseTestDto, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }
}