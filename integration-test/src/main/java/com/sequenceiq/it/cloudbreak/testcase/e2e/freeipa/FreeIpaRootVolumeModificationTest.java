package com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.util.CollectionUtils;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.sdx.PreconditionSdxE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.FreeIpaInstanceUtil;

public class FreeIpaRootVolumeModificationTest extends PreconditionSdxE2ETest {

    private static final String TEST_INSTANCE_GROUP = "master";

    private static final int ROOT_UPDATE_SIZE = 200;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private FreeIpaInstanceUtil freeIpaInstanceUtil;

    @Test(dataProvider = TEST_CONTEXT, timeOut = 9000000)
    @Description(
            given = "there is an available environment with a running freeipa",
            when = "root volume resize is called on the freeipa",
            then = "root volume of instances must be modified to the new type and size"
    )
    public void testFreeIpaRootVolumeModification(TestContext testContext) {
        CloudPlatform cloudPlatform = testContext.getCloudPlatform();
        testContext
            .given(FreeIpaTestDto.class)
            .when(freeIpaTestClient.updateDisks(ROOT_UPDATE_SIZE, getVolumeType(cloudPlatform, testContext)))
            .awaitForFlow()
            .await(Status.AVAILABLE)
            .awaitForHealthyInstances()
            .given(FreeIpaTestDto.class)
            .when(freeIpaTestClient.describe())
            .then((tc, testDto, client) -> {
                validateRootDisks(testDto, tc, client, cloudPlatform);
                return testDto;
            })
            .validate();
    }

    protected CloudFunctionality getCloudFunctionality(TestContext testContext) {
        return testContext.getCloudProvider().getCloudFunctionality();
    }

    private String getVolumeType(CloudPlatform cloudPlatform, TestContext testContext) {
        if (cloudPlatform == CloudPlatform.AWS || cloudPlatform == CloudPlatform.AZURE) {
            return testContext.getCloudProvider().verticalScaleVolumeType();
        }
        return null;
    }

    private List<String> getVolumesOnCloudProvider(FreeIpaTestDto freeIpaTestDto, TestContext tc, FreeIpaClient client) {
        List<String> updatedInstances = freeIpaInstanceUtil.getInstanceIds(freeIpaTestDto, client, TEST_INSTANCE_GROUP);
        CloudFunctionality cloudFunctionality = getCloudFunctionality(tc);
        return cloudFunctionality.listInstancesRootVolumeIds(freeIpaTestDto.getName(), updatedInstances);
    }

    private void validateRootDisks(FreeIpaTestDto freeIpaTestDto, TestContext tc, FreeIpaClient client, CloudPlatform cloudPlatform) {
        int expectedDiskSize = ROOT_UPDATE_SIZE;
        String expectedVolumeType = getVolumeType(cloudPlatform, tc);

        List<String> rootVolumes = getVolumesOnCloudProvider(freeIpaTestDto, tc, client);
        if (CollectionUtils.isEmpty(rootVolumes)) {
            throw new TestFailException(String.format("Root volume is not present on instances on Cloud Provider for group %s",
                    TEST_INSTANCE_GROUP));
        }

        List<Volume> rootVolumesAttributes = getCloudFunctionality(tc).describeVolumes(rootVolumes);
        rootVolumesAttributes.forEach(vol -> {
            if (vol.getSize() != expectedDiskSize || (expectedVolumeType != null && !expectedVolumeType.equalsIgnoreCase(vol.getType()))) {
                throw new TestFailException(String.format("Update Disk did not complete successfully for instances on cloud provider in group %s",
                        TEST_INSTANCE_GROUP));
            }
        });
    }
}
