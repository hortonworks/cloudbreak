package com.sequenceiq.it.cloudbreak.testcase.e2e.hybrid;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;

import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.InstanceTemplateV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.wait.WaitUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.UserAuthException;
import org.springframework.beans.factory.annotation.Value;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static org.testng.Assert.fail;

public class AwsYcloudHybridCloudTest extends AbstractE2ETest {

    private static final CloudPlatform CHILD_CLOUD_PLATFORM = CloudPlatform.YARN;

    private static final String CHILD_ENVIRONMENT_CREDENTIAL_KEY = "childCred";

    private static final String CHILD_ENVIRONMENT_NETWORK_KEY = "childNetwork";

    private static final String CHILD_ENVIRONMENT_KEY = "childEnvironment";

    private static final String MOCK_UMS_USER = "mockuser";

    private static final String MOCK_UMS_PASSWORD = "Password123!";

    private static final String MOCK_UMS_PASSWORD_INVALID = "Invalid password";

    private static final String MASTER_INSTANCE_TEMPLATE_ID = "InstanceGroupTestDtomaster";

    private static final String IDBROKER_INSTANCE_TEMPLATE_ID = "InstanceGroupTestDtoidbroker";

    private static final int SSH_PORT = 22;

    private static final int SSH_CONNECT_TIMEOUT = 120000;

    private Map<String, InstanceStatus> instancesHealthy = new HashMap<>() {{
        put(HostGroupType.MASTER.getName(), InstanceStatus.SERVICES_HEALTHY);
        put(HostGroupType.IDBROKER.getName(), InstanceStatus.SERVICES_HEALTHY);
    }};

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
    private WaitUtil waitUtil;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Override
    protected void setupTest(TestContext testContext) {
        if (!CloudPlatform.AWS.name().equals(commonCloudProperties.getCloudProvider())) {
            fail(String.format("%s cloud provider is not supported for this test case!", commonCloudProperties.getCloudProvider()));
        }

        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        //Use a pre-prepared security group what allows inbound connections from ycloud
        testContext.given(EnvironmentTestDto.class)
            .withDefaultSecurityGroupId(hybridCloudSecurityGroupID)
            .withSecurityGroupIdForKnox(hybridCloudSecurityGroupID);
        createEnvironmentWithNetworkAndFreeIPA(testContext);

        testContext.given(CHILD_ENVIRONMENT_CREDENTIAL_KEY, CredentialTestDto.class, CHILD_CLOUD_PLATFORM)
            .when(credentialTestClient.create(), RunningParameter.key(CHILD_ENVIRONMENT_CREDENTIAL_KEY))
            .given(CHILD_ENVIRONMENT_NETWORK_KEY, EnvironmentNetworkTestDto.class, CHILD_CLOUD_PLATFORM)
                .withNetworkCIDR(null)
            .given(CHILD_ENVIRONMENT_KEY, EnvironmentTestDto.class, CHILD_CLOUD_PLATFORM)
                .withCredentialName(testContext.get(CHILD_ENVIRONMENT_CREDENTIAL_KEY).getName())
                .withParentEnvironment()
                .withNetwork(CHILD_ENVIRONMENT_NETWORK_KEY)
            .when(environmentTestClient.create(), RunningParameter.key(CHILD_ENVIRONMENT_KEY))
            .await(EnvironmentStatus.AVAILABLE, RunningParameter.key(CHILD_ENVIRONMENT_KEY))
            .when(environmentTestClient.describe(), RunningParameter.key(CHILD_ENVIRONMENT_KEY))
            .given(BlueprintTestDto.class, CHILD_CLOUD_PLATFORM)
                .when(blueprintTestClient.listV4());
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak with parent-child environments ",
            when = "a valid SDX create request is sent to the child environment ",
            then = "SDX is created and instances are accessible via ssh by valid username and password ",
            and = "instances are not accessible via ssh by invalid username and password"
    )
    public void testCreateSdxOnChildEnvironment(TestContext testContext) {
        String sdxInternal = resourcePropertyProvider().getName(CHILD_CLOUD_PLATFORM);

        testContext.given(InstanceTemplateV4TestDto.class, CHILD_CLOUD_PLATFORM)
                .given(MASTER_INSTANCE_TEMPLATE_ID, InstanceTemplateV4TestDto.class, CHILD_CLOUD_PLATFORM)
                .given(IDBROKER_INSTANCE_TEMPLATE_ID, InstanceTemplateV4TestDto.class, CHILD_CLOUD_PLATFORM)
                .given(sdxInternal, SdxInternalTestDto.class, CHILD_CLOUD_PLATFORM)
                    .withEnvironmentKey(RunningParameter.key(CHILD_ENVIRONMENT_KEY))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .awaitForFlow(key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING)
                .then((tc, testDto, client) -> waitUtil.waitForSdxInstancesStatus(testDto, client, instancesHealthy))
                .then((tc, dto, client) -> {
                    for (InstanceGroupV4Response ig : dto.getResponse().getStackV4Response().getInstanceGroups()) {
                        for (InstanceMetaDataV4Response i : ig.getMetadata()) {
                            String ip = i.getPublicIp();
                            testShhAuthenticationSuccessfull(ip);
                            testShhAuthenticationFailure(ip);
                        }
                    }
                    return dto;
                })
                .given(CHILD_ENVIRONMENT_KEY, EnvironmentTestDto.class, CHILD_CLOUD_PLATFORM)
                .when(environmentTestClient.forceDelete(), RunningParameter.key(CHILD_ENVIRONMENT_KEY))
                .await(EnvironmentStatus.ARCHIVED, RunningParameter.key(CHILD_ENVIRONMENT_KEY))
                .validate();
        //Cleaned up during the test case by forced environment delete
        testContext.getResources().remove(sdxInternal);
        testContext.getResources().remove(CHILD_ENVIRONMENT_KEY);
    }

    private void testShhAuthenticationSuccessfull(String host) throws IOException, UserAuthException {
        SSHClient client = getSshClient(host);
        client.authPassword(MOCK_UMS_USER, MOCK_UMS_PASSWORD);
        client.close();
    }

    private void testShhAuthenticationFailure(String host) throws IOException {
        SSHClient client = null;
        try {
            client = getSshClient(host);
            client.authPassword(MOCK_UMS_USER, MOCK_UMS_PASSWORD_INVALID);
            fail("SSH authentication passed with invalid password.");
        } catch (UserAuthException ex) {
            //Expected
        }
        if (client != null) {
            client.close();
        }
    }

    private SSHClient getSshClient(String host) throws IOException {
        SSHClient client = new SSHClient();
        client.addHostKeyVerifier(new PromiscuousVerifier());
        client.connect(host, SSH_PORT);
        client.setConnectTimeout(SSH_CONNECT_TIMEOUT);

        return client;
    }
}
