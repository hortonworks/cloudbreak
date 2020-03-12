package com.sequenceiq.it.cloudbreak.dto.database;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractRedbeamsTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.search.Searchable;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerStatusV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.model.common.Status;

@Prototype
public class RedbeamsDatabaseServerTestDto
        extends AbstractRedbeamsTestDto<AllocateDatabaseServerV4Request, DatabaseServerStatusV4Response, RedbeamsDatabaseServerTestDto>
        implements Searchable {

    public static final String DATABASE = "DATABASE";

    public RedbeamsDatabaseServerTestDto(TestContext testContext) {
        super(new AllocateDatabaseServerV4Request(), testContext);
    }

    @Override
    public RedbeamsDatabaseServerTestDto valid() {
        TestContext testContext = getTestContext();
        if (testContext == null) {
            throw new IllegalStateException("Cannot create valid instance, test context is not available");
        }
        String environmentCrn = testContext.get(EnvironmentTestDto.class).getResponse().getCrn();
        return withName(getResourcePropertyProvider().getName())
                .withPlatform()
                .withDatabaseServer()
                .withEnvironmentCrn(environmentCrn);
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

    public RedbeamsDatabaseServerTestDto withName(String name) {
        getRequest().setName(name);
        return this;
    }

    public RedbeamsDatabaseServerTestDto withEnvironmentCrn(String environmentCrn) {
        getRequest().setEnvironmentCrn(environmentCrn);
        return this;
    }

    public RedbeamsDatabaseServerTestDto await(Status status) {
        return await(status, emptyRunningParameter());
    }

    public RedbeamsDatabaseServerTestDto await(Status status, RunningParameter runningParameter) {
        return getTestContext().await(this, status, runningParameter);
    }

    @Override
    public int order() {
        return 500;
    }

    @Override
    public String getSearchId() {
        return getName();
    }
}
