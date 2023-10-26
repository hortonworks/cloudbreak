package com.sequenceiq.it.cloudbreak.testcase.e2e.hybrid;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.testng.annotations.AfterMethod;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerStackDescriptorV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.client.UtilTestClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerProductTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.StackAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.clouderamanager.DistroXClouderaManagerProductTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.clouderamanager.DistroXClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.StackMatrixTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.ssh.action.ScpDownloadClusterLogsActions;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public abstract class HybridCloudE2ETest extends AbstractE2ETest {

    protected static final CloudPlatform CHILD_CLOUD_PLATFORM = CloudPlatform.YARN;

    protected static final String CHILD_ENVIRONMENT_CREDENTIAL_KEY = "childCred";

    protected static final String CHILD_ENVIRONMENT_NETWORK_KEY = "childNetwork";

    protected static final String CHILD_ENVIRONMENT_KEY = "childEnvironment";

    protected static final String CHILD_SDX_KEY = "childDataLake";

    protected static final String CHILD_DISTROX_KEY = "childDataHub";

    private static final Logger LOGGER = LoggerFactory.getLogger(HybridCloudE2ETest.class);

    private static final String MASTER_INSTANCE_GROUP = "master";

    private static final String IDBROKER_INSTANCE_GROUP = "idbroker";

    private static final String CDH = "CDH";

    private static final String CENTOS7 = "centos7";

    private static final String REDHAT7 = "redhat7";

    private static final String STACK_AUTHENTICATION = "stackAuthentication";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private ScpDownloadClusterLogsActions yarnClusterLogs;

    @Inject
    private UtilTestClient utilTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Value("${integrationtest.aws.hybridCloudSecurityGroupID}")
    private String hybridCloudSecurityGroupID;

    private String sdxGatewayPrivateIp;

    private String cdhVersion;

    private String cdhParcel;

    @Override
    protected void setupTest(TestContext testContext) {
        assertSupportedCloudPlatform(CloudPlatform.AWS);

        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        //Use a pre-prepared security group what allows inbound connections from ycloud
        testContext
                .given(EnvironmentTestDto.class)
                    .withDefaultSecurityGroup(hybridCloudSecurityGroupID);
        createEnvironmentWithFreeIpa(testContext);

        testContext
                .given("childtelemetry", TelemetryTestDto.class)
                    .withLogging(CHILD_CLOUD_PLATFORM)
                    .withReportClusterLogs()
                .given(CHILD_ENVIRONMENT_CREDENTIAL_KEY, CredentialTestDto.class, CHILD_CLOUD_PLATFORM)
                .when(credentialTestClient.create(), key(CHILD_ENVIRONMENT_CREDENTIAL_KEY))
                .given(CHILD_ENVIRONMENT_NETWORK_KEY, EnvironmentNetworkTestDto.class, CHILD_CLOUD_PLATFORM)
                .given(CHILD_ENVIRONMENT_KEY, EnvironmentTestDto.class, CHILD_CLOUD_PLATFORM)
                    .withCredentialName(testContext.get(CHILD_ENVIRONMENT_CREDENTIAL_KEY).getName())
                    .withParentEnvironment()
                    .withNetwork(CHILD_ENVIRONMENT_NETWORK_KEY)
                    .withTelemetry("childtelemetry")
                .when(environmentTestClient.create(), key(CHILD_ENVIRONMENT_KEY))
                .await(EnvironmentStatus.AVAILABLE, key(CHILD_ENVIRONMENT_KEY))
                .when(environmentTestClient.describe(), key(CHILD_ENVIRONMENT_KEY))
                .given(BlueprintTestDto.class, CHILD_CLOUD_PLATFORM)
                .when(blueprintTestClient.listV4())
                .validate();
    }

    protected void createChildDatalake(TestContext testContext) {
        String clouderaManager = resourcePropertyProvider().getName(CHILD_CLOUD_PLATFORM);
        String cluster = resourcePropertyProvider().getName(CHILD_CLOUD_PLATFORM);
        String cmProduct = resourcePropertyProvider().getName(CHILD_CLOUD_PLATFORM);
        String stack = resourcePropertyProvider().getName(CHILD_CLOUD_PLATFORM);
        fetchCdhDetails(testContext);

        testContext
                .given("telemetry", TelemetryTestDto.class)
                    .withLogging(CHILD_CLOUD_PLATFORM)
                    .withReportClusterLogs()
                .given(cmProduct, ClouderaManagerProductTestDto.class, CHILD_CLOUD_PLATFORM)
                    .withName(CDH)
                    .withVersion(cdhVersion)
                    .withParcel(cdhParcel)
                .given(clouderaManager, ClouderaManagerTestDto.class, CHILD_CLOUD_PLATFORM)
                    .withClouderaManagerProduct(cmProduct)
                .given(cluster, ClusterTestDto.class, CHILD_CLOUD_PLATFORM)
                    .withBlueprintName(commonClusterManagerProperties().getInternalSdxBlueprintName())
                    .withValidateBlueprint(Boolean.FALSE)
                    .withClouderaManager(clouderaManager)
                .given(MASTER_INSTANCE_GROUP, InstanceGroupTestDto.class, CHILD_CLOUD_PLATFORM)
                    .withHostGroup(MASTER)
                    .withNodeCount(1)
                .given(IDBROKER_INSTANCE_GROUP, InstanceGroupTestDto.class, CHILD_CLOUD_PLATFORM)
                    .withHostGroup(IDBROKER)
                    .withNodeCount(1)
                .given(STACK_AUTHENTICATION, StackAuthenticationTestDto.class, CHILD_CLOUD_PLATFORM)
                .given(stack, StackTestDto.class, CHILD_CLOUD_PLATFORM)
                    .withCluster(cluster)
                    .withInstanceGroups(MASTER_INSTANCE_GROUP, IDBROKER_INSTANCE_GROUP)
                    .withStackAuthentication(STACK_AUTHENTICATION)
                    .withTelemetry("telemetry")
                .given(CHILD_SDX_KEY, SdxInternalTestDto.class, CHILD_CLOUD_PLATFORM)
                    .withStackRequest(key(cluster), key(stack))
                    .withEnvironmentKey(key(CHILD_ENVIRONMENT_KEY))
                .when(sdxTestClient.createInternal(), key(CHILD_SDX_KEY))
                .await(SdxClusterStatusResponse.STACK_CREATION_IN_PROGRESS, key(CHILD_SDX_KEY).withoutWaitForFlow())
                .then((tc, dto, client) -> {
                    sdxGatewayPrivateIp = dto.awaitForPrivateIp(tc, client, MASTER.getName());
                    LOGGER.info("SDX master instance IP: {}", sdxGatewayPrivateIp);
                    return dto;
                })
                .await(SdxClusterStatusResponse.RUNNING, key(CHILD_SDX_KEY).withWaitForFlow(Boolean.TRUE))
                .awaitForHealthyInstances()
                .validate();
    }

    private void fetchCdhDetails(TestContext testContext) {
        if (StringUtils.isAnyBlank(cdhVersion, cdhParcel)) {
            testContext
                    .given(StackMatrixTestDto.class, CHILD_CLOUD_PLATFORM)
                        .withOs(CENTOS7)
                    .when(utilTestClient.stackMatrixV4())
                    .then((tc, dto, client) -> {
                        String runtimeVersion = commonClusterManagerProperties().getRuntimeVersion();
                        ClouderaManagerStackDescriptorV4Response response = dto.getResponse().getCdh().get(runtimeVersion);
                        cdhVersion = response.getVersion();
                        cdhParcel = response.getRepository().getStack().get(REDHAT7);
                        return dto;
                    })
                    .validate();
        }
    }

    protected void createChildDatahubForExistingDatalake(TestContext testContext) {
        String clouderaManager = resourcePropertyProvider().getName(CHILD_CLOUD_PLATFORM);
        String cluster = resourcePropertyProvider().getName(CHILD_CLOUD_PLATFORM);
        String cmProduct = resourcePropertyProvider().getName(CHILD_CLOUD_PLATFORM);
        fetchCdhDetails(testContext);

        testContext
                .given(cmProduct, DistroXClouderaManagerProductTestDto.class, CHILD_CLOUD_PLATFORM)
                    .withName(CDH)
                    .withVersion(cdhVersion)
                    .withParcel(cdhParcel)
                .given(clouderaManager, DistroXClouderaManagerTestDto.class, CHILD_CLOUD_PLATFORM)
                    .withClouderaManagerProduct(cmProduct)
                .given(cluster, DistroXClusterTestDto.class, CHILD_CLOUD_PLATFORM)
                    .withBlueprintName(commonClusterManagerProperties().getInternalDistroXBlueprintName())
                    .withValidateBlueprint(Boolean.FALSE)
                    .withClouderaManager(clouderaManager)
                .given(CHILD_DISTROX_KEY, DistroXTestDto.class, CHILD_CLOUD_PLATFORM)
                    .withEnvironmentKey(CHILD_ENVIRONMENT_KEY)
                    .withCluster(cluster)
                .when(distroXTestClient.create(), key(CHILD_DISTROX_KEY))
                .await(STACK_AVAILABLE, key(CHILD_DISTROX_KEY))
                .awaitForHealthyInstances()
                .when(distroXTestClient.get(), key(CHILD_DISTROX_KEY))
                .validate();
    }

    /**
     * Helper method to speed up local testing. Could be invoked to re-use an already existing environment with the given name
     */
    protected void useExistingChildEnvironment(TestContext testContext, String environmentName) {
        testContext
                .given(CHILD_ENVIRONMENT_KEY, EnvironmentTestDto.class, CHILD_CLOUD_PLATFORM)
                .withName(environmentName)
                .when(environmentTestClient.describe())
                .validate();
    }

    /**
     * Helper method to speed up local testing. Could be invoked to re-use an already existing datalake with the given name
     */
    protected void useExistingChildDatalake(TestContext testContext, String datalakeName) {
        testContext
                .given(CHILD_SDX_KEY, SdxInternalTestDto.class, CHILD_CLOUD_PLATFORM)
                .withName(datalakeName)
                .when(sdxTestClient.describeInternal())
                .validate();
    }

    @Override
    @AfterMethod(alwaysRun = true)
    public void tearDown(Object[] data) {
        TestContext testContext = (TestContext) data[0];

        LOGGER.info("Tear down AWS-YCloud E2E context");

        if (StringUtils.isBlank(sdxGatewayPrivateIp)) {
            LOGGER.warn("Error occured while creating SDX stack! So cannot download cluster logs from SDX.");
        } else {
            try {
                SdxInternalTestDto sdxInternalTestDto = testContext.get(CHILD_SDX_KEY);
                String environmentCrnSdx = sdxInternalTestDto.getResponse().getEnvironmentCrn();
                String sdxName = sdxInternalTestDto.getResponse().getName();
                LOGGER.info("Downloading YCloud SDX logs...");
                yarnClusterLogs.downloadClusterLogs(environmentCrnSdx, sdxName, sdxGatewayPrivateIp, "sdx");
                LOGGER.info("YCloud SDX logs have been downloaded!");
            } catch (Exception sdxError) {
                LOGGER.warn("Error occured while downloading SDX logs!", sdxError);
            }
        }

        if (testContext.shouldCleanUp()) {
            LOGGER.info("Terminating the child environment with cascading delete...");
            testContext
                    .given(CHILD_ENVIRONMENT_KEY, EnvironmentTestDto.class, CHILD_CLOUD_PLATFORM)
                        .withCredentialName(testContext.get(CHILD_ENVIRONMENT_CREDENTIAL_KEY).getName())
                        .withParentEnvironment()
                    .when(environmentTestClient.cascadingDelete(), key(CHILD_ENVIRONMENT_KEY).withSkipOnFail(Boolean.FALSE))
                    .await(EnvironmentStatus.ARCHIVED, key(CHILD_ENVIRONMENT_KEY).withSkipOnFail(Boolean.FALSE));
            LOGGER.info("Child environment has been deleted!");
        }

        testContext.cleanupTestContext();
    }
}
