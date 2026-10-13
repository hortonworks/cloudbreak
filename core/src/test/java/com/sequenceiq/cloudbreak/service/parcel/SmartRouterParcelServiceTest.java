package com.sequenceiq.cloudbreak.service.parcel;

import static com.sequenceiq.cloudbreak.service.parcel.SmartRouterParcelService.COMPONENT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.client.RestClientFactory;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@ExtendWith(MockitoExtension.class)
class SmartRouterParcelServiceTest {

    private static final String RELEASE_VERSION = "1.0.0";

    private static final String RHEL8 = "redhat8";

    private static final String RHEL9 = "redhat9";

    private static final String LATEST_COMPILED_BUILD_URL = "https://release.eng.cloudera.com/hwre-api/latestcompiledbuild";

    private static final String GBN_URL = "http://cloudera-build-2-us-west-2.vpc.cloudera.com/s3/build/83751218/";

    /**
     * The version Cloudera Manager knows the parcel and the CSD by, as published in the parcel manifest of the build. It is the configured
     * release version followed by the {@code build} of the latest compiled build response, which is {@code 1.0.0-b9} in the test payload.
     */
    private static final String PRODUCT_VERSION = "1.0.0.1.0.0-b9";

    /**
     * Captured response of the real hwre-api endpoint. The builds it advertises expire after a few days, so the test must never call
     * the endpoint: the whole JAX-RS chain is mocked and this payload is replayed instead.
     */
    private static String latestCompiledBuildResponse;

    @Mock
    private RestClientFactory restClientFactory;

    @Mock
    private Client client;

    @Mock
    private WebTarget webTarget;

    @Mock
    private Invocation.Builder invocationBuilder;

    @InjectMocks
    private SmartRouterParcelService underTest;

    @BeforeAll
    static void beforeAll() throws IOException {
        latestCompiledBuildResponse = FileReaderUtils.readFileFromClasspath(
                "com/sequenceiq/cloudbreak/service/parcel/smartrouter-latest-compiled-build.json");
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "latestCompiledBuildUrl", LATEST_COMPILED_BUILD_URL);
        ReflectionTestUtils.setField(underTest, "releaseVersion", RELEASE_VERSION);
    }

    @Test
    void testGetProductShouldReturnTheSmartRouterProductBasedOnTheLatestCompiledBuild() {
        mockLatestCompiledBuild(RELEASE_VERSION, RHEL8, latestCompiledBuildResponse);

        ClouderaManagerProduct actual = underTest.getProduct(RHEL8);

        assertThat(actual.getName()).isEqualTo(COMPONENT_NAME);
        assertThat(actual.getVersion()).isEqualTo(PRODUCT_VERSION);
        assertThat(actual.getParcel()).isEqualTo(GBN_URL + "smartrouter/1.x/parcels/");
        assertThat(actual.getCsd()).containsExactly(GBN_URL + "smartrouter/1.x/csd/SMARTROUTER-" + PRODUCT_VERSION + ".jar");
    }

    @ParameterizedTest(name = "osType={0}")
    @ValueSource(strings = {RHEL8, RHEL9})
    void testGetProductShouldQueryTheConfiguredEndpointForTheGivenStackReleaseAndOsAsJson(String osType) {
        mockLatestCompiledBuild(RELEASE_VERSION, osType, latestCompiledBuildResponse);

        ClouderaManagerProduct actual = underTest.getProduct(osType);

        verify(client).target(LATEST_COMPILED_BUILD_URL);
        verify(webTarget).queryParam("stack", "smartrouter");
        verify(webTarget).queryParam("release", RELEASE_VERSION);
        verify(webTarget).queryParam("os", osType);
        verify(invocationBuilder).accept(MediaType.APPLICATION_JSON);
        assertThat(actual.getVersion()).isEqualTo(PRODUCT_VERSION);
        assertThat(actual.getParcel()).isEqualTo(GBN_URL + "smartrouter/1.x/parcels/");
    }

    @Test
    void testGetProductShouldUseTheBuildBelongingToTheRequestedOsWhenTheOsesHaveDifferentBuilds() {
        String response = """
                {
                    "redhat8": {"gbn_url": "http://build.example.com/s3/build/1111/", "build": "1.0.0-b8"},
                    "redhat9": {"gbn_url": "http://build.example.com/s3/build/2222/", "build": "1.0.0-b9"}
                }""";
        mockLatestCompiledBuild(RELEASE_VERSION, RHEL9, response);

        ClouderaManagerProduct actual = underTest.getProduct(RHEL9);

        assertThat(actual.getVersion()).isEqualTo("1.0.0.1.0.0-b9");
        assertThat(actual.getParcel()).isEqualTo("http://build.example.com/s3/build/2222/smartrouter/1.x/parcels/");
        assertThat(actual.getCsd()).containsExactly("http://build.example.com/s3/build/2222/smartrouter/1.x/csd/SMARTROUTER-1.0.0.1.0.0-b9.jar");
    }

    @Test
    void testGetProductShouldDeriveTheMajorVersionDirectoryFromTheConfiguredReleaseVersion() {
        ReflectionTestUtils.setField(underTest, "releaseVersion", "2.3.4");
        String response = """
                {"redhat8": {"gbn_url": "http://build.example.com/s3/build/1111/", "build": "2.3.4-b1"}}""";
        mockLatestCompiledBuild("2.3.4", RHEL8, response);

        ClouderaManagerProduct actual = underTest.getProduct(RHEL8);

        assertThat(actual.getParcel()).isEqualTo("http://build.example.com/s3/build/1111/smartrouter/2.x/parcels/");
        assertThat(actual.getCsd()).containsExactly("http://build.example.com/s3/build/1111/smartrouter/2.x/csd/SMARTROUTER-2.3.4.2.3.4-b1.jar");
    }

    @ParameterizedTest(name = "latestCompiledBuildUrl={0}")
    @NullAndEmptySource
    void testGetProductShouldThrowCloudbreakServiceExceptionWhenTheLatestCompiledBuildUrlIsNotConfigured(String latestCompiledBuildUrl) {
        ReflectionTestUtils.setField(underTest, "latestCompiledBuildUrl", latestCompiledBuildUrl);

        CloudbreakServiceException actual = assertThrows(CloudbreakServiceException.class, () -> underTest.getProduct(RHEL8));

        assertThat(actual).hasMessage("No latest compiled build url provided for " + COMPONENT_NAME);
        verifyNoInteractions(restClientFactory);
    }

    @ParameterizedTest(name = "releaseVersion={0}")
    @NullAndEmptySource
    void testGetProductShouldThrowCloudbreakServiceExceptionWhenTheReleaseVersionIsNotConfigured(String releaseVersion) {
        ReflectionTestUtils.setField(underTest, "releaseVersion", releaseVersion);

        CloudbreakServiceException actual = assertThrows(CloudbreakServiceException.class, () -> underTest.getProduct(RHEL8));

        assertThat(actual).hasMessage("No version provided for " + COMPONENT_NAME);
        verifyNoInteractions(restClientFactory);
    }

    @Test
    void testGetProductShouldThrowCloudbreakServiceExceptionWhenTheLatestCompiledBuildHasNoEntryForTheRequestedOs() {
        String response = """
                {"redhat9": {"gbn_url": "http://build.example.com/s3/build/2222/", "build": "1.0.0-b9"}}""";
        mockLatestCompiledBuild(RELEASE_VERSION, RHEL8, response);

        CloudbreakServiceException actual = assertThrows(CloudbreakServiceException.class, () -> underTest.getProduct(RHEL8));

        assertThat(actual)
                .hasMessage("No latest compiled build found for " + COMPONENT_NAME + " on OS " + RHEL8)
                .hasNoCause();
    }

    @ParameterizedTest(name = "osType={0}")
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void testGetProductShouldThrowCloudbreakServiceExceptionWhenTheOsTypeIsBlank(String osType) {
        CloudbreakServiceException actual = assertThrows(CloudbreakServiceException.class, () -> underTest.getProduct(osType));

        assertThat(actual).hasMessage("No OS type provided for " + COMPONENT_NAME);
        verifyNoInteractions(restClientFactory);
    }

    @ParameterizedTest(name = "response={0}")
    @ValueSource(strings = {
            "{\"redhat8\": {\"build\": \"1.0.0-b9\"}}",
            "{\"redhat8\": {\"gbn_url\": \"\", \"build\": \"1.0.0-b9\"}}"
    })
    void testGetProductShouldThrowCloudbreakServiceExceptionWhenTheGbnUrlIsMissing(String response) {
        mockLatestCompiledBuild(RELEASE_VERSION, RHEL8, response);

        CloudbreakServiceException actual = assertThrows(CloudbreakServiceException.class, () -> underTest.getProduct(RHEL8));

        assertThat(actual)
                .hasMessage("Field 'gbn_url' is missing from the latest compiled build of " + COMPONENT_NAME + " on OS " + RHEL8)
                .hasNoCause();
    }

    @ParameterizedTest(name = "response={0}")
    @ValueSource(strings = {
            "{\"redhat8\": {\"gbn_url\": \"http://build.example.com/s3/build/1111/\"}}",
            "{\"redhat8\": {\"gbn_url\": \"http://build.example.com/s3/build/1111/\", \"build\": \" \"}}"
    })
    void testGetProductShouldThrowCloudbreakServiceExceptionWhenTheBuildIsMissing(String response) {
        mockLatestCompiledBuild(RELEASE_VERSION, RHEL8, response);

        CloudbreakServiceException actual = assertThrows(CloudbreakServiceException.class, () -> underTest.getProduct(RHEL8));

        assertThat(actual)
                .hasMessage("Field 'build' is missing from the latest compiled build of " + COMPONENT_NAME + " on OS " + RHEL8)
                .hasNoCause();
    }

    @Test
    void testGetProductShouldThrowCloudbreakServiceExceptionWhenTheLatestCompiledBuildEndpointFails() {
        ProcessingException cause = new ProcessingException("Connection refused");
        Invocation.Builder builder = mockInvocationBuilder(RELEASE_VERSION, RHEL8);
        when(builder.get(String.class)).thenThrow(cause);

        CloudbreakServiceException actual = assertThrows(CloudbreakServiceException.class, () -> underTest.getProduct(RHEL8));

        assertThat(actual)
                .hasMessage("Failed to get Smart Router component details from " + LATEST_COMPILED_BUILD_URL)
                .hasCause(cause);
    }

    @Test
    void testGetSmartRouterShouldReturnTheSmartRouterProduct() {
        ClouderaManagerProduct smartRouter = product(COMPONENT_NAME);

        Optional<ClouderaManagerProduct> actual = underTest.getSmartRouter(Set.of(product("CDH"), smartRouter));

        assertThat(actual).containsSame(smartRouter);
        verifyNoInteractions(restClientFactory);
    }

    @Test
    void testGetSmartRouterShouldMatchTheProductNameCaseInsensitively() {
        ClouderaManagerProduct smartRouter = product("SmartRouter");

        Optional<ClouderaManagerProduct> actual = underTest.getSmartRouter(Set.of(product("CDH"), smartRouter));

        assertThat(actual).containsSame(smartRouter);
    }

    @Test
    void testGetSmartRouterShouldReturnEmptyWhenThereIsNoSmartRouterAmongTheProducts() {
        assertThat(underTest.getSmartRouter(Set.of(product("CDH"), product("FLINK")))).isEmpty();
    }

    @Test
    void testGetSmartRouterShouldReturnEmptyWhenThereAreNoProducts() {
        assertThat(underTest.getSmartRouter(Set.of())).isEmpty();
    }

    private void mockLatestCompiledBuild(String releaseVersion, String osType, String response) {
        Invocation.Builder builder = mockInvocationBuilder(releaseVersion, osType);
        when(builder.get(String.class)).thenReturn(response);
    }

    private Invocation.Builder mockInvocationBuilder(String releaseVersion, String osType) {
        when(restClientFactory.getOrCreateDefault()).thenReturn(client);
        when(client.target(LATEST_COMPILED_BUILD_URL)).thenReturn(webTarget);
        when(webTarget.queryParam("stack", "smartrouter")).thenReturn(webTarget);
        when(webTarget.queryParam("release", releaseVersion)).thenReturn(webTarget);
        when(webTarget.queryParam("os", osType)).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(invocationBuilder);
        when(invocationBuilder.accept(MediaType.APPLICATION_JSON)).thenReturn(invocationBuilder);
        return invocationBuilder;
    }

    private ClouderaManagerProduct product(String name) {
        return new ClouderaManagerProduct().withName(name);
    }
}
