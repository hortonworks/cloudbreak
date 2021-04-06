package com.sequenceiq.it.cloudbreak.dto.mock.endpoint;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.mock.MockUri;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.DefaultResponseConfigure;

public final class SpiEndpoints<T extends CloudbreakTestDto> {

    public static final String SPI_ROOT = "/spi";

    public static final String SPI_WITH_MOCK_ROOT = "/{mockUuid}/spi";

    public static final String REGISTER_PUBLIC_KEY = "/register_public_key";

    public static final String UNREGISTER_PUBLIC_KEY = "/unregister_public_key";

    public static final String GET_PUBLIC_KEY_BY_ID = "/get_public_key/{publicKeyId}";

    private T testDto;

    private MockedTestContext mockedTestContext;

    public SpiEndpoints(T testDto, MockedTestContext mockedTestContext) {
        this.testDto = testDto;
        this.mockedTestContext = mockedTestContext;
    }

    public SpiWithMockUuid.CloudInstanceStatuses<T> cloudInstanceStatuses() {
        return (SpiWithMockUuid.CloudInstanceStatuses<T>) EndpointProxyFactory.create(SpiWithMockUuid.CloudInstanceStatuses.class, testDto, mockedTestContext);
    }

    public SpiWithMockUuid.CloudMetadataStatuses<T> cloudMetadataStatuses() {
        return (SpiWithMockUuid.CloudMetadataStatuses<T>) EndpointProxyFactory.create(SpiWithMockUuid.CloudMetadataStatuses.class, testDto, mockedTestContext);
    }

    public SpiWithMockUuid.StartInstances<T> startInstances() {
        return (SpiWithMockUuid.StartInstances<T>) EndpointProxyFactory.create(SpiWithMockUuid.StartInstances.class, testDto, mockedTestContext);
    }

    public SpiWithMockUuid.StopInstances<T> stopInstances() {
        return (SpiWithMockUuid.StopInstances<T>) EndpointProxyFactory.create(SpiWithMockUuid.StopInstances.class, testDto, mockedTestContext);
    }

    public Spi.RegisterPublicKey<T> registerPublicKey() {
        return (Spi.RegisterPublicKey<T>) EndpointProxyFactory.create(Spi.RegisterPublicKey.class, testDto, mockedTestContext);
    }

    public Spi.UnregisterPublicKey<T> unregisterPublicKey() {
        return (Spi.UnregisterPublicKey<T>) EndpointProxyFactory.create(Spi.UnregisterPublicKey.class, testDto, mockedTestContext);
    }

    public Spi.GetPublicKey<T> getPublicKey() {
        return (Spi.GetPublicKey<T>) EndpointProxyFactory.create(Spi.GetPublicKey.class, testDto, mockedTestContext);
    }

    public SpiWithMockUuid.Launch<T> launch() {
        return (SpiWithMockUuid.Launch<T>) EndpointProxyFactory.create(SpiWithMockUuid.Launch.class, testDto, mockedTestContext);
    }

    public interface Spi {

        @MockUri(url = REGISTER_PUBLIC_KEY)
        interface RegisterPublicKey<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
            DefaultResponseConfigure<T, Void> post();
        }

        @MockUri(url = UNREGISTER_PUBLIC_KEY)
        interface UnregisterPublicKey<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
            DefaultResponseConfigure<T, Void> post();
        }

        @MockUri(url = GET_PUBLIC_KEY_BY_ID)
        interface GetPublicKey<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
            DefaultResponseConfigure<T, Map<String, String>> get();
        }
    }

    @MockUri(url = SPI_WITH_MOCK_ROOT)
    public interface SpiWithMockUuid {

        @MockUri(url = "/cloud_instance_statuses")
        interface CloudInstanceStatuses<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
            DefaultResponseConfigure<T, List<CloudVmInstanceStatus>> post();
        }

        @MockUri(url = "/cloud_metadata_statuses")
        interface CloudMetadataStatuses<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
            DefaultResponseConfigure<T, List<CloudVmMetaDataStatus>> post();
        }

        interface Launch<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
            DefaultResponseConfigure<T, List<CloudVmInstanceStatus>> post();
        }

        @MockUri(url = "/start_instances")
        interface StartInstances<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
            DefaultResponseConfigure<T, List<CloudVmInstanceStatus>> post();
        }

        @MockUri(url = "/stop_instances")
        interface StopInstances<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
            DefaultResponseConfigure<T, List<CloudVmInstanceStatus>> post();
        }
    }
}
