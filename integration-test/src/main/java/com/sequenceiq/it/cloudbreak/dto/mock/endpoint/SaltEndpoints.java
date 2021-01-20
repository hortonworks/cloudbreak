package com.sequenceiq.it.cloudbreak.dto.mock.endpoint;

import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.mock.MockUri;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.DefaultResponseConfigure;

public final class SaltEndpoints<T extends CloudbreakTestDto> {

    public static final String SALT_BOOT_ROOT = "/{mockUuid}/saltboot";

    public static final String SALT_API_ROOT = "/{mockUuid}/saltapi";

    private T testDto;

    private MockedTestContext mockedTestContext;

    public SaltEndpoints(T testDto, MockedTestContext mockedTestContext) {
        this.testDto = testDto;
        this.mockedTestContext = mockedTestContext;
    }

    public SaltHealth<T> health() {
        return (SaltHealth<T>) EndpointProxyFactory.create(SaltHealth.class, testDto, mockedTestContext);
    }

    public SaltActionDistribute<T> saltActionDistribute() {
        return (SaltActionDistribute<T>) EndpointProxyFactory.create(SaltActionDistribute.class, testDto, mockedTestContext);
    }

    public Run<T> run() {
        return (Run<T>) EndpointProxyFactory.create(Run.class, testDto, mockedTestContext);
    }

    @MockUri(url = SALT_BOOT_ROOT + "/health")
    public interface SaltHealth<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T, Void> post();

        DefaultResponseConfigure<T, GenericResponse> get();
    }

    @MockUri(url = SALT_API_ROOT + "/run")
    public interface Run<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T, Object> post();
    }

    @MockUri(url = SALT_BOOT_ROOT + "/file/distribute")
    public interface SaltFileDistribute<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T, GenericResponses> post();
    }

    @MockUri(url = SALT_BOOT_ROOT + "/salt/action/distribute")
    public interface SaltActionDistribute<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T, GenericResponses> post();
    }

    // SALT_BOOT_ROOT + "/salt/server/pillar/distribute"
    @MockUri(url = SALT_BOOT_ROOT + "/salt/server/pillar/distribute")
    public interface SaltPillar<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T, GenericResponses> post();
    }
}