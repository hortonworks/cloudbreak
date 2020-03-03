package com.sequenceiq.it.cloudbreak.testcase.e2e.hybrid;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceTemplateTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.ImageValidatorE2ETest;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.UserAuthException;
import org.springframework.beans.factory.annotation.Value;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;

import static org.testng.Assert.fail;

public class AwsYcloudHybridCloudTest extends ImageValidatorE2ETest {

    private static final CloudPlatform CHILD_CLOUD_PLATFORM = CloudPlatform.YARN;

    private static final String CHILD_ENVIRONMENT_CREDENTIAL_KEY = "childCred";

    private static final String CHILD_ENVIRONMENT_NETWORK_KEY = "childNetwork";

    private static final String CHILD_ENVIRONMENT_KEY = "childEnvironment";

    private static final String MOCK_UMS_USER = "mockuser";

    private static final String MOCK_UMS_PASSWORD = "Password123!";

    private static final String MOCK_UMS_PASSWORD_INVALID = "Invalid password";

    private static final int SSH_PORT = 22;

    private static final int SSH_CONNECT_TIMEOUT = 120000;

    @Value("${integrationtest.aws.hybridCloudSecurityGroupID}")
    private String hybridCloudSecurityGroupID;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();

        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        //Use a pre-prepared security group which allows inbound connections from ycloud
        testContext.given(EnvironmentTestDto.class)
            .withDefaultSecurityGroupId(hybridCloudSecurityGroupID)
            .withSecurityGroupIdForKnox(hybridCloudSecurityGroupID);
        createEnvironmentWithNetworkAndFreeIPA(testContext);

        testContext.given(CHILD_ENVIRONMENT_CREDENTIAL_KEY, CredentialTestDto.class, CHILD_CLOUD_PLATFORM)
            .when(credentialTestClient.create())
            .given(CHILD_ENVIRONMENT_NETWORK_KEY, EnvironmentNetworkTestDto.class, CHILD_CLOUD_PLATFORM)
            .given(CHILD_ENVIRONMENT_KEY, EnvironmentTestDto.class, CHILD_CLOUD_PLATFORM)
                .withCredentialName(testContext.get(CHILD_ENVIRONMENT_CREDENTIAL_KEY).getName())
                .withParentEnvironment()
                .withNetwork(CHILD_ENVIRONMENT_NETWORK_KEY)
            .when(environmentTestClient.create())
            .await(EnvironmentStatus.AVAILABLE)
            .when(environmentTestClient.describe(), RunningParameter.key(CHILD_ENVIRONMENT_KEY))
            .given(BlueprintTestDto.class, CHILD_CLOUD_PLATFORM)
                .when(blueprintTestClient.listV4());
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak with parent-child environments ",
            when = "a valid DistroX create request is sent to the child environment ",
            then = "DistroX cluster is created and instances are accessible via ssh by valid username and password ",
            and = "instances are not accessible via ssh by invalid username and password"
    )
    public void testCreateDistroXOnChildEnvironment(TestContext testContext) {
        testContext
                .given(DistroXInstanceTemplateTestDto.class, CHILD_CLOUD_PLATFORM)
                .given(DistroXTestDto.class, CHILD_CLOUD_PLATFORM)
                    .withEnvironmentKey(CHILD_ENVIRONMENT_KEY)
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .then((context, distrox, client) -> {
                    for (InstanceGroupV4Response ig : distrox.getResponse().getInstanceGroups()) {
                        for (InstanceMetaDataV4Response i : ig.getMetadata()) {
                            String ip = i.getPublicIp();
                            testShhAuthenticationSuccessfull(ip);
                            testShhAuthenticationFailure(ip);
                        }
                    }
                    return distrox;
                })
                .validate();
    }

    private void testShhAuthenticationSuccessfull(String host) throws IOException, UserAuthException {
        SSHClient client = getSshClient(host);
        client.authPassword(MOCK_UMS_USER, MOCK_UMS_PASSWORD);
    }

    private void testShhAuthenticationFailure(String host) throws IOException {
        try {
            SSHClient client = getSshClient(host);
            client.authPassword(MOCK_UMS_USER, MOCK_UMS_PASSWORD_INVALID);
            fail("SSH authentication passed with invalid password.");
        } catch (UserAuthException ex) {
            //Expected
        }
    }

    private SSHClient getSshClient(String host) throws IOException {
        SSHClient client = new SSHClient();
        client.addHostKeyVerifier(new PromiscuousVerifier());
        client.connect(host, SSH_PORT);
        client.setConnectTimeout(SSH_CONNECT_TIMEOUT);

        return client;
    }

    @Override
    protected String getImageId(TestContext testContext) {
        return testContext.get(DistroXTestDto.class).getResponse().getImage().getId();
    }
}
