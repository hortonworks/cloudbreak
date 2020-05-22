package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.externaldatabase.DistroXExternalDatabaseTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupsBuilder;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;

import static com.sequenceiq.it.cloudbreak.assertion.distrox.DistroXExternalDatabaseAssertion.validateTemplateContainsExternalDatabaseHostname;

public class DistroXEncryptedVolumeTest extends AbstractE2ETest {

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private TestParameter testParameter;

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
                .then(validateTemplateContainsExternalDatabaseHostname())
                .then((context, distrox, client) -> {
                    distrox.getResponse();
                    return distrox;
                })
                .validate();
    }

}
