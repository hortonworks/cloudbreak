package com.sequenceiq.it.cloudbreak.testcase.e2e.environment;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.util.CollectionUtils;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.ResourceGroupTest;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;

public class AzureMultiAzFreeIpaTest extends AbstractE2ETest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        assertSupportedCloudPlatform(CloudPlatform.AZURE);
        createDefaultUser(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak",
            when = "create an Environment with MultiAz FreeIPA",
            then = "FreeIpa should be deployed across multiple Availability Zones")
    public void testCreateNewEnvironmentWithMultiAzFreeIpa(TestContext testContext) {

        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(EnvironmentNetworkTestDto.class)
                .withServiceEndpoints(ServiceEndpointCreation.DISABLED)
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withTelemetry("telemetry")
                .withCreateFreeIpa(Boolean.TRUE)
                .withFreeIpaNodes(3)
                .withResourceGroup(ResourceGroupTest.AZURE_RESOURCE_GROUP_USAGE_MULTIPLE, "")
                .withMultiAzFreeIpa()
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .init(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .then(this::validateMultiAz)
                .validate();
    }

    private FreeIpaTestDto validateMultiAz(TestContext testContext, FreeIpaTestDto freeIpaTestDto, FreeIpaClient freeIpaClient) {
        DescribeFreeIpaResponse  freeIpaResponse = freeIpaTestDto.getResponse();
        if (!freeIpaResponse.isEnableMultiAz()) {
            throw new TestFailException(String.format("MultiAz is not enabled for %s", freeIpaResponse.getName()));
        }
        List<String> instancesWithNoAz = freeIpaResponse.getInstanceGroups().stream()
                .map(ig -> ig.getMetaData())
                .filter(Objects::nonNull)
                .flatMap(ins -> ins.stream())
                .filter(igm1 -> igm1.getAvailabilityZone() == null)
                .map(igm2 -> igm2.getInstanceId())
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(instancesWithNoAz)) {
            throw new TestFailException(String.format("Availability Zones is missing for instances %s in %s",
                    instancesWithNoAz.stream().collect(Collectors.joining(",")), freeIpaResponse.getName()));
        }
        return freeIpaTestDto;
    }
}

