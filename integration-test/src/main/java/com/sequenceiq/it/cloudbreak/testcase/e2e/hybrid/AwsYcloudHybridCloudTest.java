package com.sequenceiq.it.cloudbreak.testcase.e2e.hybrid;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerStackDescriptorV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.SanitizerUtil;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.UserV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.EnvironmentUserSyncState;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.client.UtilTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerProductTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.SecurityGroupTestDto;
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
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
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

    private static final String MOCK_UMS_PASSWORD = "Password123!";

    private static final String MOCK_UMS_PASSWORD_INVALID = "Invalid password";

    private static final String MASTER_INSTANCE_GROUP = "master";

    private static final String IDBROKER_INSTANCE_GROUP = "idbroker";

    private static final int SSH_PORT = 22;

    private static final int SSH_CONNECT_TIMEOUT = 120000;

    private static final String CDH = "CDH";

    private static final String REDHAT7 = "redhat7";

    private static final Map<String, InstanceStatus> INSTANCES_HEALTHY = new HashMap<>() {{
        put(HostGroupType.MASTER.getName(), InstanceStatus.SERVICES_HEALTHY);
        put(HostGroupType.IDBROKER.getName(), InstanceStatus.SERVICES_HEALTHY);
    }};

    private static final String STACK_AUTHENTICATION = "stackAuthentication";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
    private UtilTestClient utilTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        checkCloudPlatform(CloudPlatform.AWS);

        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        //Use a pre-prepared security group what allows inbound connections from ycloud
        testContext
                .given(SecurityGroupTestDto.class)
                .withSecurityGroupIds(hybridCloudSecurityGroupID);
        createEnvironmentWithNetworkAndFreeIpa(testContext);

        testContext
                .given("childtelemetry", TelemetryTestDto.class)
                .withLogging(CloudPlatform.YARN)
                .withReportClusterLogs()
                .given(CHILD_ENVIRONMENT_CREDENTIAL_KEY, CredentialTestDto.class, CHILD_CLOUD_PLATFORM)
                .when(credentialTestClient.create(), RunningParameter.key(CHILD_ENVIRONMENT_CREDENTIAL_KEY))
                .given(CHILD_ENVIRONMENT_NETWORK_KEY, EnvironmentNetworkTestDto.class, CHILD_CLOUD_PLATFORM)
                .given(CHILD_ENVIRONMENT_KEY, EnvironmentTestDto.class, CHILD_CLOUD_PLATFORM)
                .withCredentialName(testContext.get(CHILD_ENVIRONMENT_CREDENTIAL_KEY).getName())
                .withParentEnvironment()
                .withNetwork(CHILD_ENVIRONMENT_NETWORK_KEY)
                .withTelemetry("childtelemetry")
                .when(environmentTestClient.create(), RunningParameter.key(CHILD_ENVIRONMENT_KEY))
                .await(EnvironmentStatus.AVAILABLE, RunningParameter.key(CHILD_ENVIRONMENT_KEY))
                .when(environmentTestClient.describe(), RunningParameter.key(CHILD_ENVIRONMENT_KEY))
                .given(BlueprintTestDto.class, CHILD_CLOUD_PLATFORM)
                .when(blueprintTestClient.listV4());
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
        String sdxInternal = resourcePropertyProvider().getName(CHILD_CLOUD_PLATFORM);
        String clouderaManager = resourcePropertyProvider().getName(CHILD_CLOUD_PLATFORM);
        String cluster = resourcePropertyProvider().getName(CHILD_CLOUD_PLATFORM);
        String cmProduct = resourcePropertyProvider().getName(CHILD_CLOUD_PLATFORM);
        String stack = resourcePropertyProvider().getName(CHILD_CLOUD_PLATFORM);

        AtomicReference<String> cdhVersion = new AtomicReference<>();
        AtomicReference<String> cdhParcel = new AtomicReference<>();
        String runtimeVersion = commonClusterManagerProperties().getRuntimeVersion();

        testContext
                .given(StackMatrixTestDto.class, CHILD_CLOUD_PLATFORM)
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
                .given(MASTER_INSTANCE_GROUP, InstanceGroupTestDto.class, CHILD_CLOUD_PLATFORM).withHostGroup(MASTER).withNodeCount(1)
                .given(IDBROKER_INSTANCE_GROUP, InstanceGroupTestDto.class, CHILD_CLOUD_PLATFORM).withHostGroup(IDBROKER).withNodeCount(1)
                .given(STACK_AUTHENTICATION, StackAuthenticationTestDto.class, CHILD_CLOUD_PLATFORM)
                .given(stack, StackTestDto.class, CHILD_CLOUD_PLATFORM).withCluster(cluster)
                    .withInstanceGroups(MASTER_INSTANCE_GROUP, IDBROKER_INSTANCE_GROUP)
                    .withStackAuthentication(STACK_AUTHENTICATION)
                    .withTelemetry("telemetry")
                .given(sdxInternal, SdxInternalTestDto.class, CHILD_CLOUD_PLATFORM)
                    .withStackRequest(key(cluster), key(stack))
                    .withEnvironmentKey(RunningParameter.key(CHILD_ENVIRONMENT_KEY))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then((tc, dto, client) -> {
                    String environmentCrn = dto.getResponse().getEnvironmentCrn();
                    com.sequenceiq.freeipa.api.client.FreeIpaClient freeIpaClient = tc.getMicroserviceClient(FreeIpaClient.class).getDefaultClient();
                    checkUserSyncState(environmentCrn, freeIpaClient);

                    String username = testContext.getActingUserCrn().getResource();
                    String sanitizedUserName = SanitizerUtil.sanitizeWorkloadUsername(username);

                    for (InstanceGroupV4Response ig : dto.getResponse().getStackV4Response().getInstanceGroups()) {
                        for (InstanceMetaDataV4Response i : ig.getMetadata()) {
                            String ip = i.getPublicIp();

                            LOGGER.info("Trying to ssh with user {} into instance: {}", sanitizedUserName, OBJECT_MAPPER.writeValueAsString(i));
                            testShhAuthenticationSuccessful(sanitizedUserName, ip);
                            testShhAuthenticationFailure(sanitizedUserName, ip);
                        }
                    }
                    return dto;
                })
                .given(CHILD_ENVIRONMENT_KEY, EnvironmentTestDto.class, CHILD_CLOUD_PLATFORM)
                .when(environmentTestClient.cascadingDelete(), RunningParameter.key(CHILD_ENVIRONMENT_KEY))
                .await(EnvironmentStatus.ARCHIVED, RunningParameter.key(CHILD_ENVIRONMENT_KEY))
                .validate();
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

    private void testShhAuthenticationSuccessful(String username, String host) {
        try (SSHClient client = getSshClient(host)) {
            client.authPassword(username, MOCK_UMS_PASSWORD);
        } catch (IOException e) {
            throw new TestFailException(String.format("Failed to ssh into host %s", host), e);
        }
    }

    private void testShhAuthenticationFailure(String username, String host) throws IOException {
        try (SSHClient client = getSshClient(host)) {
            client.authPassword(username, MOCK_UMS_PASSWORD_INVALID);
            throw new TestFailException(String.format("SSH authentication passed with invalid password on host %s.", host));
        } catch (UserAuthException ex) {
            //Expected
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
