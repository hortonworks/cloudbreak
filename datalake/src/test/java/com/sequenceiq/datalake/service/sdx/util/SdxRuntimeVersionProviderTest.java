package com.sequenceiq.datalake.service.sdx.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.BaseStackDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.BaseStackRepoDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerProductV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerV4Response;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;

class SdxRuntimeVersionProviderTest {

    private static final String DEFAULT_RUNTIME = "7.2.18";

    private static final String REPOSITORY_VERSION = "7.3.2-1.cdh7.3.2.p10000.81908621";

    private static final String CDH_PRODUCT_VERSION = "7.3.2-1.cdh7.3.2.p10000.81908621";

    private static final String BUILD_QUALIFIED_VERSION = "7.3.2.10000";

    @Test
    void getRuntimeReturnsImageVersionWhenPresent() {
        ImageV4Response image = new ImageV4Response();
        image.setVersion("7.3.1");
        SdxClusterRequest request = new SdxClusterRequest();
        request.setRuntime("7.2.17");

        String result = SdxRuntimeVersionProvider.getRuntime(image, request, stackRequestWithCdhProduct("7.2.16-x"), DEFAULT_RUNTIME);

        assertEquals("7.3.1", result);
    }

    @Test
    void getRuntimeFallsBackToRequestRuntimeWhenImageMissing() {
        SdxClusterRequest request = new SdxClusterRequest();
        request.setRuntime("7.2.17");

        String result = SdxRuntimeVersionProvider.getRuntime(null, request, stackRequestWithCdhProduct("7.2.16-x"), DEFAULT_RUNTIME);

        assertEquals("7.2.17", result);
    }

    @Test
    void getRuntimeFallsBackToCdhProductVersionLineWhenImageAndRequestMissing() {
        SdxClusterRequest request = new SdxClusterRequest();

        String result = SdxRuntimeVersionProvider.getRuntime(null, request, stackRequestWithCdhProduct(CDH_PRODUCT_VERSION), DEFAULT_RUNTIME);

        assertEquals("7.3.2", result);
    }

    @Test
    void getRuntimeFallsBackToDefaultWhenNoSourceAvailable() {
        String result = SdxRuntimeVersionProvider.getRuntime(null, new SdxClusterRequest(), new StackV4Request(), DEFAULT_RUNTIME);

        assertEquals(DEFAULT_RUNTIME, result);
    }

    @Test
    void getServicePackQualifiedRuntimeVersionFromPrewarmedImageRepositoryVersion() {
        ImageV4Response image = prewarmedImage(REPOSITORY_VERSION);

        String result = SdxRuntimeVersionProvider.getServicePackQualifiedRuntimeVersion(image, new StackV4Request(), "7.3.2");

        assertEquals(BUILD_QUALIFIED_VERSION, result);
    }

    @Test
    void getServicePackQualifiedRuntimeVersionFromBaseImageStackRequestCdhProduct() {
        String result = SdxRuntimeVersionProvider.getServicePackQualifiedRuntimeVersion(null, stackRequestWithCdhProduct(CDH_PRODUCT_VERSION), "7.3.2");

        assertEquals(BUILD_QUALIFIED_VERSION, result);
    }

    @Test
    void getServicePackQualifiedRuntimeVersionPrefersPrewarmedImageOverStackRequest() {
        ImageV4Response image = prewarmedImage("7.3.3-1.cdh7.3.3.p20000.111");

        String result = SdxRuntimeVersionProvider.getServicePackQualifiedRuntimeVersion(image, stackRequestWithCdhProduct(CDH_PRODUCT_VERSION), "7.3.3");

        assertEquals("7.3.3.20000", result);
    }

    @Test
    void getServicePackQualifiedRuntimeFallsBackToRuntimeVersionWhenNoSourceAvailable() {
        String result = SdxRuntimeVersionProvider.getServicePackQualifiedRuntimeVersion(null, new StackV4Request(), "7.3.2");

        assertEquals("7.3.2", result);
    }

    @Test
    void getServicePackQualifiedRuntimeVersionFromStackResponseCdhProduct() {
        String result = SdxRuntimeVersionProvider.getServicePackQualifiedRuntimeVersion(stackResponseWithCdhProduct(CDH_PRODUCT_VERSION), "7.3.2");

        assertEquals(BUILD_QUALIFIED_VERSION, result);
    }

    @Test
    void getServicePackQualifiedRuntimeFromStackResponseFallsBackToRuntimeVersionWhenMissing() {
        assertEquals("7.3.2", SdxRuntimeVersionProvider.getServicePackQualifiedRuntimeVersion(new StackV4Response(), "7.3.2"));
    }

    @Test
    void getRuntimeSkipsImageWhenItsVersionIsNull() {
        SdxClusterRequest request = new SdxClusterRequest();
        request.setRuntime("7.2.17");

        String result = SdxRuntimeVersionProvider.getRuntime(new ImageV4Response(), request, new StackV4Request(), DEFAULT_RUNTIME);

        assertEquals("7.2.17", result);
    }

    @Test
    void getRuntimeFallsBackToDefaultWhenStackRequestIsNull() {
        String result = SdxRuntimeVersionProvider.getRuntime(null, new SdxClusterRequest(), null, DEFAULT_RUNTIME);

        assertEquals(DEFAULT_RUNTIME, result);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("brokenImageAndStackRequestChains")
    void resolveBuildQualifiedRuntimeFromImageAndStackRequestFallsBackWhenChainHasNull(String name, ImageV4Response image, StackV4Request stackRequest) {
        assertEquals("7.3.2", SdxRuntimeVersionProvider.getServicePackQualifiedRuntimeVersion(image, stackRequest, "7.3.2"));
    }

    static Stream<Arguments> brokenImageAndStackRequestChains() {
        return Stream.of(
                Arguments.of("image null and stackRequest null", null, null),
                Arguments.of("image stackDetails null", new ImageV4Response(), new StackV4Request()),
                Arguments.of("image repository null", imageWith(new BaseStackDetailsV4Response()), new StackV4Request()),
                Arguments.of("image repository stack null", imageWith(stackDetailsWith(new BaseStackRepoDetailsV4Response())), new StackV4Request()),
                Arguments.of("image repository stack missing repository-version key", prewarmed(Map.of("other", "1")), new StackV4Request()),
                Arguments.of("image repository-version blank", prewarmed(mapWith(StackRepoDetails.REPOSITORY_VERSION, "  ")), new StackV4Request()),
                Arguments.of("image repository-version null value", prewarmed(mapWith(StackRepoDetails.REPOSITORY_VERSION, null)), new StackV4Request()),
                Arguments.of("stackRequest cluster null", null, new StackV4Request()),
                Arguments.of("stackRequest cm null", null, stackRequestWith(new ClusterV4Request())),
                Arguments.of("stackRequest products null", null, stackRequestWith(clusterWith(new ClouderaManagerV4Request()))),
                Arguments.of("stackRequest products empty", null, stackRequestWithProducts(List.of())),
                Arguments.of("stackRequest no CDH product", null, stackRequestWithProducts(List.of(productRequest("SPARK", "1.2.3-x")))),
                Arguments.of("stackRequest CDH product version null", null, stackRequestWithProducts(List.of(productRequest("CDH", null)))),
                Arguments.of("stackRequest CDH product version blank", null, stackRequestWithProducts(List.of(productRequest("CDH", "  "))))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("brokenStackResponseChains")
    void resolveBuildQualifiedRuntimeFromStackResponseFallsBackWhenChainHasNull(String name, StackV4Response stackResponse) {
        assertEquals("7.3.2", SdxRuntimeVersionProvider.getServicePackQualifiedRuntimeVersion(stackResponse, "7.3.2"));
    }

    static Stream<Arguments> brokenStackResponseChains() {
        return Stream.of(
                Arguments.of("stackResponse null", null),
                Arguments.of("stackResponse cluster null", new StackV4Response()),
                Arguments.of("stackResponse cm null", stackResponseWith(new ClusterV4Response())),
                Arguments.of("stackResponse products null", stackResponseWith(clusterResponseWith(new ClouderaManagerV4Response()))),
                Arguments.of("stackResponse products empty", stackResponseWithProducts(List.of())),
                Arguments.of("stackResponse no CDH product", stackResponseWithProducts(List.of(productResponse("SPARK", "1.2.3-x")))),
                Arguments.of("stackResponse CDH product version null", stackResponseWithProducts(List.of(productResponse("CDH", null)))),
                Arguments.of("stackResponse CDH product version blank", stackResponseWithProducts(List.of(productResponse("CDH", "  "))))
        );
    }

    private static Map<String, String> mapWith(String key, String value) {
        Map<String, String> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    private static ImageV4Response prewarmed(Map<String, String> stack) {
        BaseStackRepoDetailsV4Response repository = new BaseStackRepoDetailsV4Response();
        repository.setStack(stack);
        return imageWith(stackDetailsWith(repository));
    }

    private static BaseStackDetailsV4Response stackDetailsWith(BaseStackRepoDetailsV4Response repository) {
        BaseStackDetailsV4Response stackDetails = new BaseStackDetailsV4Response();
        stackDetails.setRepository(repository);
        return stackDetails;
    }

    private static ImageV4Response imageWith(BaseStackDetailsV4Response stackDetails) {
        ImageV4Response image = new ImageV4Response();
        image.setStackDetails(stackDetails);
        return image;
    }

    private static ClusterV4Request clusterWith(ClouderaManagerV4Request cm) {
        ClusterV4Request cluster = new ClusterV4Request();
        cluster.setCm(cm);
        return cluster;
    }

    private static StackV4Request stackRequestWith(ClusterV4Request cluster) {
        StackV4Request stackRequest = new StackV4Request();
        stackRequest.setCluster(cluster);
        return stackRequest;
    }

    private static StackV4Request stackRequestWithProducts(List<ClouderaManagerProductV4Request> products) {
        ClouderaManagerV4Request cm = new ClouderaManagerV4Request();
        cm.setProducts(products);
        return stackRequestWith(clusterWith(cm));
    }

    private static ClouderaManagerProductV4Request productRequest(String name, String version) {
        ClouderaManagerProductV4Request product = new ClouderaManagerProductV4Request();
        product.setName(name);
        product.setVersion(version);
        return product;
    }

    private static ClusterV4Response clusterResponseWith(ClouderaManagerV4Response cm) {
        ClusterV4Response cluster = new ClusterV4Response();
        cluster.setCm(cm);
        return cluster;
    }

    private static StackV4Response stackResponseWith(ClusterV4Response cluster) {
        StackV4Response stackResponse = new StackV4Response();
        stackResponse.setCluster(cluster);
        return stackResponse;
    }

    private static StackV4Response stackResponseWithProducts(List<ClouderaManagerProductV4Response> products) {
        ClouderaManagerV4Response cm = new ClouderaManagerV4Response();
        cm.setProducts(products);
        return stackResponseWith(clusterResponseWith(cm));
    }

    private static ClouderaManagerProductV4Response productResponse(String name, String version) {
        ClouderaManagerProductV4Response product = new ClouderaManagerProductV4Response();
        product.setName(name);
        product.setVersion(version);
        return product;
    }

    private ImageV4Response prewarmedImage(String repositoryVersion) {
        BaseStackRepoDetailsV4Response repository = new BaseStackRepoDetailsV4Response();
        repository.setStack(Map.of(StackRepoDetails.REPOSITORY_VERSION, repositoryVersion));
        BaseStackDetailsV4Response stackDetails = new BaseStackDetailsV4Response();
        stackDetails.setRepository(repository);
        ImageV4Response image = new ImageV4Response();
        image.setStackDetails(stackDetails);
        return image;
    }

    private StackV4Request stackRequestWithCdhProduct(String version) {
        ClouderaManagerProductV4Request product = new ClouderaManagerProductV4Request();
        product.setName("CDH");
        product.setVersion(version);
        ClouderaManagerV4Request cm = new ClouderaManagerV4Request();
        cm.setProducts(List.of(product));
        ClusterV4Request cluster = new ClusterV4Request();
        cluster.setCm(cm);
        StackV4Request stackRequest = new StackV4Request();
        stackRequest.setCluster(cluster);
        return stackRequest;
    }

    private StackV4Response stackResponseWithCdhProduct(String version) {
        ClouderaManagerProductV4Response product = new ClouderaManagerProductV4Response();
        product.setName("CDH");
        product.setVersion(version);
        ClouderaManagerV4Response cm = new ClouderaManagerV4Response();
        cm.setProducts(List.of(product));
        ClusterV4Response cluster = new ClusterV4Response();
        cluster.setCm(cm);
        StackV4Response stackResponse = new StackV4Response();
        stackResponse.setCluster(cluster);
        return stackResponse;
    }
}
