package com.sequenceiq.it.cloudbreak.testcase.e2e.environment;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.util.CollectionUtils;
import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;

public class AzureMultiAzFreeIpaTest extends AbstractE2ETest {

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        initializeTest(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak",
            when = "create an Environment with MultiAz FreeIPA",
            then = "FreeIpa should be deployed across multiple Availability Zones")
    public void testCreateNewEnvironmentWithMultiAzFreeIpa(TestContext testContext) {
        setUpEnvironmentTestDto(testContext, Boolean.TRUE, 3)
                .withEnableMultiAzFreeIpa()
                .when(getEnvironmentTestClient().create())
                .awaitForCreationFlow()
                .given(FreeIpaTestDto.class)
                .then((tc, testDto, client) -> {
                    validateMultiAz(testDto.getRequest().getEnvironmentCrn(), client, testDto.getName(), tc);
                    return testDto;
                })
                .validate();
    }

    private void validateMultiAz(String environmentCrn, FreeIpaClient client, String freeIpa, TestContext tc) {
        DescribeFreeIpaResponse freeIpaResponse = client.getDefaultClient().getFreeIpaV1Endpoint().describe(environmentCrn);
        if (!freeIpaResponse.isEnableMultiAz()) {
            throw new TestFailException(String.format("MultiAz is not enabled for %s", freeIpaResponse.getName()));
        }
        List<String> instanceIds = freeIpaResponse.getInstanceGroups().stream()
                .map(ig -> ig.getMetaData())
                .filter(Objects::nonNull)
                .flatMap(ins -> ins.stream())
                .map(InstanceMetaDataResponse::getInstanceId)
                .collect(Collectors.toList());
        Map<String, Set<String>> availabilityZoneForVms = getCloudFunctionality(tc).listAvailabilityZonesForVms(freeIpa, instanceIds);
        List<String> instancesWithNoAz = instanceIds.stream().filter(instance -> CollectionUtils.isEmpty(availabilityZoneForVms.get(instance)))
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(instancesWithNoAz)) {
            throw new TestFailException(String.format("Availability Zones is missing for instances %s in %s",
                    instancesWithNoAz.stream().collect(Collectors.joining(",")), freeIpaResponse.getName()));
        }
    }

    private CloudFunctionality getCloudFunctionality(TestContext testContext) {
        return testContext.getCloudProvider().getCloudFunctionality();
    }
}

