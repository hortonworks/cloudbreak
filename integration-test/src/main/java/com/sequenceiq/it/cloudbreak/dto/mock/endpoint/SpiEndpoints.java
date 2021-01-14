package com.sequenceiq.it.cloudbreak.dto.mock.endpoint;

import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.mock.SparkUri;
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

    public CloudInstanceStatuses<T> cloudInstanceStatuses() {
        return (CloudInstanceStatuses<T>) EndpointProxyFactory.create(CloudInstanceStatuses.class, testDto, mockedTestContext);
    }

    public CloudMetadataStatuses<T> cloudMetadataStatuses() {
        return (CloudMetadataStatuses<T>) EndpointProxyFactory.create(CloudMetadataStatuses.class, testDto, mockedTestContext);
    }

    public RegisterPublicKey<T> registerPublicKey() {
        return (RegisterPublicKey<T>) EndpointProxyFactory.create(RegisterPublicKey.class, testDto, mockedTestContext);
    }

    public UnregisterPublicKey<T> unregisterPublicKey() {
        return (UnregisterPublicKey<T>) EndpointProxyFactory.create(UnregisterPublicKey.class, testDto, mockedTestContext);
    }

    public GetPublicKey<T> getPublicKey() {
        return (GetPublicKey<T>) EndpointProxyFactory.create(GetPublicKey.class, testDto, mockedTestContext);
    }

    public Launch<T> launch() {
        return (Launch<T>) EndpointProxyFactory.create(Launch.class, testDto, mockedTestContext);
    }

    @SparkUri(url = SPI_ROOT + REGISTER_PUBLIC_KEY)
    public interface RegisterPublicKey<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T> post();
    }

    @SparkUri(url = SPI_ROOT + UNREGISTER_PUBLIC_KEY)
    public interface UnregisterPublicKey<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T> post();
    }

    @SparkUri(url = SPI_ROOT + GET_PUBLIC_KEY_BY_ID)
    public interface GetPublicKey<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T> get();
    }

    @SparkUri(url = SPI_WITH_MOCK_ROOT + "/cloud_instance_statuses")
    public interface CloudInstanceStatuses<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T> post();
    }

    @SparkUri(url = SPI_WITH_MOCK_ROOT + "/cloud_metadata_statuses")
    public interface CloudMetadataStatuses<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T> post();
    }

    @SparkUri(url = SPI_WITH_MOCK_ROOT + "/launch")
    public interface Launch<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T> post();
    }
}
