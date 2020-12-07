package com.sequenceiq.it.cloudbreak.dto.mock.endpoint;

import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.mock.SparkUri;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.DefaultResponseConfigure;
import com.sequenceiq.it.cloudbreak.mock.ExecuteQueryToMockInfrastructure;

public final class SaltEndpoints<T extends CloudbreakTestDto> {

    public static final String SALT_BOOT_ROOT = "/{mockUuid}/saltboot";

    public static final String SALT_API_ROOT = "/{mockUuid}/saltapi";

    private T testDto;

    private ExecuteQueryToMockInfrastructure executeQueryToMockInfrastructure;

    public SaltEndpoints(T testDto, ExecuteQueryToMockInfrastructure executeQueryToMockInfrastructure) {
        this.testDto = testDto;
        this.executeQueryToMockInfrastructure = executeQueryToMockInfrastructure;
    }

    public SaltHealth<T> health() {
        return (SaltHealth<T>) EndpointProxyFactory.create(SaltHealth.class, testDto, executeQueryToMockInfrastructure);
    }

    public SaltActionDistribute<T> saltActionDistribute() {
        return (SaltActionDistribute<T>) EndpointProxyFactory.create(SaltActionDistribute.class, testDto, executeQueryToMockInfrastructure);
    }

    public Run<T> run() {
        return (Run<T>) EndpointProxyFactory.create(Run.class, testDto, executeQueryToMockInfrastructure);
    }

    @SparkUri(url = SALT_BOOT_ROOT + "/health")
    public interface SaltHealth<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T> post();

        DefaultResponseConfigure<T> get();
    }

    @SparkUri(url = SALT_API_ROOT + "/run")
    public interface Run<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T> post();
    }

    @SparkUri(url = SALT_BOOT_ROOT + "/file/distribute")
    public interface SaltFileDistribute<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T> post();
    }

    @SparkUri(url = SALT_BOOT_ROOT + "/salt/action/distribute")
    public interface SaltActionDistribute<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T> post();
    }

    // SALT_BOOT_ROOT + "/salt/server/pillar/distribute"
    @SparkUri(url = SALT_BOOT_ROOT + "/salt/server/pillar/distribute")
    public interface SaltPillar<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T> post();
    }
}