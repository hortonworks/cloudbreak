package com.sequenceiq.it.cloudbreak.dto.mock.endpoint;

import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.mock.SparkUri;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.DefaultResponseConfigure;
import com.sequenceiq.it.cloudbreak.mock.ExecuteQueryToMockInfrastructure;

public final class SpiEndpoints<T extends CloudbreakTestDto> {

    public static final String SPI_ROOT = "/spi";

    public static final String SPI_WITH_MOCK_ROOT = "/{mockUuid}/spi";

    public static final String REGISTER_PUBLIC_KEY = "/register_public_key";

    public static final String UNREGISTER_PUBLIC_KEY = "/unregister_public_key";

    public static final String GET_PUBLIC_KEY_BY_ID = "/get_public_key/{publicKeyId}";

    private T testDto;

    private ExecuteQueryToMockInfrastructure executeQueryToMockInfrastructure;

    public SpiEndpoints(T testDto, ExecuteQueryToMockInfrastructure executeQueryToMockInfrastructure) {
        this.testDto = testDto;
        this.executeQueryToMockInfrastructure = executeQueryToMockInfrastructure;
    }

    public CloudInstanceStatuses<T> cloudInstanceStatuses() {
        return (CloudInstanceStatuses<T>) EndpointProxyFactory.create(CloudInstanceStatuses.class, testDto, executeQueryToMockInfrastructure);
    }

    public CloudMetadataStatuses<T> cloudMetadataStatuses() {
        return (CloudMetadataStatuses<T>) EndpointProxyFactory.create(CloudMetadataStatuses.class, testDto, executeQueryToMockInfrastructure);
    }

    public RegisterPublicKey<T> registerPublicKey() {
        return (RegisterPublicKey<T>) EndpointProxyFactory.create(RegisterPublicKey.class, testDto, executeQueryToMockInfrastructure);
    }

    public UnregisterPublicKey<T> unregisterPublicKey() {
        return (UnregisterPublicKey<T>) EndpointProxyFactory.create(UnregisterPublicKey.class, testDto, executeQueryToMockInfrastructure);
    }

    public GetPublicKey<T> getPublicKey() {
        return (GetPublicKey<T>) EndpointProxyFactory.create(GetPublicKey.class, testDto, executeQueryToMockInfrastructure);
    }

    public Launch<T> launch() {
        return (Launch<T>) EndpointProxyFactory.create(Launch.class, testDto, executeQueryToMockInfrastructure);
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
