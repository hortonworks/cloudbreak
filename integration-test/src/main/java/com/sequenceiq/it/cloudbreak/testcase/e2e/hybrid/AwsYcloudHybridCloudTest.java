package com.sequenceiq.it.cloudbreak.testcase.e2e.hybrid;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerStackDescriptorV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.UserV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.EnvironmentUserSyncState;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.client.UtilTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerProductTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.StackAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.StackMatrixTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.cloudbreak.util.ssh.action.ScpDownloadClusterLogsActions;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.UserAuthException;

public class AwsYcloudHybridCloudTest extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsYcloudHybridCloudTest.class);

    private static final CloudPlatform CHILD_CLOUD_PLATFORM = CloudPlatform.YARN;

    private static final String CHILD_ENVIRONMENT_CREDENTIAL_KEY = "childCred";

    private static final String CHILD_ENVIRONMENT_NETWORK_KEY = "childNetwork";

    private static final String CHILD_ENVIRONMENT_KEY = "childEnvironment";

    private static final String CHILD_SDX_KEY = "childDataLake";

    private static final String MOCK_UMS_PASSWORD_INVALID = "Invalid password";

    private static final String MASTER_INSTANCE_GROUP = "master";

    private static final String IDBROKER_INSTANCE_GROUP = "idbroker";

    private static final int SSH_PORT = 22;

    private static final int SSH_CONNECT_TIMEOUT = 120000;

    private static final String CDH = "CDH";

    private static final String CENTOS7 = "centos7";

    private static final String REDHAT7 = "redhat7";

    private static final String STACK_AUTHENTICATION = "stackAuthentication";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private String sdxGatewayPrivateIp;

    @Value("${integrationtest.aws.hybridCloudSecurityGroupID}")
    private String hybridCloudSecurityGroupID;

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

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak with parent-child environments ",
            when = "a valid SDX create request is sent to the child environment ",
            then = "SDX is created and instances are accessible via ssh by valid username and password ",
            and = "instances are not accessible via ssh by invalid username and password"
    )
    public void testCreateSdxOnChildEnvironment(TestContext testContext) {
        String clouderaManager = resourcePropertyProvider().getName(CHILD_CLOUD_PLATFORM);
        String cluster = resourcePropertyProvider().getName(CHILD_CLOUD_PLATFORM);
        String cmProduct = resourcePropertyProvider().getName(CHILD_CLOUD_PLATFORM);
        String stack = resourcePropertyProvider().getName(CHILD_CLOUD_PLATFORM);

        AtomicReference<String> cdhVersion = new AtomicReference<>();
        AtomicReference<String> cdhParcel = new AtomicReference<>();
        String runtimeVersion = commonClusterManagerProperties().getRuntimeVersion();

        testContext
                .given(StackMatrixTestDto.class, CHILD_CLOUD_PLATFORM)
                    .withOs(CENTOS7)
                .when(utilTestClient.stackMatrixV4())
                .then((tc, dto, client) -> {
                    ClouderaManagerStackDescriptorV4Response response = dto.getResponse().getCdh().get(runtimeVersion);
                    cdhVersion.set(response.getVersion());
                    cdhParcel.set(response.getRepository().getStack().get(REDHAT7));
                    return dto;
                })
                .validate();

        testContext
                .given("telemetry", TelemetryTestDto.class)
                    .withLogging(CHILD_CLOUD_PLATFORM)
                    .withReportClusterLogs()
                .given(cmProduct, ClouderaManagerProductTestDto.class, CHILD_CLOUD_PLATFORM)
                    .withName(CDH)
                    .withVersion(cdhVersion.get())
                    .withParcel(cdhParcel.get())
                .given(clouderaManager, ClouderaManagerTestDto.class, CHILD_CLOUD_PLATFORM)
                    .withClouderaManagerProduct(cmProduct)
                .given(cluster, ClusterTestDto.class, CHILD_CLOUD_PLATFORM)
                    .withBlueprintName(getDefaultSDXBlueprintName())
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
                .then((tc, dto, client) -> {
                    String environmentCrn = dto.getResponse().getEnvironmentCrn();
                    com.sequenceiq.freeipa.api.client.FreeIpaClient freeIpaClient = tc.getMicroserviceClient(FreeIpaClient.class).getDefaultClient();
                    checkUserSyncState(environmentCrn, freeIpaClient);

                    for (InstanceGroupV4Response ig : dto.getResponse().getStackV4Response().getInstanceGroups()) {
                        for (InstanceMetaDataV4Response i : ig.getMetadata()) {
                            String ip = i.getPublicIp();

                            LOGGER.info("Trying to ssh with user {} into instance: {}", tc.getWorkloadUserName(), OBJECT_MAPPER.writeValueAsString(i));
                            testShhAuthenticationSuccessful(tc, ip);
                            testShhAuthenticationFailure(tc, ip);
                        }
                    }
                    return dto;
                })
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

        LOGGER.info("Terminating the child environment with cascading delete...");
        testContext
                .given(CHILD_ENVIRONMENT_KEY, EnvironmentTestDto.class, CHILD_CLOUD_PLATFORM)
                    .withCredentialName(testContext.get(CHILD_ENVIRONMENT_CREDENTIAL_KEY).getName())
                    .withParentEnvironment()
                .when(environmentTestClient.cascadingDelete(), key(CHILD_ENVIRONMENT_KEY).withSkipOnFail(Boolean.FALSE))
                .await(EnvironmentStatus.ARCHIVED, key(CHILD_ENVIRONMENT_KEY).withSkipOnFail(Boolean.FALSE));
        LOGGER.info("Child environment has been deleted!");

        testContext.cleanupTestContext();
    }

    private String getDefaultSDXBlueprintName() {
        return commonClusterManagerProperties().getInternalSdxBlueprintName();
    }

    private void checkUserSyncState(String environmentCrn, com.sequenceiq.freeipa.api.client.FreeIpaClient freeIpaClient) throws JsonProcessingException {
        UserV1Endpoint userV1Endpoint = freeIpaClient.getUserV1Endpoint();
        EnvironmentUserSyncState userSyncState = userV1Endpoint.getUserSyncState(environmentCrn);
        SyncOperationStatus syncOperationStatus = userV1Endpoint.getSyncOperationStatus(userSyncState.getLastUserSyncOperationId());
        LOGGER.info("Last user sync is in state {}, last operation: {}", userSyncState.getState(), OBJECT_MAPPER.writeValueAsString(syncOperationStatus));
    }

    private void testShhAuthenticationSuccessful(TestContext testContext, String host) {
        try (SSHClient client = getSshClient(host)) {
            client.authPassword(testContext.getWorkloadUserName(), testContext.getWorkloadPassword());
        } catch (IOException e) {
            throw new TestFailException(String.format("Failed to ssh into host %s", host), e);
        }
    }

    private void testShhAuthenticationFailure(TestContext testContext, String host) throws IOException {
        try (SSHClient client = getSshClient(host)) {
            client.authPassword(testContext.getWorkloadUserName(), MOCK_UMS_PASSWORD_INVALID);
            throw new TestFailException(String.format("SSH authentication passed with invalid password on host %s.", host));
        } catch (UserAuthException ex) {
            LOGGER.info("Expected: SSH authentication failure has been happend!");
        }
    }

    private SSHClient getSshClient(String host) throws IOException {
        SSHClient client = new SSHClient();
        client.addHostKeyVerifier(new PromiscuousVerifier());
        client.setConnectTimeout(SSH_CONNECT_TIMEOUT);
        client.connect(host, SSH_PORT);
        return client;
    }
}
