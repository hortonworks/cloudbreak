package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.NetworkV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.SecurityGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.wait.WaitUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxPrivateNetworkTests extends AbstractE2ETest {

    private Map<String, InstanceStatus> instancesHealthy = new HashMap<>() {{
        put(HostGroupType.MASTER.getName(), InstanceStatus.SERVICES_HEALTHY);
        put(HostGroupType.IDBROKER.getName(), InstanceStatus.SERVICES_HEALTHY);
    }};

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private WaitUtil waitUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak",
            when = "an internal SDX create request is sent (with Private Network and FreeIPA only)",
            then = "SDX should get available"
    )
    public void testSDXInPrivateNetwork(TestContext testContext) {
        String credential = resourcePropertyProvider().getName();
        String environmentNetwork = resourcePropertyProvider().getName();
        String environment = resourcePropertyProvider().getName();
        String clouderaManager = resourcePropertyProvider().getName();
        String cluster = resourcePropertyProvider().getName();
        String securityGroup = resourcePropertyProvider().getName();
        String network = resourcePropertyProvider().getName();

        String sdxInternal = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        String masterInstanceGroup = "master";
        String idbrokerInstanceGroup = "idbroker";

        testContext
                .given(credential, CredentialTestDto.class)
                .when(credentialTestClient.create(), key(credential))
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(environmentNetwork, EnvironmentNetworkTestDto.class)
                .withAws(environmentNetworkParameters())
                .withNetworkCIDR("172.18.0.0/16")
                .withSubnetIDs(Set.of("subnet-0f4adedb9b605dc88", "subnet-094e7d13273d4be78"))
                .given(environment, EnvironmentTestDto.class)
                .withNetwork(environmentNetwork)
                .withTelemetry("telemetry")
                .withCreateFreeIpa(Boolean.TRUE)
                .when(environmentTestClient.create(), key(environment))
                .await(EnvironmentStatus.AVAILABLE, key(environment))
                .given(clouderaManager, ClouderaManagerTestDto.class)
                .given(cluster, ClusterTestDto.class).withBlueprintName("7.1.0 - SDX Light Duty: Apache Hive Metastore, Apache Ranger, Apache Atlas")
                .withValidateBlueprint(Boolean.FALSE)
                .withClouderaManager(clouderaManager)
                .given(securityGroup, SecurityGroupTestDto.class).withSecurityGroupIds("sg-0b6596ce403337592")
                .given(network, NetworkV4TestDto.class).withSubnetCIDR("172.18.0.0/16").withAws(stackNetworkParameters())
                .given(masterInstanceGroup, InstanceGroupTestDto.class).withHostGroup(MASTER).withNodeCount(1).withSecurityGroup(securityGroup)
                .given(idbrokerInstanceGroup, InstanceGroupTestDto.class).withHostGroup(IDBROKER).withNodeCount(1).withSecurityGroup(securityGroup)
                .given(stack, StackTestDto.class).withCluster(cluster).withInstanceGroups(masterInstanceGroup, idbrokerInstanceGroup).withNetwork(network)
                .given(sdxInternal, SdxInternalTestDto.class)
                .withStackRequest(key(cluster), key(stack))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .awaitForFlow(key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxInternal))
                .then((tc, testDto, client) -> {
                    return waitUtil.waitForSdxInstancesStatus(testDto, client, instancesHealthy);
                })
                .validate();
    }

    private EnvironmentNetworkAwsParams environmentNetworkParameters() {
        EnvironmentNetworkAwsParams environmentNetworkAwsParams = new EnvironmentNetworkAwsParams();
        environmentNetworkAwsParams.setVpcId("vpc-0bc2537e869168284");
        return environmentNetworkAwsParams;
    }

    public AwsNetworkV4Parameters stackNetworkParameters() {
        AwsNetworkV4Parameters awsNetworkV4Parameters = new AwsNetworkV4Parameters();
        awsNetworkV4Parameters.setVpcId("vpc-0bc2537e869168284");
        awsNetworkV4Parameters.setSubnetId("subnet-0f4adedb9b605dc88");
        return awsNetworkV4Parameters;
    }
}
