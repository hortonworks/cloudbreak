package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import java.util.List;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupsBuilder;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

public class DistroXEncryptedVolumeTest extends AbstractE2ETest {

    @Inject
    private DistroXTestClient distroXTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithNetworkAndFreeIPA(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid DistroX create request is sent with encrypted discs",
            then = "DistroX cluster is created")
    public void testCreateDistroXWithEncryptedVolume(TestContext testContext) {
        List<DistroXInstanceGroupTestDto> distroXInstanceGroupTestDtos = new DistroXInstanceGroupsBuilder(testContext)
                .defaultHostGroup()
                .withDiskEncryption()
                .build();
        testContext.given(DistroXTestDto.class)
                .withInstanceGroupsEntity(distroXInstanceGroupTestDtos)
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .then((context, distrox, client) -> {
                    distrox.getResponse();
                    return distrox;
                })
                .validate();
    }
}
