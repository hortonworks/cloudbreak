package com.sequenceiq.it.cloudbreak.dto.mock.endpoint;

import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.mock.MockUri;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.DefaultResponseConfigure;

public class DatabaseEndpoints<T extends CloudbreakTestDto> {

    public static final String DATABASE_API_ROOT = "/{mockUuid}/db";

    public static final String DATABASE_API_UPGRADE = DATABASE_API_ROOT + "/upgrade";

    private T testDto;

    private MockedTestContext mockedTestContext;

    public DatabaseEndpoints(T testDto, MockedTestContext mockedTestContext) {
        this.testDto = testDto;
        this.mockedTestContext = mockedTestContext;
    }

    public Database<T> database() {
        return (Database<T>) EndpointProxyFactory.create(Database.class, testDto, mockedTestContext);
    }

    public Database<T> databaseUpgrade() {
        return (Database<T>) EndpointProxyFactory.create(DatabaseUpgrade.class, testDto, mockedTestContext);
    }

    @MockUri(url = DATABASE_API_ROOT)
    public interface Database<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T, CloudResourceStatus[]> post();

        DefaultResponseConfigure<T, Object> delete();

        DefaultResponseConfigure<T, ExternalDatabaseStatus> get();

        DefaultResponseConfigure<T, Void> put();
    }

    @MockUri(url = DATABASE_API_UPGRADE)
    public interface DatabaseUpgrade<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T, Void> post();

        DefaultResponseConfigure<T, Void> put();
    }
}
