package com.sequenceiq.cloudbreak.converter.v4.stacks;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.WORKLOAD;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.common.api.type.Tunnel.CCMV2_JUMPGATE;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
class StackToAutoscaleStackV4ResponseConverterTest {

    private static final String TEST_SALT_CB_VERSION = "2.70.0-b42";

    private static final String TEST_ENV_CRN = "testEnvCrn";

    private static final String TEST_BLUE_PRINT_TEXT = "bluePrintText";

    private static final String TEST_DH_CRN = "testDatahubCrn";

    private static final Integer TEST_GATEWAY_PORT = 1234;

    private static final String TEST_USER_ID = "testUserId";

    private static final String TEST_USER_CRN = "testUserCrn";

    private static final String TEST_TENANT = "testTenant";

    private static final String TEST_STACK_NAME = "testName";

    private static final String TEST_CLOUD_PLATFORM = "AWS";

    @Mock
    private ClusterComponentConfigProvider componentConfigProvider;

    @InjectMocks
    private StackToAutoscaleStackV4ResponseConverter underTest;

    @Test
    void testConvert() {
        long created = Instant.now().minus(30, MINUTES).toEpochMilli();
        Stack stack = getStack(true, created);

        doReturn(TEST_SALT_CB_VERSION).when(componentConfigProvider).getSaltStateComponentCbVersion(anyLong());

        AutoscaleStackV4Response result = underTest.convert(stack);

        makeBasicAssertions(result, created);
        assertThat(result.getSaltCbVersion()).isEqualTo(TEST_SALT_CB_VERSION);
        assertThat(result.getBluePrintText()).isEqualTo(TEST_BLUE_PRINT_TEXT);
        assertThat(result.getClusterStatus()).isEqualTo(AVAILABLE);
        assertThat(result.getClusterManagerIp()).isEqualTo("testIP");
        assertThat(result.getUserNamePath()).isEqualTo("user");
        assertThat(result.getPasswordPath()).isEqualTo("passwd");
    }

    @Test
    void testConvertWithNoCluster() {
        long created = Instant.now().minus(30, MINUTES).toEpochMilli();
        Stack stack = getStack(false, created);

        AutoscaleStackV4Response result = underTest.convert(stack);

        makeBasicAssertions(result, created);
    }

    private Stack getStack(boolean withCluster, long created) {
        Stack stack = new Stack();
        Workspace workspace = new Workspace();
        Tenant tenant = new Tenant();
        User user = new User();
        StackStatus stackStatus = new StackStatus();

        user.setUserId(TEST_USER_ID);
        user.setUserCrn(TEST_USER_CRN);
        tenant.setName(TEST_TENANT);
        workspace.setId(1L);
        workspace.setTenant(tenant);
        stack.setId(1L);
        stack.setWorkspace(workspace);
        stack.setCreator(user);
        stack.setName(TEST_STACK_NAME);
        stack.setGatewayPort(TEST_GATEWAY_PORT);
        stack.setTunnel(CCMV2_JUMPGATE);
        stack.setCreated(created);
        stackStatus.setStatus(AVAILABLE);
        stack.setStackStatus(stackStatus);
        stack.setResourceCrn(TEST_DH_CRN);
        stack.setCloudPlatform(TEST_CLOUD_PLATFORM);
        stack.setType(WORKLOAD);
        stack.setEnvironmentCrn(TEST_ENV_CRN);

        if (withCluster) {
            stack.setCluster(getMockedCluster());
        }

        return stack;
    }

    private Cluster getMockedCluster() {
        Cluster cluster = mock(Cluster.class);
        doReturn("testIP").when(cluster).getClusterManagerIp();
        doReturn("user").when(cluster).getCloudbreakAmbariUserSecretPath();
        doReturn("passwd").when(cluster).getCloudbreakAmbariPasswordSecretPath();

        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(TEST_BLUE_PRINT_TEXT);
        doReturn(blueprint).when(cluster).getBlueprint();
        return cluster;
    }

    private void makeBasicAssertions(AutoscaleStackV4Response result, long created) {
        assertThat(result.getCloudPlatform()).isEqualTo(TEST_CLOUD_PLATFORM);
        assertThat(result.getCreated()).isEqualTo(created);
        assertThat(result.getEnvironmentCrn()).isEqualTo(TEST_ENV_CRN);
        assertThat(result.getGatewayPort()).isEqualTo(TEST_GATEWAY_PORT);
        assertThat(result.getTunnel()).isEqualTo(CCMV2_JUMPGATE);
        assertThat(result.getStackType()).isEqualTo(WORKLOAD);
        assertThat(result.getStackCrn()).isEqualTo(TEST_DH_CRN);
        assertThat(result.getStatus()).isEqualTo(AVAILABLE);

    }
}