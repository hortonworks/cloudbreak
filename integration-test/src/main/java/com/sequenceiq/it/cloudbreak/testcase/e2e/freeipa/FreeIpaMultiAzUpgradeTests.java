package com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.waitForFlow;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
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

public class FreeIpaMultiAzUpgradeTests extends AbstractE2ETest {

    protected static final Status FREEIPA_AVAILABLE = Status.AVAILABLE;

    protected static final Status FREEIPA_DELETE_COMPLETED = Status.DELETE_COMPLETED;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid MultiAz stack create request is sent with 3 FreeIPA instances " +
                    "AND the MultiAz stack is upgraded one node at a time",
            then = "the MultiAz stack should be available AND deletable")
    public void testHAFreeIpaMultiAzInstanceUpgrade(TestContext testContext) {
        String freeIpa = resourcePropertyProvider().getName();

        testContext
                .given(freeIpa, FreeIpaTestDto.class)
                .withFreeIpaHa(1, 3)
                .withUpgradeCatalogAndImage()
                .withEnableMultiAz(true)
                .when(freeIpaTestClient.create(), key(freeIpa))
                .await(FREEIPA_AVAILABLE)
                .given(freeIpa, FreeIpaTestDto.class)
                .when(freeIpaTestClient.upgrade())
                .await(FREEIPA_AVAILABLE, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .then((tc, testDto, client) -> {
                    validateMultiAz(testDto.getRequest().getEnvironmentCrn(), client, freeIpa, tc);
                    return testDto;
                })
                .then((tc, testDto, client) -> freeIpaTestClient.delete().action(tc, testDto, client))
                .await(FREEIPA_DELETE_COMPLETED, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .validate();
    }

    private void validateMultiAz(String environmentCrn, FreeIpaClient client, String freeIpa, TestContext tc) {
        DescribeFreeIpaResponse freeIpaResponse = client.getDefaultClient().getFreeIpaV1Endpoint().describe(environmentCrn);
        if (!freeIpaResponse.isEnableMultiAz()) {
            throw new TestFailException(String.format("MultiAz is not enabled for %s after upgrade", freeIpaResponse.getName()));
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
            throw new TestFailException(String.format("Availability Zones is missing for instances %s in %s after upgrade",
                    instancesWithNoAz.stream().collect(Collectors.joining(",")), freeIpaResponse.getName()));
        }
    }

    private CloudFunctionality getCloudFunctionality(TestContext testContext) {
        return testContext.getCloudProvider().getCloudFunctionality();
    }
}