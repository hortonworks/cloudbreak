package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.Collection;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.client.UmsTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxRefreshEntitlementTest extends PreconditionSdxE2ETest {

    private static final String SERVICE_ACTIVE = "active";

    private static final String SERVICE_INACTIVE = "inactive";

    private static final String CLEAN_UP = "CLEAN_UP";

    private static final String PROMETHEUS_SERVICE_STATUS_COMMAND = "sudo systemctl is-active cdp-prometheus";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private UmsTestClient umsTestClient;

    @Inject
    private SshJClientActions sshJClientActions;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "entitlement refresh called to enable/disable prometheus",
            then = "prometheus service status should change"
    )
    public void testSDXRefreshEntitlement(TestContext testContext) {
        String clusterName = resourcePropertyProvider().getName();
        String accountId = testContext.getActingUserCrn().getAccountId();
        String entitlementName = Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name();
        testContext
                .given(clusterName, SdxTestDto.class)
                .withName(clusterName)
                .withCloudStorage()
                .when(sdxTestClient.create(), key(clusterName))
                .await(SdxClusterStatusResponse.RUNNING, key(clusterName))
                .awaitForHealthyInstances()
                .given(UmsTestDto.class)
                .when(umsTestClient.grantEntitlement(accountId, entitlementName))
                .given(StackTestDto.class)
                .withName(clusterName)
                .withEnvironmentClass(EnvironmentTestDto.class)
                .when(stackTestClient.getV4())
                .when(stackTestClient.refreshEntitlementParamsV4())
                .awaitForFlow()
                .then((tc, testDto, client) -> {
                    checkPrometheusServiceStatus(testDto.getResponse().getInstanceGroups(), SERVICE_ACTIVE);
                    return testDto;
                })
                .given(UmsTestDto.class)
                .when(umsTestClient.revokeEntitlement(accountId, entitlementName))
                .given(StackTestDto.class)
                .withName(clusterName)
                .withEnvironmentClass(EnvironmentTestDto.class)
                .when(stackTestClient.refreshEntitlementParamsV4())
                .awaitForFlow()
                .then((tc, testDto, client) -> {
                    checkPrometheusServiceStatus(testDto.getResponse().getInstanceGroups(), SERVICE_INACTIVE);
                    return testDto;
                })
                .given(UmsTestDto.class)
                .when(umsTestClient.grantEntitlement(accountId, CLEAN_UP))
                .validate();
    }

    private void checkPrometheusServiceStatus(Collection<InstanceGroupV4Response> instanceGroupV4Responses, String serviceStatus) {
        Map<String, Pair<Integer, String>> sshCommandResponse = sshJClientActions.executeSshCommandOnPrimaryGateways(instanceGroupV4Responses,
                PROMETHEUS_SERVICE_STATUS_COMMAND, false);
        boolean prometheusServiceStatusMatch = sshCommandResponse.values().stream()
                .map(Map.Entry::getValue)
                .allMatch(status -> status.startsWith(serviceStatus));
        if (!prometheusServiceStatusMatch) {
            throw new TestFailException(String.format("Prometheus service is not '%s' on all gateway nodes. " +
                    "Checks: %s", serviceStatus, sshCommandResponse));
        }
    }
}
