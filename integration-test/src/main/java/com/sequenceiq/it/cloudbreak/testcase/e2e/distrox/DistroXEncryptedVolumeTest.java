package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.it.cloudbreak.assertion.distrox.DistroXExternalDatabaseAssertion.validateTemplateContainsExternalDatabaseHostname;

import java.util.List;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.assertion.distrox.AvailabilityZoneAssertion;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.externaldatabase.DistroXExternalDatabaseTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupsBuilder;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshEnaDriverCheckActions;

public class DistroXEncryptedVolumeTest extends AbstractE2ETest {

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private TestParameter testParameter;

    @Inject
    private SshEnaDriverCheckActions sshEnaDriverCheckActions;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithFreeIpaAndDatalake(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid DistroX create request is sent with encrypted discs",
            then = "DistroX cluster is created")
    public void testCreateDistroXWithEncryptedVolume(TestContext testContext) {
        String externalDatabaseName = resourcePropertyProvider().getName();
        List<DistroXInstanceGroupTestDto> distroXInstanceGroupTestDtos = new DistroXInstanceGroupsBuilder(testContext)
                .defaultHostGroup()
                .withDiskEncryption()
                .build();
        testContext.given(externalDatabaseName, DistroXExternalDatabaseTestDto.class)
                .withAvailabilityType(DistroXDatabaseAvailabilityType.NON_HA)
                .given(DistroXTestDto.class)
                .withInstanceGroupsEntity(distroXInstanceGroupTestDtos)
                .withExternalDatabase(externalDatabaseName)
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .then((tc, testDto, client) -> {
                    sshEnaDriverCheckActions.checkEnaDriverOnAws(testDto.getResponse(), client);
                    return testDto;
                })
                .then(new AvailabilityZoneAssertion())
                .then(validateTemplateContainsExternalDatabaseHostname())
                .then((context, distrox, client) -> {
                    distrox.getResponse();
                    return distrox;
                })
                .validate();
    }

}
