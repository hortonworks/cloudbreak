package com.sequenceiq.it.cloudbreak.dto.database;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractRedbeamsTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.microservice.RedbeamsClient;
import com.sequenceiq.it.cloudbreak.search.Searchable;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.model.common.Status;

@Prototype
public class RedbeamsDatabaseServerTestDto
        extends AbstractRedbeamsTestDto<AllocateDatabaseServerV4Request, DatabaseServerV4Response, RedbeamsDatabaseServerTestDto>
        implements Searchable {

    /**
     * The environment does not delete the standalone database servers on its deletion, so they have to be cleaned up before it.
     */
    private static final int ORDER = 50;

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsDatabaseServerTestDto.class);

    private boolean createdByTest;

    public RedbeamsDatabaseServerTestDto(TestContext testContext) {
        super(new AllocateDatabaseServerV4Request(), testContext);
    }

    @Override
    public RedbeamsDatabaseServerTestDto valid() {
        TestContext testContext = getTestContext();
        if (testContext == null) {
            throw new IllegalStateException("Cannot create valid instance, test context is not available");
        }
        DetailedEnvironmentResponse environmentResponse = testContext.get(EnvironmentTestDto.class).getResponse();
        RedbeamsDatabaseServerTestDto redbeamsDatabaseServerTestDto = withName(getResourcePropertyProvider().getName())
                .withPlatform()
                .withDatabaseServer();
        if (environmentResponse != null) {
            String environmentCrn = environmentResponse.getCrn();
            redbeamsDatabaseServerTestDto = redbeamsDatabaseServerTestDto.withEnvironmentCrn(environmentCrn);
        }
        return redbeamsDatabaseServerTestDto;
    }

    public RedbeamsDatabaseServerTestDto withRequest(AllocateDatabaseServerV4Request request) {
        setRequest(request);
        return this;
    }

    public RedbeamsDatabaseServerTestDto withDatabaseServer() {
        DatabaseServerV4StackRequest databaseServerV4StackRequest = new DatabaseServerV4StackRequest();
        databaseServerV4StackRequest.setDatabaseVendor("postgres");
        getRequest().setDatabaseServer(databaseServerV4StackRequest);
        return this;
    }

    public RedbeamsDatabaseServerTestDto withPlatform() {
        getRequest().setCloudPlatform(CloudPlatform.MOCK);
        return this;
    }

    public RedbeamsDatabaseServerTestDto withProviderDefaults() {
        getRequest().setCloudPlatform(getTestContext().getCloudPlatform());
        getCloudProvider().extendDatabaseServerRequestWithProviderParams(getRequest().getDatabaseServer());
        return this;
    }

    public RedbeamsDatabaseServerTestDto withInstanceType(String instanceType) {
        getRequest().getDatabaseServer().setInstanceType(instanceType);
        return this;
    }

    public RedbeamsDatabaseServerTestDto withFallbackInstanceTypes(List<String> fallbackInstanceTypes) {
        getRequest().getDatabaseServer().setFallbackInstanceTypes(fallbackInstanceTypes);
        return this;
    }

    public RedbeamsDatabaseServerTestDto withTags(Map<String, String> tags) {
        getRequest().setTags(tags);
        return this;
    }

    public RedbeamsDatabaseServerTestDto withName(String name) {
        getRequest().setName(name);
        return this;
    }

    public RedbeamsDatabaseServerTestDto withEnvironmentCrn(String environmentCrn) {
        getRequest().setEnvironmentCrn(environmentCrn);
        return this;
    }

    public RedbeamsDatabaseServerTestDto withClusterCrn(String clusterCrn) {
        getRequest().setClusterCrn(clusterCrn);
        return this;
    }

    public RedbeamsDatabaseServerTestDto await(Status status) {
        return await(status, emptyRunningParameter());
    }

    public RedbeamsDatabaseServerTestDto await(Status status, RunningParameter runningParameter) {
        return getTestContext().await(this, Map.of("status", status), runningParameter);
    }

    /**
     * Marks the database server as created by the test, so it is deleted on cleanup. Database servers only described by the test, like the
     * ones owned by a Data Lake, are left for their owner to delete.
     */
    public RedbeamsDatabaseServerTestDto markCreatedByTest() {
        createdByTest = true;
        return this;
    }

    @Override
    public void deleteForCleanup() {
        if (!createdByTest || getResponse() == null) {
            LOGGER.info("Database server {} was not created by the test, thus cleanup not needed.", getName());
            return;
        }
        try {
            RedbeamsClient client = getClientForCleanup();
            LOGGER.info("Deleting database server with crn: {}", getCrn());
            client.getDefaultClient(getTestContext()).databaseServerV4Endpoint().deleteByCrn(getCrn(), true);
            getTestContext().awaitWithClient(this, Map.of("status", Status.DELETE_COMPLETED), client);
        } catch (NotFoundException nfe) {
            LOGGER.info("Database server not found, thus cleanup not needed.");
        }
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public String getSearchId() {
        return getName();
    }

    @Override
    public String getCrn() {
        return getResponse().getCrn();
    }
}
