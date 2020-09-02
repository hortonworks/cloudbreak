package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static java.util.Objects.isNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerStackDescriptorV4Response;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.client.UtilTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerProductTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerRepositoryTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.StackMatrixTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxInternalClusterRequest;

public class YcloudHybridCloudTest extends AbstractIntegrationTest {

    private static final String CHILD_ENVIRONMENT = "childEnvironment";

    private static final String MASTER_INSTANCE_GROUP = "master";

    private static final String IDBROKER_INSTANCE_GROUP = "idbroker";

    private static final String CDH = "CDH";

    private static final String REDHAT7 = "redhat7";

    private static final String CM_VERSION_KEY = "cmVersion";

    private static final String CM_REPOSITORY_BASE_URL_KEY = "cmRepositoryBaseUrl";

    private static final String CDH_VERSION_KEY = "cdhVersion";

    private static final String CDH_PARCEL_KEY = "cdhParcel";

    private final Map<String, String> validationParameters = new HashMap<>();

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private UtilTestClient utilTestClient;

    protected void setupTest(MockedTestContext testContext) {
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithNetworkAndFreeIpa(testContext);
        createDefaultImageCatalog(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there are an available parent and child environments",
            when = "a valid SDX create request is sent to the child environment",
            then = "environment should be created and parent environment should be referenced in the child environment",
            and = "SDX is created with the proper information")
    public void testCreateSdxOnChildEnvironment(MockedTestContext testContext) {
        validateParentEnvironment(testContext);
        createAndValidateChildEnvironment(testContext);
        createAndValidateSdx(testContext);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(Object[] data) {
        TestContext testContext = (TestContext) data[0];

        testContext
                .given(CHILD_ENVIRONMENT, EnvironmentTestDto.class)
                .when(environmentTestClient.cascadingDelete(), RunningParameter.key(CHILD_ENVIRONMENT))
                .await(EnvironmentStatus.ARCHIVED, RunningParameter.key(CHILD_ENVIRONMENT))
                .validate();

        testContext.cleanupTestContext();
    }

    private void validateParentEnvironment(TestContext testContext) {
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.list())
                .then(this::checkEnvIsListedByNameAndParentName);
    }

    private void createAndValidateChildEnvironment(TestContext testContext)  {
        testContext
                .given(CHILD_ENVIRONMENT, EnvironmentTestDto.class)
                    .withParentEnvironment()
                .when(environmentTestClient.create(), RunningParameter.key(CHILD_ENVIRONMENT))
                .await(EnvironmentStatus.AVAILABLE, RunningParameter.key(CHILD_ENVIRONMENT))
                .when(environmentTestClient.describe(), RunningParameter.key(CHILD_ENVIRONMENT))
                .when(environmentTestClient.list())
                .then(this::checkEnvIsListedByNameAndParentName)
                .validate();
    }

    private void createAndValidateSdx(TestContext testContext) {
        String sdxInternal = resourcePropertyProvider().getName();
        String clouderaManager = resourcePropertyProvider().getName();
        String cluster = resourcePropertyProvider().getName();
        String cmProduct = resourcePropertyProvider().getName();
        String cmRepository = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();

        testContext
                .given(StackMatrixTestDto.class)
                .when(utilTestClient.stackMatrixV4())
                .then((tc, dto, client) -> {
                    ClouderaManagerStackDescriptorV4Response response = dto.getResponse().getCdh().get(commonClusterManagerProperties().getRuntimeVersion());
                    validationParameters.put(CM_VERSION_KEY, response.getClouderaManager().getVersion());
                    validationParameters.put(CM_REPOSITORY_BASE_URL_KEY, response.getClouderaManager().getRepository().get(REDHAT7).getBaseUrl());
                    validationParameters.put(CDH_VERSION_KEY, response.getVersion());
                    validationParameters.put(CDH_PARCEL_KEY, response.getRepository().getStack().get(REDHAT7));
                    return dto;
                })
                .validate();

        testContext
                .given(cmProduct, ClouderaManagerProductTestDto.class)
                    .withName(CDH)
                    .withVersion(validationParameters.get(CDH_VERSION_KEY))
                    .withParcel(validationParameters.get(CDH_PARCEL_KEY))
                .given(cmRepository, ClouderaManagerRepositoryTestDto.class)
                    .withVersion(validationParameters.get(CM_VERSION_KEY))
                    .withBaseUrl(validationParameters.get(CM_REPOSITORY_BASE_URL_KEY))
                .given(clouderaManager, ClouderaManagerTestDto.class)
                    .withClouderaManagerProduct(cmProduct)
                    .withClouderaManagerRepository(cmRepository)
                .given(cluster, ClusterTestDto.class)
                    .withBlueprintName(getDefaultSDXBlueprintName())
                    .withValidateBlueprint(Boolean.FALSE)
                    .withClouderaManager(clouderaManager)
                .given(MASTER_INSTANCE_GROUP, InstanceGroupTestDto.class).withHostGroup(MASTER).withNodeCount(1)
                .given(IDBROKER_INSTANCE_GROUP, InstanceGroupTestDto.class).withHostGroup(IDBROKER).withNodeCount(1)
                .given(stack, StackTestDto.class)
                    .withCluster(cluster)
                    .withInstanceGroups(MASTER_INSTANCE_GROUP, IDBROKER_INSTANCE_GROUP)
                .given(sdxInternal, SdxInternalTestDto.class)
                    .withStackRequest(key(cluster), key(stack))
                    .withEnvironmentKey(RunningParameter.key(CHILD_ENVIRONMENT))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .awaitForFlow(key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING)
                .then(this::validateRequestParameters)
                .validate();
    }

    private String getDefaultSDXBlueprintName() {
        return commonClusterManagerProperties().getInternalSdxBlueprintName();
    }

    private EnvironmentTestDto checkEnvIsListedByNameAndParentName(TestContext testContext,
            EnvironmentTestDto environment,
            EnvironmentClient environmentClient) {
        Collection<SimpleEnvironmentResponse> simpleEnvironmentV4Response = environment.getResponseSimpleEnvSet();
        if (isNull(simpleEnvironmentV4Response)) {
            throw new TestFailException("Environment list response is missing.");
        }
        boolean listed = simpleEnvironmentV4Response.stream()
                .anyMatch(environmentResponse -> nameEquals(environment, environmentResponse) && parentNameEquals(environment, environmentResponse));
        if (!listed) {
            throw new TestFailException("Environment is not listed");
        }
        return environment;
    }

    private boolean nameEquals(EnvironmentTestDto environment, SimpleEnvironmentResponse environmentResponse) {
        return environment.getName().equals(environmentResponse.getName());
    }

    private boolean parentNameEquals(EnvironmentTestDto environment, SimpleEnvironmentResponse environmentResponse) {
        return isNull(environment.getParentEnvironmentName()) && isNull(environmentResponse.getParentEnvironmentName()) ||
                environment.getParentEnvironmentName().equals(environmentResponse.getParentEnvironmentName());
    }

    private SdxInternalTestDto validateRequestParameters(TestContext tc, SdxInternalTestDto dto, SdxClient client) {
        assertNotNull(dto);
        SdxInternalClusterRequest sdxRequest = dto.getRequest();
        assertNotNull(sdxRequest);
        StackV4Request stackRequest = sdxRequest.getStackV4Request();
        assertNotNull(stackRequest);
        ClusterV4Request clusterRequest = stackRequest.getCluster();
        assertNotNull(clusterRequest);
        ClouderaManagerV4Request cmRequest = clusterRequest.getCm();
        assertNotNull(cmRequest);

        validateInstanceGroups(stackRequest.getInstanceGroups());
        validateRepository(cmRequest.getRepository());
        validateProducts(cmRequest.getProducts());

        return dto;
    }

    private void validateInstanceGroups(List<InstanceGroupV4Request> instanceGroups) {
        assertNotNull(instanceGroups);
        assertEquals("The result instance group list has " + instanceGroups.size() + " items instead of 2", 2, instanceGroups.size());
        Set<String> groups = instanceGroups.stream()
                .map(InstanceGroupV4Request::getName)
                .collect(Collectors.toSet());
        assertTrue(groups.contains(MASTER_INSTANCE_GROUP) && groups.contains(IDBROKER_INSTANCE_GROUP));
    }

    private void validateRepository(ClouderaManagerRepositoryV4Request repository) {
        assertNotNull(repository);
        assertEquals(validationParameters.get(CM_VERSION_KEY), repository.getVersion());
        assertEquals(validationParameters.get(CM_REPOSITORY_BASE_URL_KEY), repository.getBaseUrl());
    }

    private void validateProducts(List<ClouderaManagerProductV4Request> products) {
        assertNotNull(products);
        assertEquals("The result product list has " + products.size() + " items instead of 1", 1, products.size());
        assertEquals(CDH, products.get(0).getName());
        assertEquals(validationParameters.get(CDH_VERSION_KEY), products.get(0).getVersion());
        assertEquals(validationParameters.get(CDH_PARCEL_KEY), products.get(0).getParcel());
    }
}
